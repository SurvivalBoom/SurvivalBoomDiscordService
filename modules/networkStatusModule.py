#    _____                  _             ______
#   / ___/__  ________   __(_)   ______ _/ / __ )____  ____  ____ ___
#   \__ \/ / / / ___/ | / / / | / / __ `/ / __  / __ \/ __ \/ __ `__ \
#  ___/ / /_/ / /   | |/ / /| |/ / /_/ / / /_/ / /_/ / /_/ / / / / / /
# /____/\__,_/_/    |___/_/ |___/\__,_/_/_____/\____/\____/_/ /_/ /_/
# SurvivalBoom Network 2023 | SurvivalBoom Discord Service
#     Network Status Module | By TIMURishche
#
#
import asyncio
import time
import traceback
import aiohttp
import disnake
import mcstatus
from disnake.ext import commands, tasks
from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")
class Service:

    _name = None
    _typee = None
    _host = None

    _last_status = False
    _last_online = 0

    def __init__(self, name: str, typee: str, host: str):

        self._host = host
        self._typee = typee
        self._name = name

    async def getStatus(self):

        if self._typee == "JAVA_SERVER":

            try: data = await (await mcstatus.JavaServer.async_lookup(self._host)).async_status()
            except:
                self._last_status = False
                return False

            self._last_status = True
            self._last_online = data.players.online

            return data.players.online

        elif self._typee == "BEDROCK_SERVER":

            try: data = await (mcstatus.BedrockServer.lookup(self._host)).async_status()
            except:
                self._last_status = False
                return False

            self._last_status = True
            self._last_online = data.players.online

            return data.players.online

        else:

            try:
                data = await self._http_get(self._host)
                if data['status'] == "OK":
                    self._last_status = True
                    return True

                else:
                    self._last_status = False
                    return False

            except:
                self._last_status = False
                return False

    def getType(self): return self._typee

    def getName(self): return self._name

    def getLastOnline(self) -> int:
        if self._last_status and self._typee != "SERVICE":
            return self._last_online
        else:
            return 0

    @staticmethod
    async def _http_get(url: str):

        async with aiohttp.ClientSession() as session:

            async with session.get(url) as resp:

                return await resp.json()

