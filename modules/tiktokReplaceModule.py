import asyncio
import os.path
import random
import re
import traceback

import disnake

from main import SurvivalBoomDiscordService as SBDS
from disnake.ext import commands
from tiktok_downloader import ttdownloader

this_module_name = f"{__name__}".removeprefix("modules.")

class TikTokReplaceModule(commands.Cog):

    def __init__(self):
        self.module_settings: SBDS.Settings.SettingsSection = ...
        self.webooks: dict[int, disnake.Webhook] = {}
        self.logger: SBDS.mainlogger.ModuleLogger = ...

    def cog_load(self) -> None:
        self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
        self.module_settings = SBDS.settings.createSection("modules.tiktok-replace-module")

        folder_name: str = self.module_settings.get("tiktok-cache-folder-name")
        if not os.path.exists(folder_name): os.mkdir(folder_name)

        asyncio.create_task(self.check_webhooks(), name=f"{this_module_name} - CheckWebhooks")

    async def check_webhooks(self):

        try:

            await SBDS.main_bot.wait_until_ready()

            for channel in self.module_settings.get("allowed-channels-ids"):

                channel = SBDS.utils.getGuild().get_channel(channel)

                webhook_name = self.module_settings.get("webhook-name")
                webhooks = [webhook for webhook in await channel.webhooks() if webhook.name == webhook_name]
                if webhooks:
                    self.webooks.update({channel.id: webhooks[0]})
                    continue

                self.webooks.update({channel.id: await channel.create_webhook(name=webhook_name)})

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="check_webhooks()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=tracebackk)

    @staticmethod
    async def video_too_large(message: disnake.Message):
        _cog.logger.warn("Неможливо завантажити відео у Discord оскільки воно занадто велике.")
        limit = "25MB"
        glevel = SBDS.utils.getGuild().premium_tier
        if glevel > 1: limit = "50MB"
        elif glevel > 2: limit = "100MB"
        _cog.logger.warn(f"Ліміт для вашого сервера: {limit}. (Щоб збільшити ліміт, забустіть сервер)")
        await message.remove_reaction(emoji="🔄", member=SBDS.utils.getGuild().get_member(SBDS.main_bot.user.id))
        await message.add_reaction(emoji="📄")
        await message.add_reaction(emoji="⚠")


    @staticmethod
    @SBDS.main_bot.listen("on_message")
    async def message(message: disnake.Message):

        try:

            if message.author.bot or not message.channel.id in _cog.module_settings.get("allowed-channels-ids"): return

            pattern = r"https://www\.tiktok\.com/[@a-zA-Z0-9_.]+/video/\d+|https://vm\.tiktok\.com/[A-Za-z0-9]+"

            links = re.findall(pattern, message.content)
            if not links or len(links) > _cog.module_settings.get("max-links-to-process"): return

            _cog.logger.info(f"Користувач &b{message.author.name} &rвідправив TikTok посилання.")

            await message.add_reaction(emoji="🔄")
            await message.edit(suppress_embeds=True)

            files: list[disnake.File] = []
            names: list[str] = []
            for link in links:

                try: video = ttdownloader(str(link))
                except ValueError:
                    await message.remove_reaction(emoji="🔄", member=SBDS.utils.getGuild().get_member(SBDS.main_bot.user.id))
                    await message.add_reaction(emoji="⁉️")
                    _cog.logger.error("Помилка про спробі завантажити tiktok відео.")
                    _cog.logger.error(f"Отримане посилання: {link}")
                    _cog.logger.error("Можливо користувач відправив невірне посилання, або regex вираз неправильно видає дані.")
                    return

                if video[0].get_size() >= 8000000 and SBDS.utils.getGuild().premium_tier < 2:
                    await _cog.video_too_large(message=message)
                    return

                if video[0].get_size() >= 100000000 and SBDS.utils.getGuild().premium_tier < 3:
                    await _cog.video_too_large(message=message)
                    return

                name = _cog.module_settings.get("tiktok-cache-folder-name") + "/video_" + str(message.author.display_name) + "_" + str(random.randint(1, 9999)) + ".mp4"
                video[0].download(name)

                files.append(disnake.File(name))
                names.append(name)

            webhook = _cog.webooks[message.channel.id]

            try: await message.delete()
            except disnake.NotFound: pass

            await webhook.send(files=files, content=re.sub(pattern, "", message.content), username=message.author.display_name, avatar_url=message.author.avatar.url)

            for name in names: os.remove(name)

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="message()")
            SBDS.utils.sendErrorToDebugChannel(file_name=f"{__name__}".replace(".", "/"), error=error, place="message()")



_cog = TikTokReplaceModule()

def setup(bot: commands.InteractionBot):
    SBDS.settings.checkKeys({"allowed-channels-ids": list, "tiktok-cache-folder-name": str, "max-links-to-process": int, "webhook-name": str}, path="modules.tiktok-replace-module")
    bot.add_cog(_cog)