#    _____                  _             ______
#   / ___/__  ________   __(_)   ______ _/ / __ )____  ____  ____ ___
#   \__ \/ / / / ___/ | / / / | / / __ `/ / __  / __ \/ __ \/ __ `__ \
#  ___/ / /_/ / /   | |/ / /| |/ / /_/ / / /_/ / /_/ / /_/ / / / / / /
# /____/\__,_/_/    |___/_/ |___/\__,_/_/_____/\____/\____/_/ /_/ /_/
# SurvivalBoom Network 2023 | SurvivalBoom Discord Service
#     Private Voices Module | By TIMURishche
#
#
import asyncio
import json
import time
import traceback
import disnake
from disnake.ext import commands, tasks
from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")
class Channel:
    """
    Об'єкт голосового каналу. Зберігає у собі усі налаштування цього каналу й іншу інформацію.
    Кожен такий об'єкт додається у список з активними каналами.

    Параметри
    ----------
    owner: :class:`disnake.Mebmer` -- Власник цього каналу.
    channel: :class:`disnake.VoiceChannel` -- Голосовий канал до якого прікріплений цей об'єкт.
    userdata: :class:`dict` -- Інформація про цей канал із бази даних.
    """

    # Ініціалізація каналу.
    def __init__(self, owner: disnake.Member, channel: disnake.VoiceChannel, userdata: dict):

        # Ну тіпа змінні, угу...
        self._owner: disnake.Member = owner # Власник каналу.
        self._channel: disnake.VoiceChannel = channel # Discord канал.
        self._control_panel_msg: disnake.Message | None = None  # Повідомлення палі керування каналом.

        self.blocked_users: dict[int, disnake.Member] = {} # Заблоковані користувачі у каналі.
        self.allowed_users: dict[int, disnake.Member] = {}  # Дозволені користувачі у каналі.
        self.muted_users: dict[int, disnake.Member] = {}  # Замучені корситувачі у каналі.


        self._limit: int = userdata['channel-limit'] # Підвантажуємо ліміт користувачів у каналі із бази даних.
        self.rename_time = None

        # Підвантажуємо статус каналу із бази даних. Далі мені лінь писати...
        self._locked: bool = userdata['locked']
        self._hidden: bool = userdata['hidden']

        # Підвантажуємо заблокованих користувачів із бази даних.
        for idd in userdata['blocked-users']:
            member = cog.guild.get_member(idd)
            self.blocked_users.update({idd: member})

        # Підвантажуємо дозволених користувачів із бази даних.
        for idd in userdata['allowed-users']:
            member = cog.guild.get_member(idd)
            self.allowed_users.update({idd: member})

        # Підвантажуємо замучених користувачів із бази даних.
        for idd in userdata['muted-users']:
            member = cog.guild.get_member(idd)
            self.muted_users.update({idd: member})

        # Огризок коду. ги-ги-ги, як смішно.
        async def start():
            await asyncio.sleep(2)
            self._voice_task.start()
            self._voice_task.get_task().set_name(f"{module} - voice_task()")

        # Стартуємо voice_task() цього каналу.
        asyncio.create_task(start()).set_name(f"{module} - _Start voice_task()")

    @property # Метод отримання прикріпленого голосового каналу до цього приватного каналу.
    def channel(self): return self._channel

    @property # Метод отримання власника цього приватного каналу.
    def owner(self): return self._owner

    @property
    def locked(self): return self._locked

    @property
    def hidden(self): return self._hidden

    async def clear_channel(self):
        await self._channel.purge()
        self._control_panel_msg = None

    # Метод встановлення значення блокування канала.
    def set_lock(self, state: bool) -> None: self._locked = state

    # Метод встановлення видимості канала.
    def set_hidden(self, state: bool) -> None: self._hidden = state

    # Метод дозволення користувачів у каналі якщо канал був заблокований або схований.
    def add_connected_users_as_allowed_users(self) -> None:
        if not cog.module_settings['auto-whitelist']: return
        for member in self._channel.members:
            if not member.bot and not member.id in self.allowed_users and member.id != self._owner.id: self.allowed_users.update({member.id: member})


    # Метод зберігання налаштувань каналу у базу даних.
    # Тобто усі налаштування (Блокування, назва, ліміти та і.н) зберігаються у даних власника каналу.
    def _save_channel_settings(self):

        # Конвертуємо дозволених користувачів у json-fiendly формат.
        allowed_users: list[int] = []
        for a in self.allowed_users: allowed_users.append(a)

        # Конвертуємо заблокованих користувачів у json-fiendly формат.
        blocked_users: list[int] = []
        for a in self.blocked_users: blocked_users.append(a)

        # Конвертуємо замучних користувачів у json-fiendly формат.
        muted_users: list[int] = []
        for a in self.muted_users: muted_users.append(a)

        # Зберігаємо дані.
        data = {"channel-name": self.channel.name, "channel-limit": self._limit, "allowed-users": allowed_users, "blocked-users": blocked_users, "muted-users": muted_users, "locked": self._locked, "hidden": self._hidden}
        SBDS.database.setUserKey(user_id=str(self.owner.id), key="private-voice", value=json.dumps(data))

    # Метод встановлення нового власника цього канала.
    # При зміні власника дані про канал зберігаються для старого власника.
    def set_owner(self, owner: disnake.Member) -> None:

        self._save_channel_settings() # Зберігаємо нахуй нікому не потрібні дані.

        # Ну тіпа встановлюємо нового власника, угу...
        cog.channels.pop(self._owner.id)
        self._owner = owner
        cog.channels.update({self._owner.id: self})

    # Метод видалення каналу. Перед видаленням усі учасники переміщаються у бекап-канал й дані про канал зберігаються у даних власника канала.
    async def delete_channel(self):

        self._save_channel_settings() # Зберігаємо налаштування каналу.

        self._voice_task.stop() # Зупиняємо voice_task() каналу.

        cog.channels.pop(self._owner.id) # Видаляємо цей канал зі списку з активними каналами.

        # Переміщаємо кожного учасника канала у бекап-канал.
        if cog.module_settings['use-backup-channel']:
            for member in self._channel.members:

                try: await member.move_to(channel=cog.backup_channel, reason="SBDS - PrivateVoiceModule ChannelDeleting")
                except disnake.HTTPException: pass

        # Видаляємо Discord канал.
        await self._channel.delete(reason="SBDS - PrivateVoiceModule ChannelDelete")

    # Метод генерації повідомлення контрольнох панелі.
    # На виході отримуємо embed повідомлення.
    def _generate_control_panel(self) -> disnake.Embed:

        # Перекладаємо статус каналу із bool на нормальний текст.
        locked = str(self._locked).replace("True", "Так").replace("False", "Ні")
        hidden = str(self._hidden).replace("True", "Так").replace("False", "Ні")

        # Перекладаємо список з айдішниками заблокованих користувачів у нормальний вигляд.
        blocked_users = "Немає"
        if len(self.blocked_users) > 0: blocked_users = ", ".join([self.blocked_users[user].display_name for user in self.blocked_users])

        # Перекладаємо список з айдішниками замучених користувачів у нормальний вигляд.
        muted_users = "Немає"
        if len(self.muted_users) > 0: muted_users = ", ".join([self.muted_users[user].display_name for user in self.muted_users])

        # Перекладаємо список з айдішниками дозволених користувачів у нормальний вигляд.
        allowed_users = "Немає"
        if len(self.allowed_users) > 0: allowed_users = ", ".join([self.allowed_users[user].display_name for user in self.allowed_users])

        # Поміщаємо значення у плейсхолдери.
        placeholders = {"{{CHANNEL}}": f"<#{self._channel.id}>", "{{OWNER}}": f"<@{self._owner.id}>", "{{LOCKED}}": locked, "{{HIDDEN}}": hidden, "{{BLOCKED_USERS}}": blocked_users, "{{MUTED_USERS}}": muted_users, "{{ALLOWED_USERS}}": allowed_users}

        # Повертаємо embed повідомлення.
        return SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CONTROL-PANEL", placehoders=placeholders)

    # Метод генерування dropdown списків під повідомленням контрольної панелі.
    def _generate_actions_dropdown(self) -> disnake.ui.View:

        items = disnake.ui.View() # Ствоюємо об'єкт кнопочок.

        c = cog.dropdown_settings.copy()

        # Створюємо і додаємо варіанти у список із налаштуваннями каналу.
        channel_settings = [
            disnake.SelectOption(label=c['set-name']['title'], description=c['set-name']['description'], value="SetName"),
            disnake.SelectOption(label=c['set-limit']['title'], description=c['set-limit']['description'], value="SetLimit"),
            disnake.SelectOption(label=c['set-bitrate']['title'], description=c['set-bitrate']['description'], value="SetBitrate"),
            disnake.SelectOption(label=c['delete-channel']['title'], description=c['delete-channel']['description'], value="DeleteChannel"),
        ]

        # Створюємо список із варіантами налаштувань користувачів.
        channel_permissions = []

        # Перевірємо статус блокування каналу і додаємо відповідну кнопку у список.
        if self._locked: channel_permissions.append(disnake.SelectOption(label=c['unlock-channel']['title'], description=c['unlock-channel']['description'], value="UnlockChannel"))
        else: channel_permissions.append(disnake.SelectOption(label=c['lock-channel']['title'], description=c['lock-channel']['description'], value="LockChannel"))

        # Перевірємо статус видимості каналу і додаємо відповідну кнопку у список.
        if self._hidden: channel_permissions.append(disnake.SelectOption(label=c['show-channel']['title'], description=c['show-channel']['description'], value="ShowChannel"))
        else: channel_permissions.append(disnake.SelectOption(label=c['hide-channel']['title'], description=c['hide-channel']['description'], value="HideChannel"))

        # Додаємо у список інші кнопки.
        channel_permissions.append(disnake.SelectOption(label=c['block-control']['title'], description=c['block-control']['description'], value="BanUser"))
        channel_permissions.append(disnake.SelectOption(label=c['whitelist-control']['title'], description=c['whitelist-control']['description'], value="AllowUser"))
        channel_permissions.append(disnake.SelectOption(label=c['mute-control']['title'], description=c['mute-control']['description'], value="MuteUser"))
        channel_permissions.append(disnake.SelectOption(label=c['give-ownership']['title'], description=c['give-ownership']['description'], value="GiveOwnership"))
        channel_permissions.append(disnake.SelectOption(label=c['take-ownership']['title'], description=c['take-ownership']['description'], value="TakeOwnership"))

        # Додаємо dropdown списки до об'єкту із кнопочками.
        items.add_item(disnake.ui.StringSelect(placeholder=c['placeholders']['edit-channel-dropdown'], min_values=1, max_values=1, options=channel_settings, custom_id="SurvivalBoomPrivateVoices_EditChannel"))
        items.add_item(disnake.ui.StringSelect(placeholder=c['placeholders']['users-dropdown'], min_values=1, max_values=1, options=channel_permissions, custom_id="SurvivalBoomPrivateVoices_Users"))

        # Повертаємо кнопочки.
        return items

    # Метод оновлення контрольної панелі.
    # Банально: Генерує embed повідомлення і кнопки під ним. Потім змінює що існує повідомлення.
    def update_control_panel(self):
        asyncio.create_task(self._control_panel_msg.edit(embed=self._generate_control_panel(), view=self._generate_actions_dropdown())).set_name(f"{module} - Update Control Panel")

    # Серце приватного каналу.
    # noinspection PyDunderSlots,PyUnresolvedReferences
    @tasks.loop(seconds=1)
    async def _voice_task(self):

        try:

            # Видалення каналу якщо у ньому нікого немає.
            members = [member for member in self._channel.members if not member.bot] # Генеруємо список усіх учасників каналу без ботів.
            # Якщо у списку нікого немає, запускаємо видалення.
            if not members:
                name = self._channel.name
                await self.delete_channel()
                cog.logger.info(f"Видалено приватний канал &b#{name}&r.")

            # Відправлення повідомлення із контрольною панеллю якщо воно ще не було відправлено.
            if self._control_panel_msg is None: self._control_panel_msg = await self._channel.send(embed=self._generate_control_panel(), view=self._generate_actions_dropdown())

            #
            # Зміна доступності каналу.
            #

            e_ovw = self._channel.overwrites[cog.guild.default_role] # Отримуємо дозволи ролі евріван у каналі.

            # Блокуємо чи розблокуємо підлючення до каналу ролі евріван (тобто усім учасникам) у залежності від обставин.

            # Якщо канал заблоковано й роль евріван має право заходити у канал, заборонити їй це робити.
            if self._locked and e_ovw.connect is not False and not self._hidden:

                e_ovw.connect = False
                await self._channel.set_permissions(target=cog.guild.default_role, overwrite=e_ovw)

            # Якщо канал не заблоковано й роль евріван не має право заходити у канал, дозволити їй це робити.
            if not self._locked and e_ovw.connect is False:

                e_ovw.connect = None
                await self._channel.set_permissions(target=cog.guild.default_role, overwrite=e_ovw)


            # Ховаємо чи показуємо канал ролі евріван (тобто усім учасникам) у залежності від обставин.

            # Якщо канал сховано й роль евріван бачить канал, заборонити їй це робити.
            if self._hidden and e_ovw.view_channel is not False:

                e_ovw.view_channel = False
                await self._channel.set_permissions(target=cog.guild.default_role, overwrite=e_ovw)

            # Якщо канал не сховано й роль евріван не бачить канал, дозволити їй це робити.
            if not self._hidden and e_ovw.view_channel is False:

                e_ovw.view_channel = True
                await self._channel.set_permissions(target=cog.guild.default_role, overwrite=e_ovw)


            # Видаємо власнику канала імунітет якщо його немає.
            if self._owner in self._channel.overwrites:
                o_owv = self._channel.overwrites[self._owner]
                if o_owv.view_channel is not True or o_owv.connect is not True:

                    o_owv.connect = True
                    o_owv.view_channel = True
                    await self._channel.set_permissions(target=self._owner, overwrite=o_owv)

            else: await self._channel.set_permissions(target=self._owner, view_channel=True, connect=True)


            # Отримуємо персональні дозволи користувачів у каналі.
            users_overwrites: dict[disnake.Member, disnake.PermissionOverwrite] = {}
            for overwrite in self._channel.overwrites:
                if isinstance(overwrite, disnake.Member) and overwrite.id != self._owner.id: users_overwrites.update({overwrite: self._channel.overwrites[overwrite]})

            #
            # Заборона/Розборона (що блін?) конкретним користувачам заходити у канал.
            #

            # Автоматичне видалення із дозволених користувачів якщо кристувач заблокований.
            for user in self.allowed_users.copy():
                if user in self.blocked_users: self.allowed_users.pop(user)

            # Блокування заблокованих користувачів у каналі.
            for user in self.blocked_users.copy():

                userr = self.blocked_users[user] # Отримуємо об'єкт заблокованого користувача.

                # Якщо ніяких дозволів не було додано користувачу, заблокувати користувача у каналі.
                if not userr in self._channel.overwrites:
                    await self._channel.set_permissions(target=userr, connect=False)
                    continue

                # Якщо користувач не заблокований, заблокувати його у каналі.
                if self.channel.overwrites[userr].connect is not False:

                    ov = self._channel.overwrites[userr]
                    ov.connect = False
                    await self._channel.set_permissions(target=userr, overwrite=ov)

            # Дозволення підлючатись дозволеним користувачам.
            for user in self.allowed_users.copy():

                userr = self.allowed_users[user] # Отримуємо об'єкт заблокованого користувача.

                # Якщо ніяких дозволів не було додано користувачу, дозволити користувача у каналі.
                if not userr in self._channel.overwrites:
                    await self._channel.set_permissions(target=userr, connect=True, view_channel=True)
                    continue

                # Якщо користувач не дозволений, дозволити його у каналі.
                if self._channel.overwrites[userr].connect is not True:
                    overwrite = self._channel.overwrites[userr]
                    overwrite.connect = True
                    overwrite.view_channel = True
                    await self._channel.set_permissions(target=userr, overwrite=overwrite)

            # Мут замучених користувачів.
            for user in self.muted_users.copy():

                userr = self.muted_users[user]  # Отримуємо об'єкт заблокованого користувача.

                # Якщо ніяких дозволів не було додано користувачу, замутити й перепід'єднати користувача до канала.
                if not userr in self.channel.overwrites:
                    await self._channel.set_permissions(target=userr, speak=False) # Встановлюємо дозвіл.
                    if userr.voice is not None and userr.voice.channel.id == self._channel.id and cog.module_settings['use-backup-channel']:
                        try:
                            await userr.move_to(channel=cog.backup_channel) # Переміщаємо користувача у бекап канал.
                            await userr.move_to(channel=self._channel) # Повертаємо користувача назад.
                        except disnake.HTTPException: pass
                        continue

                # Якщо ніяких дозволів не було додано користувачу, замутити користувача у каналі.
                if self.channel.overwrites[userr].speak is not False:
                    overwrite = self._channel.overwrites[userr]
                    overwrite.speak = False
                    await self._channel.set_permissions(target=userr, overwrite=overwrite)
                    if userr.voice is not None and userr.voice.channel.id == self._channel.id and cog.module_settings['use-backup-channel']:
                        try:
                            await userr.move_to(channel=cog.backup_channel)  # Переміщаємо користувача у бекап канал.
                            await userr.move_to(channel=self._channel)  # Повертаємо користувача назад.
                        except disnake.HTTPException:
                            pass

            # Дії скасування.
            for user in users_overwrites:

                overwrites = users_overwrites[user] # Отримуємо дозволи учасника.

                # Якщо користувач не заблокований, але не має дозволу зайти у канал дозволяємо йому це.
                if not user.id in self.blocked_users and overwrites.connect is False:

                    overwrites.connect = None
                    await self._channel.set_permissions(target=user, overwrite=overwrites)

                # Якщо користувач не замучений, але не має дозволу говорити у каналі дозволяємо йому це.
                if not user.id in self.muted_users and overwrites.speak is False:

                    overwrites.speak = None
                    await self._channel.set_permissions(target=user, overwrite=overwrites)

                    if user.voice is not None and user.voice.channel.id == self._channel.id and cog.module_settings['use-backup-channel']:

                        try:
                            await user.move_to(channel=cog.backup_channel)  # Переміщаємо користувача у бекап канал.
                            await user.move_to(channel=self._channel)  # Повертаємо користувача назад.
                        except disnake.HTTPException: pass

                # Якщо користувач не дозволений, але має дозвіл до каналу, забороняємо йому це.
                if not user.id in self.allowed_users:
                    if overwrites.connect or overwrites.view_channel:

                        overwrites.connect = None
                        overwrites.view_channel = None
                        await self._channel.set_permissions(target=user, overwrite=overwrites)

                # Якщо користувач взагалі ніяк не причетний до цього каналу видаляємо його із довзволів каналу.
                if not user.id in self.blocked_users and not user.id in self.allowed_users and not user.id in self.muted_users: await self._channel.set_permissions(target=user, overwrite=None)

            #
            # Захист від проблем.
            #

            # Якщо модератор знаходиться у списку дозволених користувачів, прибрати його звідти.
            for user in self.allowed_users.copy():
                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=self.allowed_users[user]): self.allowed_users.pop(user)

            # Якщо модератор знаходиться у списку замучених користувачів, прибрати його звідти.
            for user in self.muted_users.copy():
                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=self.muted_users[user]): self.muted_users.pop(user)

            # Якщо модератор знаходиться у списку заблокованих користувачів, прибрати його звідти.
            for user in self.blocked_users.copy():
                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=self.blocked_users[user]): self.blocked_users.pop(user)

            # Якщо власник каналу заблокований лошпєд, розблокувати його.
            if self._owner.id in self.blocked_users: await self.blocked_users.pop(self._owner.id)

            # Якщо заблокований користувач якимось магічним чином знаходиться у каналі, перемістити у бекап канал.
            for member in self._channel.members:
                if member.id in self.blocked_users:
                    if cog.module_settings['use-backup-channel']: await member.move_to(channel=cog.backup_channel)
                    else: # noinspection PyTypeChecker
                        await member.move_to(channel=None)

            # Якщо не дозволений користувач знаходиться у заблокованому каналі (або прихованому), перемістити у бекап канал.
            if self._locked or self._hidden:
                for member in self._channel.members:

                    if member.id == self._owner.id or member.id in self.allowed_users.copy() or member.bot or SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=member): continue
                    if cog.module_settings['use-backup-channel']: await member.move_to(channel=cog.backup_channel)
                    else: # noinspection PyTypeChecker
                        await member.move_to(channel=None)

            # Якщо власник каналу замучений лошпєд, розмутити його.
            if self._owner.id in self.muted_users: self.muted_users.pop(self._owner.id)

            # Забрати імунітет в учасників. Наприклад якщо права власності були передані іншому учаснику.
            for user in users_overwrites:
                ov = users_overwrites[user]
                if not user.id in self.allowed_users:
                    if ov.connect or ov.view_channel: await self._channel.set_permissions(target=user, connect=None, view_channel=None)

        except Exception as error:

            try: await self.delete_channel()
            except: pass

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, place=f"{self._channel.name}.tasks.voice_task()", module=module)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place=f"{self._channel.name}.tasks.voice_task()")

