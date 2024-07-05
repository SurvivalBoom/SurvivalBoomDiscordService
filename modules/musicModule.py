#    _____                  _             ______
#   / ___/__  ________   __(_)   ______ _/ / __ )____  ____  ____ ___
#   \__ \/ / / / ___/ | / / / | / / __ `/ / __  / __ \/ __ \/ __ `__ \
#  ___/ / /_/ / /   | |/ / /| |/ / /_/ / / /_/ / /_/ / /_/ / / / / / /
# /____/\__,_/_/    |___/_/ |___/\__,_/_/_____/\____/\____/_/ /_/ /_/
# SurvivalBoom Network 2023 | SurvivalBoom Discord Service
#              Music Module | By TIMURishche
#
#
import asyncio
import re
import traceback
import disnake
import requests
import validators
import spotipy
import mafic

from disnake.ext import commands, tasks
from main import SurvivalBoomDiscordService as SBDS


this_module_name = f"{__name__}".removeprefix("modules.")

# Класс музичного бота.

class Api:

    class MusicBotNotFound(Exception):
        def __init__(self, name): super().__init__(f"Бота із назвою {name} не знайдено")

    class MusicBot(commands.Cog):

        # Встановлюємо змінну бота із аргументу класса.
        def __init__(self, bot: commands.InteractionBot, name: str, node: mafic.Node):

            self._bot: commands.InteractionBot = bot
            self._name: str = name
            self.task: asyncio.Task = ...
            self.crashed: bool = False
            self.crash_error: str = ...

            self._playlist: list[mafic.Track] = []  # Плейліст цього музичного бота.
            self._current_song_index: int = -1 # Індекс поточної пісні у плейлісті.

            self.busy: bool = False  # Змінна зайнятості цього бота.
            self.loop: bool = False # Змінна режиму повторення цього бота.
            self.mode_24_7 = False # Змінна роботи режиму 24 на 7.
            self.locked = False # Змінна блокування цього бота.

            self._connected_channel: disnake.VoiceChannel = ... # Канал до якого підключений цей бот.
            self._player: mafic.Player = ...
            self._last_text_channel: disnake.TextChannel = ... # Останній канал де була виконана команда бота. У цей канал бот буде відправляти повідомлення про пісню яка зараз грає.

            self._node: mafic.Node = node

            async def started():

                await self._bot.change_presence(status=disnake.Status.offline)


            self._bot.add_listener(started, "on_ready")

        # Додавання пісні у плейліст цього бота.
        async def playlist_add(self, track: mafic.Track):
            if self.busy is True:
                self._playlist.append(track)

        # Зупинка (відключення) музичного бота.
        def disconnect(self):
            asyncio.create_task(self._player.disconnect(force=True))

        # Запуск музичного бота (підключення)
        async def connect(self, channel: disnake.VoiceChannel, track: mafic.Track):

            if self.busy is False:

                # noinspection PyTypeChecker
                channel: disnake.VoiceChannel = self._bot.get_channel(channel.id)

                # noinspection PyTypeChecker
                self._player: mafic.Player = await channel.connect(cls=mafic.Player(client=self._bot, nodes=[self._node]))

                self._playlist.append(track)

                # print(f"{self._player.client.user.name} - {self._player.is_playing()} - {self._player.current_node} - {self._player.nodes}")

                self.busy = True
                self._song_task.start()
                self._song_task.get_task().set_name(f"{__name__} -- {self._bot.user} song_task()")

        # Пропуск пісні.
        def skip(self):
            if not self._current_song_index >= len(self._playlist) - 1:
                if self.loop == "TRACK": self._current_song_index = self._current_song_index + 1

                asyncio.create_task(self._player.stop())

                if self.loop != "TRACK": return {"current_song": self._playlist[self._current_song_index], "next_song": self._playlist[self._current_song_index + 1]}
                else: return {"current_song": self._playlist[self._current_song_index], "next_song": self._playlist[self._current_song_index]}

            elif self.loop == "PLAYLIST": return {"current_song": self._playlist[self._current_song_index], "next_song": self._playlist[0]}
            else:
                self.disconnect()
                return "STOP"

        # Повернення минулої пісні.
        def back(self) -> str | mafic.Track:
            if self._current_song_index > 0:

                i = self._current_song_index

                if not self.loop == "TRACK": self._current_song_index = i - 2
                else: self._current_song_index = i - 1
                asyncio.create_task(self._player.stop())

                return self._playlist[i - 3]

            else:
                self.disconnect()
                return "STOP"

        @tasks.loop(seconds=1)  # Головна такса яка відповідає за роботу музичного бота.
        async def _song_task(self):

            try:

                # Вимикання музичного бота якщо він був відключений з голосового каналу.
                if self.busy is True and len(self._bot.voice_clients) < 1:
                    await self._bot.change_presence(status=disnake.Status.invisible)

                    # Очищення плейлісту музичного бота.
                    self._playlist.clear()
                    self._current_song_index = -1

                    # Вимикання усіх особливих режимів музичного бота.
                    self.mode_24_7 = False
                    self.loop = False
                    self.locked = False

                    # Очищення змінних із каналами бота.
                    # noinspection PyTypeChecker
                    self._connected_channel = ...
                    # noinspection PyTypeChecker
                    self._last_text_channel = ...

                    self.busy = False  # Перемикання статусу зайнятості бота на незайнятий.

                    self._song_task.stop()  # Вимикання таску цього музичного бота.

                    return

                # Відключення музичного бота із заблокованих каналів.
                if self._connected_channel is not ... and self._connected_channel.id in SBDS.settings.get("modules.music-module.channels-black-list"): self.disconnect()

                # noinspection PyTypeChecker
                self._connected_channel: disnake.VoiceChannel = self._bot.get_channel(self._bot.voice_clients[0].channel.id)  # Встановлюємо змінну поточного голосового каналу.
                is_playing = self._player.playing  # Перевіряємо чи грає бот і встановлюємо змінну.

                print(is_playing)

                # Перевірка чи грає зараз бот і дії якщо бот не грає.
                if self.busy is True and not is_playing:

                    # Спроба програти наступну пісню.
                    try:

                        if not self.loop == "TRACK": self._current_song_index = self._current_song_index + 1

                        await self._player.play(self._playlist[self._current_song_index])

                    # Дії якщо бот програв усі пісні з плейліста.
                    except IndexError:

                        # Якщо увімкнено режим повторення або увімкнено режим 24 на 7 почати програвати плейліст знову.
                        if self.loop == "PLAYLIST" or self.mode_24_7 is True: self._current_song_index = -1

                        # Якщо минула умова не спрацювала, відключити бота.
                        else:

                            # _cog.logger.error("ERROR!!")
                            #
                            # _cog.logger.error(str(self._playlist))

                            await self._player.disconnect(force=True)

                if self.busy is True: await self._bot.change_presence(status=disnake.Status.online, activity=disnake.Activity(type=disnake.ActivityType.playing, name=f"#{self._connected_channel.name}"))

                # Перевірка чи є у голосовому каналі користувачі. Якщо немає і режим 24 на 7 вимкнено, відключити бота.
                channel_members = [member for member in self._connected_channel.members if not member.bot]  # Отримуємо список користувачів у голосовому каналі бота (боти не враховуються)
                if self.busy is True and self.mode_24_7 is False and not channel_members: self.disconnect()

            except Exception as error:

                self.disconnect()

                _cog.logger.error(f"Виникла помилка &c'{error}' &4у &csong_task()&4.")
                _cog.logger.error(f"Музичного бота &e{self._bot.user.name}&4 від'єднано!")
                if SBDS.tracebackAllowed: _cog.logger.error(traceback.format_exc())

        @property
        def connected_channel(self): return self._connected_channel

        @property
        def name(self): return self._bot.user.name

        @property
        def bot(self): return self._bot

        @property
        def node(self): return self._node

    def __init__(self):
        self._music_bots: dict[str, Api.MusicBot] = {}

    def startBots(self) -> None:

        for music_bot_info in SBDS.settings.get("modules.music-module.music-bots"):

            token = music_bot_info['token']
            name = music_bot_info['name']
            bot = commands.InteractionBot(intents=disnake.Intents.all())
            bot_cog = self.MusicBot(bot, name, _cog.wavelink_node)

            async def start_bot(bott: commands.InteractionBot, tokenn: str, bot_cogg: Api.MusicBot):

                try: await bott.start(tokenn)
                except Exception as error:
                    bot_cogg.crashed = True
                    bot_cogg.crash_error = error

            bot.add_cog(bot_cog)
            task = asyncio.create_task(start_bot(bot, token, bot_cog))
            task.set_name(f"{this_module_name} - {name}")
            bot_cog.task = task

            self._music_bots.update({name: bot_cog})

    def stopBots(self) -> None:

        for mbot in self._music_bots.copy():

            mmbot = self._music_bots[mbot]

            try: mmbot.disconnect()
            except: pass

            asyncio.create_task(mmbot.bot.change_presence(status=disnake.Status.offline), name=f"{this_module_name} - ChangeMusicBotStatusToInvisible")

            mmbot.task.cancel()

            self._music_bots.pop(mbot)

    @property
    def running_bots(self) -> list[MusicBot]: return [self._music_bots[bot] for bot in self._music_bots if self._music_bots[bot].crashed is False]

    @property
    def crashed_bots(self) -> list[MusicBot]: return [self._music_bots[bot] for bot in self._music_bots if self._music_bots[bot].crashed is True]

    @property
    def all_bots(self) -> list[MusicBot]: return [self._music_bots[bot] for bot in self._music_bots]

    def getBot(self, name: str) -> MusicBot:
        try: return self._music_bots[name]
        except KeyError: raise self.MusicBotNotFound(name)