class NetworkStatusCog(commands.Cog):

    def __init__(self):

        self.status_channel = None
        self.status_channel_message = None
        self.placeholders = {}
        self.services: list = []
        self.module_settings: SBDS.settings.SettingsSection = None
        self.logger: SBDS.mainlogger.ModuleLogger = ...
        self.ovh_support = False

    def cog_load(self) -> None:

        try:

            self.module_settings = SBDS.settings.createSection(path="modules.network-status-module")
            self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)

            self.status_message_task.change_interval(seconds=self.module_settings.get('message-update-interval'))
            self.update_placeholders.change_interval(seconds=self.module_settings.get('status-update-interval'))
            self.update_placeholders.start()

            for service in self.module_settings.get('services'): self.services.append(Service(name=service['name'], typee=service['type'], host=service['host']))

            try:
                from modules import ovhDDoSAlertsModule
                self.ovh_support = True
                async def aaaa():
                    await SBDS.main_bot.wait_until_ready()
                    await asyncio.sleep(2)
                    self.logger.info("Підтримку &bOVH DDoS Alerts &rувімкнено.")
                asyncio.create_task(aaaa())

            except ImportError: pass

        except Exception as error: SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    def cog_unload(self) -> None:

        self.status_message_task.stop()
        self.update_placeholders.stop()

    @tasks.loop(hours=999)
    async def update_placeholders(self):

        try:

            if not SBDS.main_bot.is_ready(): return

            for service in self.services:

                async def services(servicee: Service):

                    #
                    # Плейсхолдер STATUS
                    #
                    placeholder_name = "{{" + servicee.getName() + "$STATUS" + "}}"
                    value = await servicee.getStatus()
                    value_str = str(value)

                    if value_str == "False": value_str = self.module_settings.get("placeholders-settings.status-placeholder-result.offline")
                    else: value_str = self.module_settings.get("placeholders-settings.status-placeholder-result.online")

                    self.placeholders.update({placeholder_name: value_str})

                    #
                    # Плейсхолдер ONLINE
                    #

                    if servicee.getType() == "SERVICE": return

                    if value is False: value = self.module_settings.get("placeholders-settings.status-placeholder-result.no-players")

                    placeholder_name = "{{" + servicee.getName() + "$ONLINE" + "}}"

                    self.placeholders.update({placeholder_name: value})

                asyncio.create_task(services(service), name=f"{this_module_name} - GetServiceStatus(): {service.getName()}")

            await asyncio.sleep(5)

            if not self.status_message_task.is_running(): self.status_message_task.start()

        except Exception as error:

            file_name = f"{__name__}".replace(".", "/")
            file_name2 = f"{__name__}".removeprefix("modules.")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=file_name2, place="update_placeholders()")
            SBDS.utils.sendErrorToDebugChannel(error=error, place="update_placeholders()", file_name=file_name)

            SBDS.modules.unloadModule(name=f"{__name__}".removeprefix("modules."), crashed=True, error=error, tracebackk=traceback.format_exc())

    @tasks.loop(hours=999)
    async def status_message_task(self):

        try:

            if not SBDS.main_bot.is_ready(): return

            if self.status_channel is None:

                self.status_channel = SBDS.main_bot.get_channel(int(self.module_settings.get("status-channel-id")))

                await self.status_channel.purge()

            if self.status_channel_message is None: self.status_channel_message = await self.status_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.network-status-module.embeds.LOADING"))

            buttons = SBDS.utils.buttonsBuilder(path_to_embed="modules.network-status-module.embeds.MAIN.buttons")

            global_online = 0
            for service in self.services:
                if service.getType() == "SERVICE": continue
                if not service.getName() in self.module_settings.get("placeholders-settings.how-to-count-global-online"): continue

                global_online = global_online + service.getLastOnline()

            self.placeholders.update({"{{GLOBAL$ONLINE}}": global_online})
            self.placeholders.update({"{{LAST_UPDATE}}": f"<t:{int(time.time())}:R>"})

            embed = SBDS.utils.buildEmbed(path_to_embed="modules.network-status-module.embeds.MAIN", placehoders=self.placeholders)
            if self.ovh_support:
                from modules import ovhDDoSAlertsModule
                servers = ovhDDoSAlertsModule.getServersUnderDDoS()
                if len(servers) > 0: embed.add_field(name="💥 Під DDoS атакою: `" + ", ".join(servers) + "`", value="", inline=False)

            await self.status_channel_message.edit(embed=embed, view=buttons)


        except Exception as error:

            file_name = f"{__name__}".replace(".", "/")
            file_name2 = f"{__name__}".removeprefix("modules.")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=file_name2, place="status_message_task()")
            SBDS.utils.sendErrorToDebugChannel(error=error, place="status_message_task()", file_name=file_name)

            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    @staticmethod
    @SBDS.main_bot.listen("on_button_click")
    async def button_clicked(interaction: disnake.MessageInteraction):

        try:

            button_name = interaction.data['custom_id']

            for button in cog.module_settings.get('embeds.MAIN.buttons'):

                if not button['name'] == button_name: continue

                await interaction.send(embed=SBDS.utils.buildEmbed(path_to_embed=f"modules.network-status-module.embeds.{button['send-embed']}", placehoders=cog.placeholders), ephemeral=True, delete_after=30)

                return

        except Exception as error:

            file_name = f"{__name__}".replace(".", "/")
            file_name2 = f"{__name__}".removeprefix("modules.")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=file_name2, place="button_clicked()")
            SBDS.utils.sendErrorToDebugChannel(error=error, place="button_clicked()", file_name=file_name)

            SBDS.modules.unloadModule(name=f"{__name__}".removeprefix("modules."), crashed=True, error=error, tracebackk=traceback.format_exc())


# noinspection PyTypeChecker
cog: NetworkStatusCog = NetworkStatusCog()



def setup(self: commands.AutoShardedInteractionBot) -> None:

    SBDS.settings.checkKeys(check_keys={"network-status-module": dict}, path="modules")

    SBDS.settings.checkKeys(check_keys={"services": list, "embeds": dict, "placeholders-settings": dict, "status-channel-id": int, "message-update-interval": int, "status-update-interval": int}, path="modules.network-status-module")

    SBDS.settings.checkKeys(check_keys={"MAIN": dict, "LOADING": dict}, path="modules.network-status-module.embeds")

    SBDS.settings.checkKeys(check_keys={"status-placeholder-result": dict, "how-to-count-global-online": list}, path="modules.network-status-module.placeholders-settings")
    SBDS.settings.checkKeys(check_keys={"online": str, "offline": str, "no-players": str}, path="modules.network-status-module.placeholders-settings.status-placeholder-result")

    self.add_cog(cog)