# Клас модуля.
class PrivateVoiceCog(commands.Cog):


    def __init__(self):

        self.create_channel: disnake.VoiceChannel = ...  # Канал у який треба зайти, щоб приватний канал створився.
        self.work_category: disnake.CategoryChannel = ...  # Категорія у якій буде працювати бот.
        self.backup_channel: disnake.VoiceChannel = ...  # Бекап канал.
        self.guild: disnake.Guild = ...  # Сервер на якому буде працювати бот.
        self.moderator_roles: list[int] = ...
        self.dropdown_settings: dict[str, dict[str, str]] = ...
        self.logger: SBDS.mainlogger.ModuleLogger = ...

        self.module_settings: dict = ...  # Налаштування цього модуля.
        self.channels: dict[int, Channel] = {} # Головний список із активними каналами.

    # Ініціалізація модуля.
    def cog_load(self) -> None:

        self.module_settings = SBDS.settings.get("modules.private-voices-module")
        self.create_voice_task.start()
        self.create_voice_task.get_task().set_name(f"{module} - create_voice_task()")
        self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)

    # Завершення роботи модуля і видалення усіх приватних каналів.
    def cog_unload(self) -> None:
        self.create_voice_task.stop()
        for channel in self.channels: asyncio.create_task(self.channels[channel].delete_channel()).set_name(f"{module} - DeleteChannel")
    @tasks.loop(seconds=2) # Таск створення приватних каналів.
    async def create_voice_task(self):

        try:

            await SBDS.main_bot.wait_until_ready() # Чекаємо поки бот запуститься.

            # Встановлюємо змінні.
            if self.create_channel is ...: self.create_channel = SBDS.main_bot.get_channel(self.module_settings['create-voice-channel-id'])
            if self.backup_channel is ...: self.backup_channel = SBDS.main_bot.get_channel(self.module_settings['backup-voice-channel-id'])
            if self.moderator_roles is ...: self.moderator_roles = self.module_settings['bypass-roles-ids']
            if self.dropdown_settings is ...: self.dropdown_settings = self.module_settings['control-panel-settings']['dropdowns']

            if self.guild is ...: self.guild = SBDS.utils.getGuild()
            if self.work_category is ...: self.work_category = self.create_channel.category

            members = [member for member in self.create_channel.members if not member.bot]
            for member in members:

                if member.id in self.channels:
                    await member.move_to(channel=self.channels[member.id].channel)
                    continue

                try: userdata = json.loads(SBDS.database.getUserData(user_id=str(member.id), key="private-voice"))
                except SBDS.database.KeyNotFound:

                    userdata = {"channel-name": member.display_name, "channel-limit": 20, "blocked-users": [], "allowed-users": [], "muted-users": [], "locked": False, "hidden": False}

                    SBDS.database.addUserKey(user_id=str(member.id), key="private-voice")
                    SBDS.database.setUserKey(user_id=str(member.id), key="private-voice", value=json.dumps(userdata))

                    self.logger.info(f"Додано ключ &3private-voice &rдо даних користувача &b{member.name} &rу базі даних.")

                channel = await self.work_category.create_voice_channel(name=userdata['channel-name'], user_limit=userdata['channel-limit'], overwrites=self.work_category.overwrites)

                ch_obj = Channel(channel=channel, owner=member, userdata=userdata)
                self.channels.update({member.id: ch_obj})

                await member.move_to(channel=channel, reason="SBDS - PrivateVoiceModule")

                self.logger.info(f"Створено приватний голосовий канал &b#{channel.name} &rдля користувача &b{member.name}&r.")

        except KeyError:

            # noinspection PyUnboundLocalVariable
            SBDS.database.delUserKey(user_id=str(member.id), key="private-voice")

            try: # noinspection PyUnboundLocalVariable
                await channel.delete()
            except: pass
        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=tracebackk)
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, place="tasks.create_voice_task()", module=this_module_name)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=this_module_name, place="tasks.create_voice_task()")

    @staticmethod # Обробка дій про натисканні кнопок.
    @SBDS.main_bot.listen("on_dropdown")
    async def control_panel(inter: disnake.MessageInteraction):

        try:

            value = inter.data['values'][0]

            if not inter.data['custom_id'] == "SurvivalBoomPrivateVoices_EditChannel" and not inter.data['custom_id'] == "SurvivalBoomPrivateVoices_Users": return

            if inter.user.voice is None:
                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NOT-IN-PRIVATE-VOICE-CHANNEL"), ephemeral=True, delete_after=10)
                return

            channel = [cog.channels[channel] for channel in cog.channels if cog.channels[channel].channel.id == inter.user.voice.channel.id]

            if not channel:
                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NOT-IN-PRIVATE-VOICE-CHANNEL"), ephemeral=True, delete_after=10)
                return

            channel = channel[0]
            channel.update_control_panel()

            if not SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=inter.user) and not channel.owner.id == inter.user.id and value != "TakeOwnership":
                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NO-PERMISSION"), ephemeral=True, delete_after=10)
                return

            if value == "SetName":

                if channel.rename_time is not None and int(time.time()) < channel.rename_time + 600:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.WAIT-BEFORE-RENAME", placehoders={"{{TIME}}": cog.seconds_to_time(channel.rename_time + 600 - int(time.time()))}), ephemeral=True, delete_after=10)
                    return

                await inter.response.send_modal(
                    custom_id="SurvivalBoomPrivateVoices_SetName",
                    title="Змінити назву канала",
                    components=[disnake.ui.TextInput(label="Уведіть нову назву каналу", placeholder="Супер дупер крута назва...", custom_id="name", style=disnake.TextInputStyle.short, min_length=0, max_length=16)]
                )

                try: modal: disnake.ModalInteraction = await SBDS.main_bot.wait_for("modal_submit", check=lambda i: i.custom_id == "SurvivalBoomPrivateVoices_SetName" and i.author.id, timeout=20)
                except asyncio.TimeoutError:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.TIMED-OUT"), ephemeral=True)
                    return

                channel_name = modal.text_values['name']

                await channel.channel.edit(name=channel_name, reason="SBDS - PrivateVoiceModule RenameChannel")
                channel.rename_time = int(time.time())

                await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-RENAMED", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>", "{{NAME}}": channel_name}), ephemeral=True, delete_after=10)

            elif value == "SetLimit":

                await inter.response.send_modal(
                    custom_id="SurvivalBoomPrivateVoices_SetLimit",
                    title="Встановити ліміт",
                    components=[disnake.ui.TextInput(label="ліміт користувачів", placeholder="999?...", custom_id="limit", style=disnake.TextInputStyle.short, min_length=1, max_length=2)]
                )

                try: modal: disnake.ModalInteraction = await SBDS.main_bot.wait_for("modal_submit", check=lambda i: i.custom_id == "SurvivalBoomPrivateVoices_SetLimit" and i.author.id, timeout=15)
                except asyncio.TimeoutError:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.TIMED-OUT"), ephemeral=True, delete_after=10)
                    return

                try: limit = int(modal.text_values['limit'])
                except:
                    await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NOT-INT", placehoders={"{{TEXT}}": modal.text_values['limit']}), ephemeral=True, delete_after=10)
                    return

                await channel.channel.edit(user_limit=limit, reason="SBDS - PrivateVoiceModule SetLimit")

                await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-SET-LIMIT", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>", "{{LIMIT}}": limit}), ephemeral=True, delete_after=10)

            elif value == "SetBitrate":

                await inter.response.send_modal(
                    custom_id="SurvivalBoomPrivateVoices_SetLimit",
                    title="SurvivalBoom",
                    components=[disnake.ui.TextInput(label="бітрейт", placeholder="Вкажіть значення від 8 до 256...", custom_id="value", style=disnake.TextInputStyle.short, min_length=1, max_length=3)]
                )

                try: modal: disnake.ModalInteraction = await SBDS.main_bot.wait_for("modal_submit", check=lambda i: i.custom_id == "SurvivalBoomPrivateVoices_SetLimit" and i.author.id, timeout=15)
                except asyncio.TimeoutError:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.TIMED-OUT"), ephemeral=True, delete_after=10)
                    return

                try: bitrate = int(modal.text_values['value'])
                except:
                    await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NOT-INT", placehoders={"{{TEXT}}": modal.text_values['limit']}), ephemeral=True, delete_after=10)
                    return

                if bitrate > 256 or bitrate < 8:
                    await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.WRONG-BITRATE"), ephemeral=True, delete_after=10)
                    return

                await channel.channel.edit(bitrate=bitrate * 1000, reason="SBDS - PrivateVoiceModule SetBitrate")

                await modal.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-SET-BITRATE", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>", "{{BITRATE}}": bitrate}), ephemeral=True, delete_after=10)

            elif value == "LockChannel":

                channel.add_connected_users_as_allowed_users()
                channel.set_lock(True)
                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-LOCKED", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)
                channel.update_control_panel()

            elif value == "UnlockChannel":

                channel.set_lock(False)

                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-UNLOCKED", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "HideChannel":

                channel.add_connected_users_as_allowed_users()
                channel.set_hidden(True)
                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-HIDDEN", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)
                channel.update_control_panel()

            elif value == "ShowChannel":

                channel.set_hidden(False)

                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-SHOWN", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "DeleteChannel":

                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CHANNEL-DELETING", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)

                name = channel.channel.name

                await channel.delete_channel()

                cog.logger.info(f"Видалено приватний канал &b#{name}&r.")

            elif value == "TakeOwnership":

                if not SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=inter.user) and channel.owner in channel.channel.members:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.OWNER-STILL-IN-VOICE", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)
                    return

                if inter.user.id == channel.owner.id:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.YOU-ARE-AN-OWNER", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)
                    return

                channel.set_owner(owner=inter.user)

                if channel.locked or channel.hidden: channel.allowed_users.update({inter.user.id: inter.user})

                await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.OWNERSHIP-TAKEN", placehoders={"{{CHANNEL}}": f"<#{channel.channel.id}>"}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "GiveOwnership":

                members = [member for member in channel.channel.members if not member.bot and member.id != channel.owner.id]
                if len(members) < 1:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NO-USERS-IN-CHANNEL", placehoders={"{{CHANNEL}}": channel.channel.mention}), ephemeral=True, delete_after=10)
                    return

                items = disnake.ui.View()

                options = []

                for member in members:
                    if member is not channel.owner: options.append(disnake.SelectOption(label=member.display_name, description=f"Передати права {member.display_name}", value=str(member.id)))

                items.add_item(item=disnake.ui.StringSelect(placeholder="Виберіть користувача...", min_values=1, max_values=1, options=options, custom_id="SurvivalBoomPrivateVoices_GiveOwnership2"))

                await inter.send(view=items, ephemeral=True, delete_after=20)

                try: dropdown: disnake.ModalInteraction = await SBDS.main_bot.wait_for("dropdown", check=lambda i: i.data['custom_id'] == "SurvivalBoomPrivateVoices_GiveOwnership2" and i.author.id, timeout=20)
                except asyncio.TimeoutError: return

                selected_user = SBDS.main_bot.get_user(int(dropdown.data['values'][0]))

                channel.set_owner(selected_user)

                if channel.locked or channel.hidden: channel.allowed_users.update({inter.user.id: inter.user})

                await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.GIVEN-OWNERSHIP", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{NEW_OWNER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "BanUser":

                items = disnake.ui.View()

                items.add_item(item=disnake.ui.UserSelect(custom_id="SurvivalBoomPrivateVoices_BanUser", placeholder="Виберіть користувача...", min_values=1, max_values=1))

                await inter.send(view=items, ephemeral=True, delete_after=20)

                try: dropdown: disnake.ModalInteraction = await SBDS.main_bot.wait_for("dropdown", check=lambda i: i.data['custom_id'] == "SurvivalBoomPrivateVoices_BanUser" and i.author.id, timeout=20)
                except asyncio.TimeoutError: return

                selected_user = SBDS.main_bot.get_user(int(dropdown.data['values'][0]))

                if selected_user.bot:
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.MUST-BE-NOT-BOT"), ephemeral=True, delete_after=10)
                    return

                if selected_user.id == dropdown.user.id:
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.SELF-USE"), ephemeral=True, delete_after=10)
                    return

                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=selected_user):
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CANNOT-DO-THIS-MODERATOR"), ephemeral=True, delete_after=10)
                    return


                if selected_user.id in channel.blocked_users:

                    channel.blocked_users.pop(selected_user.id)

                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-UNBLOCKED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                else:

                    channel.blocked_users.update({selected_user.id: selected_user})

                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-BLOCKED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "MuteUser":

                members = [member for member in channel.channel.members.copy() if not member.bot and member.id != channel.owner.id]
                if len(members) < 1:
                    await inter.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.NO-USERS-IN-CHANNEL", placehoders={"{{CHANNEL}}": channel.channel.mention}), ephemeral=True, delete_after=10)
                    return

                items = disnake.ui.View()

                options = []

                for member in members:
                    if member.id in channel.muted_users: options.append(disnake.SelectOption(label=member.display_name, description=f"Розмутити {member.display_name}", value=str(member.id)))
                    else: options.append(disnake.SelectOption(label=member.display_name, description=f"Замутити {member.display_name}", value=str(member.id)))

                items.add_item(item=disnake.ui.StringSelect(placeholder="Виберіть користувача...", min_values=1, max_values=1, options=options, custom_id="SurvivalBoomPrivateVoices_MuteUser"))

                await inter.send(view=items, ephemeral=True, delete_after=20)

                try: dropdown: disnake.ModalInteraction = await SBDS.main_bot.wait_for("dropdown", check=lambda i: i.data['custom_id'] == "SurvivalBoomPrivateVoices_MuteUser" and i.author.id, timeout=20)
                except asyncio.TimeoutError: return

                selected_user = cog.guild.get_member(int(dropdown.data['values'][0]))

                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=selected_user):
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CANNOT-DO-THIS-MODERATOR"), ephemeral=True, delete_after=10)
                    return

                if selected_user.id in channel.muted_users:

                    channel.muted_users.pop(selected_user.id)
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-UNMUTED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                else:

                    channel.muted_users.update({selected_user.id: selected_user})
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-MUTED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

            elif value == "AllowUser":

                items = disnake.ui.View()

                items.add_item(item=disnake.ui.UserSelect(custom_id="SurvivalBoomPrivateVoices_AllowUser", placeholder="Виберіть користувача...", min_values=1, max_values=1))

                await inter.send(view=items, ephemeral=True, delete_after=20)

                try: dropdown: disnake.ModalInteraction = await SBDS.main_bot.wait_for("dropdown", check=lambda i: i.data['custom_id'] == "SurvivalBoomPrivateVoices_AllowUser" and i.author.id, timeout=20)
                except asyncio.TimeoutError: return

                selected_user = SBDS.main_bot.get_user(int(dropdown.data['values'][0]))

                if SBDS.utils.checkUserRoles(roles_id_list=cog.moderator_roles, member=selected_user):
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.CANNOT-DO-THIS-MODERATOR"), ephemeral=True, delete_after=10)
                    return

                if selected_user.bot:
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.MUST-BE-NOT-BOT"), ephemeral=True, delete_after=10)
                    return

                if selected_user.id == dropdown.user.id:
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.SELF-USE"), ephemeral=True, delete_after=10)
                    return

                if selected_user.id in channel.allowed_users:
                    channel.allowed_users.pop(selected_user.id)
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-UNALLOWED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                else:
                    channel.allowed_users.update({selected_user.id: selected_user})
                    await dropdown.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.private-voices-module.embeds.USER-ALLOWED", placehoders={"{{CHANNEL}}": channel.channel.mention, "{{USER}}": selected_user.mention}), ephemeral=True, delete_after=10)

                channel.update_control_panel()

        except Exception as error:

            SBDS.utils.sendErrorToDebugChannel(file_name=f"{__name__}".replace(".", "/"), error=error, place="listeners.dropdown()")
            SBDS.utils.sendErrorToUser(error=error, ctx=inter, ephemeral=True, edit=False)
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), place="listeners.dropdown()", module=this_module_name)

    @staticmethod # Очищення каналу при написанні + у чат.
    @SBDS.main_bot.listen("on_message")
    async def clear(message: disnake.Message):
        if not message.content == "+": return
        channel = [cog.channels[channel] for channel in cog.channels if cog.channels[channel].channel.id == message.channel.id]
        if not channel: return
        channel = channel[0]
        await channel.clear_channel()

    @staticmethod
    def seconds_to_time(seconds):
        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        seconds = seconds % 60

        if hours > 0:
            return f"{hours:02}:{minutes:02}:{seconds:02}"
        else:
            return f"{minutes:02}:{seconds:02}"