# Клас модуля музичного бота.
class _MusicModuleCog(commands.Cog):

    def __init__(self):

        self.wavelink_pool: mafic.Node = ...
        self.wavelink_node: mafic.Node = ...
        self.logger: SBDS.mainlogger.createModuleLogger() = ...
        self.module_settings: SBDS.settings.SettingsSection = ...
        self.spotify: spotipy.Spotify = ...

        self.lock_command_allowed_roles_ids: list[int] = ...
        self.lock_command_enabled: bool = ...

        self.mode_24_7_command_allowed_roles_ids: bool = ...
        self.mode_24_7_command_enabled: bool = ...

        self.ban_command_enabled: bool = ...
        self.ban_command_allowed_roles_ids: list[int] = ...
        self.ban_command_self_block: bool = ...
        self.ban_command_stuff_block: bool = ...

        self.bypass_black_list_roles: list[int] = ...
        self.black_list: list[str] = ...

        self.channels_black_list: list[int] = ...

    # Запускаємо музичних ботів і додаємо їх у список при завантаженні цього модуля.
    def cog_load(self):

        try:

            self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
            self.module_settings = SBDS.settings.createSection(path="modules.music-module")

            self.lock_command_allowed_roles_ids = self.module_settings.get("commands.lock-command.allowed-roles")
            self.lock_command_enabled = self.module_settings.get("commands.lock-command.enabled")

            self.ban_command_enabled = self.module_settings.get("commands.ban-command.enabled")
            self.ban_command_allowed_roles_ids = self.module_settings.get("commands.ban-command.allowed-roles")
            self.ban_command_self_block = self.module_settings.get("commands.ban-command.block-self-use")
            self.ban_command_stuff_block = self.module_settings.get("commands.ban-command.block-stuff-use")

            self.mode_24_7_command_allowed_roles_ids = self.module_settings.get("commands.24_7-mode-command.allowed-roles")
            self.mode_24_7_command_enabled = self.module_settings.get("commands.24_7-mode-command.enabled")

            self.bypass_black_list_roles = self.module_settings.get("music-name-black-list-bypass-roles")

            self.black_list = self.module_settings.get("music-name-black-list")
            self.channels_black_list = self.module_settings.get("channels-black-list")

            lavalink_settings = SBDS.settings.createSection(path="modules.music-module.lavalink")
            host = lavalink_settings.get("host")
            port = lavalink_settings.get("port")
            password = lavalink_settings.get("password")
            self.wavelink_node = mafic.NodePool.create_node(host=host, port=port, label="Node", password=password)

            self.wavelink_pool = mafic.Node()

            async def connect_to_lavalink():
                await self.wavelink_pool.create_node(nodes=[self.wavelink_node], client=SBDS.main_bot)

                # await self.wavelink_node._connect(client=SBDS.main_bot)

            asyncio.create_task(connect_to_lavalink(), name=f"{this_module_name} - Connect To MediaServer")


            # Якщо підтримка Spotify увімкнена, запустити сессію Spotify API.
            if self.module_settings.get("spotify.enabled"): self.spotify = spotipy.Spotify(auth_manager=spotipy.SpotifyClientCredentials(client_id=self.module_settings.get("spotify.spotify-api-client-id"), client_secret=self.module_settings.get("spotify.spotify-api-client-secret")))

            # Запускаємо музичних ботів.
            MusicBotsApi.startBots()


        except Exception as error: SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    # Вимикаємо усіх музичних ботів при вивантаженні цього модуля.
    def cog_unload(self):

        try: MusicBotsApi.stopBots()
        except Exception as error:
            self.logger.error(f"Виникла помилка при вивантаженні модуля &c{__name__}&4: &b{error}&4.")
            if SBDS.tracebackAllowed: self.logger.error(traceback.format_exc())
            self.logger.error("")

    # Метод пошуку нового вільного бота.
    @staticmethod
    def findFreeBot():
        for music_bot in MusicBotsApi.running_bots:
            if not music_bot.busy: return music_bot
        return None

    # Метод пошуку бота який зараз знаходиться у каналі користувача.
    @staticmethod
    def findBotInVoice(voice_channel: disnake.VoiceChannel):
        for music_bot in MusicBotsApi.running_bots:
            if music_bot.connected_channel is not ... and music_bot.connected_channel.id == voice_channel.id: return music_bot
        return None

    # Метод обробки посилання. Повертає тип цього посилання.
    def check_type_of_url(self, url: str):

        if not validators.url(url) and self.module_settings.get("youtube-support-enabled"): return "SearchText"

        spotify_support: bool = self.module_settings.get("spotify.enabled")
        youtube_support: bool = self.module_settings.get("youtube-support-enabled")

        if re.compile(r"https://open\.spotify\.com/track/", re.IGNORECASE).match(url) and spotify_support and youtube_support: return "SpotifyTrack"
        if re.compile(r"https://open\.spotify\.com/playlist/", re.IGNORECASE).match(url) and spotify_support and youtube_support: return "SpotifyPlaylist"
        if re.compile(r"https://www\.youtube\.com/watch\?v=", re.IGNORECASE).match(url) and youtube_support: return "YoutubeVideo"
        if re.compile(r"https://youtube\.com/playlist\?list=", re.IGNORECASE).match(url) and youtube_support: return "YoutubePlaylist"


        try:

            if self.module_settings.get("direct-media-support-enabled"):

                response = requests.head(url=url)

                if 'Content-Type' in response.headers and 'audio' in response.headers['Content-Type'] or 'video' in response.headers['Content-Type']: return "DirectUrl"

        except: pass


        return None

    @staticmethod # Метод отримання інформації про відео.
    def seconds_to_time(seconds):

        seconds = seconds // 1000

        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        seconds = seconds % 60

        if hours > 0:
            return f"{hours:02}:{minutes:02}:{seconds:02}"
        else:
            return f"{minutes:02}:{seconds:02}"

    @staticmethod
    def get_user_blocked_status(user_id: str):

        try:
            ban_state = SBDS.database.getUserData(user_id=user_id, key="musicbot-banned")
            return ban_state

        except SBDS.database.KeyNotFound:

            SBDS.database.addUserKey(user_id=user_id, key="musicbot-banned")
            SBDS.database.setUserKey(user_id=user_id, key="musicbot-banned", value="False")

            ban_state = SBDS.database.getUserData(user_id=user_id, key="musicbot-banned")

            return ban_state

    # Метод виконання дій при помилках у боті.
    def if_error_occurred(self, error: Exception, ctx: disnake.ApplicationCommandInteraction, place: str, command: str):

        module_name = f"{__name__}".removeprefix("modules.")

        self.logger.error("")
        self.logger.error(f"--- Виникла помилка у при обробці команди &b{command} &4(&a{module_name}&4) ---")
        self.logger.error(f"Помилка: &2{error}.")
        self.logger.error(f"Де саме: &2{place}.")
        if SBDS.tracebackAllowed: self.logger.error(f"Stacktrace: &3{traceback.format_exc()}")
        self.logger.error("")

        SBDS.utils.sendErrorToDebugChannel(file_name=f"{__name__}".replace(".", "/"), place=place, error=error)
        SBDS.utils.sendErrorToUser(error=error, ctx=ctx)