module = f"{__name__}".removeprefix("modules.")
cog = PrivateVoiceCog()


def setup(self: commands.AutoShardedInteractionBot) -> None:

    SBDS.settings.checkKeys(check_keys={"private-voices-module": dict}, path="modules")
    SBDS.settings.checkKeys(check_keys={"create-voice-channel-id": int, "bypass-roles-ids": list, "embeds": dict, "use-backup-channel": bool, "auto-whitelist": bool}, path="modules.private-voices-module")
    SBDS.settings.checkKeys(check_keys={"CONTROL-PANEL": dict, "TIMED-OUT": dict, "NOT-IN-PRIVATE-VOICE-CHANNEL": dict, "NO-PERMISSION": dict, "NOT-INT": dict, "WRONG-BITRATE": dict, "CHANNEL-RENAMED": dict,
                                     "WAIT-BEFORE-RENAME": dict, "CHANNEL-SET-BITRATE": dict, "CHANNEL-SET-LIMIT": dict, "CHANNEL-LOCKED": dict, "CHANNEL-UNLOCKED": dict, "CHANNEL-HIDDEN": dict,
                                     "CHANNEL-SHOWN": dict, "CHANNEL-DELETING": dict, "OWNERSHIP-TAKEN": dict, "YOU-ARE-AN-OWNER": dict, "OWNER-STILL-IN-VOICE": dict, "NO-USERS-IN-CHANNEL": dict,
                                     "GIVEN-OWNERSHIP": dict, "USER-BLOCKED": dict, "USER-UNBLOCKED": dict, "MUST-BE-NOT-BOT": dict, "CANNOT-DO-THIS-MODERATOR": dict, "SELF-USE": dict, "USER-MUTED": dict,
                                     "USER-UNMUTED": dict, "USER-ALLOWED": dict, "USER-UNALLOWED": dict}, path="modules.private-voices-module.embeds")

    SBDS.settings.checkKeys(check_keys={"control-panel-settings": dict}, path="modules.private-voices-module")
    SBDS.settings.checkKeys(check_keys={"dropdowns": dict}, path="modules.private-voices-module.control-panel-settings")
    SBDS.settings.checkKeys(check_keys={"placeholders": dict, "lock-channel": dict, "unlock-channel": dict, "show-channel": dict, "hide-channel": dict, "block-control": dict, "whitelist-control": dict, "mute-control": dict, "give-ownership": dict, "take-ownership": dict, "set-name": dict, "set-limit": dict, "set-bitrate": dict, "delete-channel": dict}, path="modules.private-voices-module.control-panel-settings.dropdowns")

    self.add_cog(cog)