#
# Команди.
#

    # Обробка команди /play
    @commands.slash_command(name="play", description="Запускає музичного бота у голосовому каналі або додає пісню у плейліст.", options=[disnake.Option(name="data", description="Пошуковий запит, YouTube посилання, Spotify посилання або пряме посилання на аудіо файл.", type=3, required=True)])
    async def play_command(self, ctx: disnake.ApplicationCommandInteraction, data: str):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            if self.get_user_blocked_status(user_id=str(ctx.user.id)) == "True":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.YOU-MUSIC-BANNED"))
                return

            # Перевіряємо чи знаходиться користувач у голосовому каналі. Якщо ні, посилаємо користувача нахуй.
            if ctx.user.voice is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))
                return

            # Перевіряємо чи знаходиться користувач у заблокованому каналі.
            if ctx.user.voice.channel.id in self.channels_black_list:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BLOCKED-CHANNEL", placehoders={"{{CHANNEL}}": f"<#{ctx.user.voice.channel.id}>"}))
                return

            # Перевіряємо запит користувача і якщо користувач не адмін і у запиті є погані слова відправити користувача на ЮХ!
            if not SBDS.utils.checkUserRoles(roles_id_list=self.bypass_black_list_roles, member=ctx.user) and data in self.black_list:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BLACK-LISTED-WORDS-FOUND"))
                return

            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel) # Шукаємо бота який знаходиться у голосовому каналі користувача.

            if bot is None: bot = self.findFreeBot() # Якщо бота у голосовому каналі не знайдено, шукаємо вільного бота.

            # Якщо після цих дій bot все ще None, відправляємо повідомлення про незнайденого бота.
            if bot is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.FREE-BOT-NOT-FOUND"))
                return

            # Перевіряємо чи заблокований поточний музичний бот.
            if bot.locked and not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BOT-LOCKED", placehoders={"{{BOT}}": f"<@{bot.bot.user.id}>"}))
                return


            data_type = self.check_type_of_url(url=data) # Визначаємо тип посилання.

            # Якщо це посилання на ютуб відео або просто пошуковий запит, шукаємо на ютубі.
            if data_type == "SearchText" or data_type == "YoutubeVideo":
                tracks = await mafic.Node().fetch_tracks(f"ytsearch:{data}")
                if not tracks:
                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-RESULT-FOUND", placehoders={"{{QUERY}}": data}))
                    return
                track = tracks[0]

            # Якщо це посилання на спотіфай, шукаємо ютуб відео із назвою трека.
            elif data_type == "SpotifyTrack":

                spotify_id = data.split("/track/")[1].split("?")[0]

                spotify_track_name = self.spotify.track(track_id=spotify_id)['name']
                spotify_track_author = self.spotify.track(track_id=spotify_id)['album']['artists'][0]['name']

                tracks = await self.wavelink_pool.fetch_tracks(f"ytsearch:{spotify_track_author} + {spotify_track_name}")
                if not tracks:
                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-RESULT-FOUND", placehoders={"{{QUERY}}": data}))
                    return

                track = tracks[0]

            # Якщо це пряме посилання, генеруємо об'єкт Track із цим посиланням.
            elif data_type == "DirectUrl":

                track = await self.wavelink_pool.fetch_tracks(data)
                track = track[0]

            # Якщо нічого не спрацювало, значить це не те посилання яке підтримує бот.
            else:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NOT-VALID-URL"))
                return

            # Якщо знайдений бот зайнятий, додати пісню йому у плейліст.
            if bot.busy:
                await bot.playlist_add(track)  # Додаємо пісню у плейлист.
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.ADDED-TO-PLAYLIST", placehoders={"{{NAME}}": track.title, "{{DURATION}}": self.seconds_to_time(track.length), "{{BOT}}": bot.bot.user.mention}))  # Відправляємо повідомлення про успіх.
                return

            # Якщо знайдений бот не зайнятий, під'єднати його до каналу користувача.
            else:
                await bot.connect(track=track, channel=ctx.user.voice.channel)  # Підключаємо нового знайденого бота у голосовий канал.
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-CONNECTED", placehoders={"{{NAME}}": track.title, "{{DURATION}}": self.seconds_to_time(track.length), "{{BOT}}": bot.bot.user.mention, "{{CHANNEL}}": ctx.user.voice.channel.mention}))  # Відправляємо повідомлення про успіх.
                return


        except Exception as error: self.if_error_occurred(error=error, ctx=ctx, place="commands.playcommand()", command="/play")

    # Обробка команди /skip
    @commands.slash_command(name="skip", description="Запускає наступну пісню з плейлісту музичного бота.")
    async def skip_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            if self.get_user_blocked_status(user_id=str(ctx.user.id)) == "True":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.YOU-MUSIC-BANNED"))
                return

            # Перевіряємо чи знаходиться користувач у голосовому каналі.
            if ctx.user.voice is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))
                return

            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel) # Шукаємо бота у голосовому каналі користувача.

            # Якщо бота не знайдено, вивести помилку.
            if bot is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))
                return

            if bot.locked and not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BOT-LOCKED", placehoders={"{{BOT}}": bot.bot.user.mention}))
                return

            info = bot.skip() # Скіпаємо пісню у боті й отримуємо інформацію про скіпнуту пісню та наступну пісню.

            # Якщо пісень у черзі бота немає, вивести повідомлення про відключення бота.
            if info == "STOP":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-STOPPED", placehoders={"{{BOT}}": bot.bot.user.mention}))  # Відправляємо повідомлення про успіх.
                return

            # Виводимо повідомлення про скіп пісні якщо все пройшло нормально.
            # noinspection PyTypeChecker
            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.SONG-SKIPPED", placehoders={"{{SKIPPED_SONG_NAME}}": info['current_song'].title, "{{PLAYING_SONG_NAME}}": info['next_song'].title, "{{PLAYING_SONG_DURATION}}": self.seconds_to_time(info['next_song'].length), "{{BOT}}": bot.bot.user.mention}))

        except Exception as error: self.if_error_occurred(error=error, ctx=ctx, place="commands.skipcommand()", command="/skip")

    # Обробка команди /back
    @commands.slash_command(name="back", description="Запускає минулу пісню з плейлісту музичного бота.")
    async def back_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            if self.get_user_blocked_status(user_id=str(ctx.user.id)) == "True":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.YOU-MUSIC-BANNED"))
                return

            # Перевіряємо чи знаходиться користувач у голосовому каналі.
            if ctx.user.voice is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))
                return

            # Шукаємо музичного бота у голосовому каналі користувача.
            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel)

            # Якщо бота не знайдено, відправити помилку.
            if bot is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))
                return

            # Перевіряємо чи бот заблокований.
            if bot.locked and not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BOT-LOCKED", placehoders={"{{BOT}}": bot.bot.user.mention}))
                return

            info = bot.back() # Виконуємо дію повтору минулої пісні й отримуємо інформацію.

            # Якщо ми оримали інформацію про завершення роботи бота, відправляємо повідомлення.
            if info == "STOP":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-STOPPED", placehoders={"{{BOT}}": bot.bot.user.mention}))  # Відправляємо повідомлення про успіх.
                return

            # noinspection PyTypeChecker
            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.SONG-BACKKED", placehoders={"{{NAME}}": info.title, "{{DURATION}}": self.seconds_to_time(info.length), "{{BOT}}": bot.bot.user.mention}))

        except Exception as error: self.if_error_occurred(error=error, ctx=ctx, place="commands.backcommand()", command="/back")
    # Обробка команди /stop
    @commands.slash_command(name="stop", description="Зупиняє музичного бота у вашому каналі.")
    async def stop_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            if self.get_user_blocked_status(user_id=str(ctx.user.id)) == "True":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.YOU-MUSIC-BANNED"))
                return

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку.

            # Перевіряємо чи знаходиться користувач у голосовому каналі.
            if ctx.user.voice is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))
                return

            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel) # Шукаємо музичного бота у голосовому каналі користувача.

            # Якщо бота не знайдено, посилаємо користувача до путіна на колінки.
            if bot is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))
                return

            # Перевіряємо чи бот заблокований і, чи є у користувача доступ до нього.
            if bot.locked and not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BOT-LOCKED", placehoders={"{{BOT}}": bot.bot.user.mention}))

                return

            bot.disconnect() # Зупиняємо бота.

            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-STOPPED", placehoders={"{{BOT}}": bot.bot.user.mention})) # Відправляємо повідомлення про успіх.

        except Exception as error: self.if_error_occurred(error=error, ctx=ctx, place="commands.stopcommand()", command="/stop")

    # Обробка команди /loop
    @commands.slash_command(name="loop", description="Керує режимом повторення пісень музичного бота.", options=[disnake.Option(name="loop", description="Виберіть тип повторення.", required=True, type=3, choices=[disnake.OptionChoice(name="Вимкнути", value="False"), disnake.OptionChoice(name="Плейліст", value="Playlist"), disnake.OptionChoice(name="Один трек", value="Track")])])
    async def loop_command(self, ctx: disnake.ApplicationCommandInteraction, loop: str):

        try:

            if self.get_user_blocked_status(user_id=str(ctx.user.id)) == "True":
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.YOU-MUSIC-BANNED"))
                return

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING"))

            if ctx.user.voice is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))
                return

            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel)

            if bot is None:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))
                return

            if bot.locked and not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.BOT-LOCKED", placehoders={"{{BOT}}": bot.bot.user.mention}))
                return

            # У залежності від опції яку вибрав користувач, відправляємо повідомлення про зміну режиму повторення і звичайно ж змінюємо режим повторення бота.
            if loop == "False":
                bot.loop = "False"
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.LOOP-DISABLED", placehoders={"{{BOT}}": bot.bot.user.mention})) # Відправляємо повідомлення про успіх.

            elif loop == "Playlist":
                bot.loop = "PLAYLIST"
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.LOOP-SET", placehoders={"{{BOT}}": bot.bot.user.mention, "{{MODE}}": "Повторення плейлисту"}))  # Відправляємо повідомлення про успіх.

            elif loop == "Track":
                bot.loop = "TRACK"
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.LOOP-SET", placehoders={"{{BOT}}": bot.bot.user.mention, "{{MODE}}": "Повторення поточної пісні"}))  # Відправляємо повідомлення про успіх.

        except Exception as error:

            self.if_error_occurred(error=error, ctx=ctx, place="commands.loopcommand()", command="/loop")

    # Обробка команди /playlist
    @commands.slash_command(name="playlist", description="Відображає плейліст музичного бота.")
    async def playlist_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(content="Обробка...")

            if ctx.user.voice is None:
                await ctx.edit_original_response(content="Ви не знаходитесь у голосовому каналі.")
                return

            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel)

            if bot is None:
                await ctx.edit_original_response(content="У вашому каналі немає музичного бота.")
                return

            await ctx.edit_original_response(content="Тіпа плейліст.")

        except Exception as error:

            self.if_error_occurred(error=error, ctx=ctx, place="commands.playlistcommand()", command="/playlist")

    # Обробка команди /musicbot-lock
    @commands.slash_command(name="musicbot-lock", description="Блокує/Розблокує музичного бота.")
    async def lock_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            # Перевіряємо чи увімкнена команда.
            if not self.lock_command_enabled:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.COMMAND-DISABLED"))

                return

            # Перевіряємо чи має користувач доступ до цієї команди.
            if not SBDS.utils.checkUserRoles(roles_id_list=self.lock_command_allowed_roles_ids, member=ctx.user):

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.NO-PERMISSION"))

                return

            # Перевіряємо чи знаходиться користувач у голосовому каналі.
            if ctx.user.voice is None:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))

                return

            # Шукаємо музичного бота у голосовому каналі користувача.
            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel)

            # Якщо бота не знадйено, виводимо помилку.
            if bot is None:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))

                return

            # Встановлюємо статус блокування бота.
            if not bot.locked:

                bot.locked = True
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-LOCK-ENABLE", placehoders={"{{BOT}}": bot.bot.user.mention}))

            else:

                bot.locked = False
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-LOCK-DISABLE", placehoders={"{{BOT}}": bot.bot.user.mention}))

        except Exception as error:

            self.if_error_occurred(error=error, ctx=ctx, place="commands.lockcommand()", command="/musicbot-lock")

    # Обробка команди /musicbot-24_7
    @commands.slash_command(name="musicbot-24_7", description="Переключає режим 24 на 7.")
    async def mode_24_7_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            # Перевіряємо чи увімкнена команда.
            if not self.mode_24_7_command_enabled:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.COMMAND-DISABLED"))

                return

            # Перевіряємо чи має користувач доступ до команди.
            if not SBDS.utils.checkUserRoles(roles_id_list=self.mode_24_7_command_allowed_roles_ids, member=ctx.user):

                await ctx.edit_original_message(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.NO-PERMISSION"))

                return

            # Перевіряємо чи є користувач у голосовому каналі.
            if ctx.user.voice is None:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.USER-NOT-IN-VOICE-CHANNEL"))

                return

            # Шукаємо музичного бота у голосовому каналі користувача.
            bot = self.findBotInVoice(voice_channel=ctx.user.voice.channel)

            # Якщо бота не знайдено виводимо помилку.
            if bot is None:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.NO-BOT-IN-USER-VOICE"))

                return

            # Встановлюємо режим 24 на 7.
            if not bot.mode_24_7: bot.mode_24_7 = True
            else: bot.mode_24_7 = False

            # Відправляємо повідомлення про успіх.
            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.music-module.embeds.BOT-24_7-MODE-TOGGLE", placehoders={"{{BOT}}": bot.bot.user.mention, "{{STATE}}": str(bot.mode_24_7).replace("True", "увімкнено").replace("False", "вимкнено")}))

        except Exception as error:

            self.if_error_occurred(error=error, ctx=ctx, place="commands.mode_24_7_command()", command="/musicbot-24_7")

    # Обробка команди /musicbot-ban
    @commands.slash_command(name="musicbot-ban", description="Блокує/Розблокує користувача від використання музичного бота.", options=[disnake.Option(name="member", description="Учасник на якому виконати команду.", type=6, required=True)])
    async def ban_command(self, ctx: disnake.ApplicationCommandInteraction, member: disnake.Member):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

            # Перевіряємо чи увімкнена команда.
            if not self.ban_command_enabled:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.COMMAND-DISABLED"))

                return

            # Перевірка чи вибраний користувач це бот.
            if member.bot:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.CANNOT-USE-BOT"))

                return

            # Перевіряємо чи хоче користувач заблокувати сам себе.
            if str(ctx.user.id) == str(member.id) and self.ban_command_self_block:

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.CANNOT-USE-SELF"))

                return

            # Перевіряємо чи хоче заблокувати користувач когось із модераторів.
            if self.ban_command_stuff_block and SBDS.utils.checkUserRoles(
                    roles_id_list=self.ban_command_allowed_roles_ids, member=member):

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.CANNOT-BLOCK-STUFF"))

                return

            # Перевіряємо чи має користувач доступ до команди.
            if not SBDS.utils.checkUserRoles(roles_id_list=self.ban_command_allowed_roles_ids, member=ctx.user):

                await ctx.edit_original_message(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.NO-PERMISSION"))

                return

            ban_state = self.get_user_blocked_status(user_id=str(member.id))

            if ban_state == "True":

                SBDS.database.setUserKey(user_id=str(member.id), key="musicbot-banned", value="False")

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.USER-MUSIC-UNBANNED", placehoders={"{{USER}}": f"<@{member.id}>"}))

            else:

                SBDS.database.setUserKey(user_id=str(member.id), key="musicbot-banned", value="True")

                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.music-module.embeds.USER-MUSIC-BANNED", placehoders={"{{USER}}": f"<@{member.id}>"}))

        except Exception as error:

            self.if_error_occurred(error=error, ctx=ctx, place="commands.musicbot-bancommand()", command="/musicbot-ban")

_cog = _MusicModuleCog()
MusicBotsApi = Api()


def setup(self: commands.AutoShardedInteractionBot):

    # Перевірка існування налаштувань модуля.
    SBDS.settings.checkKeys(check_keys={"music-module": dict}, path="modules")

    # Перевірка існування налаштувань LavaLink
    SBDS.settings.checkKeys(check_keys={"lavalink": dict}, path="modules.music-module")
    SBDS.settings.checkKeys(check_keys={"host": str, "port": int, "password": str}, path="modules.music-module.lavalink")

    # Перевірка існування загальних налаштувань модуля.
    SBDS.settings.checkKeys(check_keys={"spotify": dict, "youtube-support-enabled": bool, "direct-media-support-enabled": bool, "music-bots": list, "commands": dict, "embeds": dict, "channels-black-list": list, "music-name-black-list": list, "music-name-black-list-bypass-roles": list}, path="modules.music-module")
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "spotify-api-client-id": str, "spotify-api-client-secret": str}, path="modules.music-module.spotify") # Перевіра існування налаштувань Spotify.

    # Перевірка існування налаштувань команд модуля.
    SBDS.settings.checkKeys(check_keys={"ban-command": dict, "lock-command": dict, "24_7-mode-command": dict}, path="modules.music-module.commands")
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "block-self-use": bool, "block-stuff-use": bool, "allowed-roles": list}, path="modules.music-module.commands.ban-command") # /musicbot-ban
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "allowed-roles": list}, path="modules.music-module.commands.lock-command") # /musicbot-lock
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "allowed-roles": list}, path="modules.music-module.commands.24_7-mode-command") # /musicbot-24_7

    # Перевірка існування embed повідомлень модуля.
    SBDS.settings.checkKeys(check_keys={
        "LOADING": dict,
        "NO-PERMISSION": dict,
        "BLOCKED-CHANNEL": dict,
        "NOT-VALID-URL": dict,
        "BLACK-LISTED-WORDS-FOUND": dict,
        "USER-NOT-IN-VOICE-CHANNEL": dict,
        "NO-BOT-IN-USER-VOICE": dict,
        "FREE-BOT-NOT-FOUND": dict,
        "BOT-LOCKED": dict,
        "BOT-LOCK-ENABLE": dict,
        "BOT-LOCK-DISABLE": dict,
        "BOT-24_7-MODE-TOGGLE": dict,
        "NO-RESULT-FOUND": dict,
        "ADDED-TO-PLAYLIST": dict,
        "BOT-CONNECTED": dict,
        "SONG-SKIPPED": dict,
        "CANNOT-SKIP-LOOP-MODE": dict,
        "SONG-BACKKED": dict,
        "BOT-STOPPED": dict,
        "LOOP-DISABLED": dict,
        "LOOP-SET": dict,
        "USER-MUSIC-BANNED": dict,
        "USER-MUSIC-UNBANNED": dict,
        "YOU-MUSIC-BANNED": dict,
        "CANNOT-USE-SELF": dict,
        "CANNOT-USE-BOT": dict,
        "CANNOT-BLOCK-STUFF": dict,
        "LOADING-STREAM-URL": dict
    }, path="modules.music-module.embeds")

    self.add_cog(_cog)






