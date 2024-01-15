import asyncio
import traceback

import ovh
import disnake
from main import SurvivalBoomDiscordService as SBDS
from disnake.ext import commands, tasks


this_module_name = f"{__name__}".removeprefix("modules.")
class OvhDDoSAlertsModuleCog(commands.Cog):

    def __init__(self):
        self.ovh: ovh.Client = ...
        self.module_settings = SBDS.settings.get("modules.ovh-ddos-alerts-module")
        self.servers_under_ddos: dict[str, bool] = {}
        self.servers: dict[str, str] = {}
        self.admins: list[disnake.Member] = []
        self.logger: SBDS.mainlogger.ModuleLogger = ...

    def cog_load(self) -> None:

        try:

            self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)

            ovh_credentials = SBDS.settings.createSection("modules.ovh-ddos-alerts-module.ovh-api-credentials")
            self.ovh = ovh.Client(
                endpoint=ovh_credentials.get("endpoint"),
                application_key=ovh_credentials.get("application-key"),
                application_secret=ovh_credentials.get("application-secret"),
                consumer_key=ovh_credentials.get("consumer-key")
            )

            servers: list[dict[str, any]] = self.module_settings.get("servers")

            for server in servers:
                SBDS.settings.checkKeys({"name": str, "address": str}, custom_data=server)
                self.servers.update({server['name']: server['address']})

            self.check_servers.change_interval(seconds=self.module_settings.get("check-interval"))
            self.check_servers.start()

        except Exception as error:
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="cog_load()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    def cog_unload(self) -> None:
        self.check_servers.stop()

    @tasks.loop(hours=99999999)
    async def check_servers(self):

        try:

            await SBDS.main_bot.wait_until_ready()

            if len(self.admins) < 1:
                for idd in self.module_settings.get("ddos-alert-roles-ids"):

                    role = SBDS.utils.getGuild().get_role(idd)
                    if role is None: continue

                    for member in role.members: self.admins.append(member)

            servers = self.servers.copy()

            for server in servers:

                if len(self.ovh.get(f"/ip/{servers[server]}/mitigation")) == 0:
                    if not server in self.servers_under_ddos: continue
                    self.servers_under_ddos.pop(server)

                    self.logger.info(f"&aDDoS атака завершена на сервер &3{server} &r(&b{servers[server]}&r).")
                    self.alert_send(SBDS.utils.buildEmbed(path_to_embed="modules.ovh-ddos-alerts-module.embeds.DDOS_END", placehoders={"{{SERVER}}": server, "{{ADDRESS}}": servers[server]}))

                    continue

                if server in self.servers_under_ddos and self.servers_under_ddos[server] is True: continue
                self.servers_under_ddos.update({server: False})

                self.logger.info(f"&cУвага! Виявлено DDoS атаку на сервер &3{server} &r(&3{servers[server]}&r).")
                self.alert_send(SBDS.utils.buildEmbed(path_to_embed="modules.ovh-ddos-alerts-module.embeds.DDOS_DETECTED", placehoders={"{{SERVER}}": server, "{{ADDRESS}}": servers[server]}))

                self.servers_under_ddos.update({server: True})

        except ovh.exceptions.APIError:
            await asyncio.sleep(5)

        except Exception as error:
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="tasks.check_servers()")
            SBDS.utils.sendErrorToDebugChannel(file_name=this_module_name, error=error, place="tasks.check_servers()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    def alert_send(self, embed: disnake.Embed):
        for member in self.admins: asyncio.create_task(member.send(embed=embed), name=f"{this_module_name} - sendAlert()")

def getServersUnderDDoS() -> dict[str, str]:

    var = {}

    servers = _cog.servers.copy()

    for server in _cog.servers.copy():
        if server in _cog.servers_under_ddos:
            var.update({server: servers[server]})

    return var


_cog = OvhDDoSAlertsModuleCog()

def setup(self):

    SBDS.settings.checkKeys(check_keys={"ovh-ddos-alerts-module": dict}, path="modules")
    SBDS.settings.checkKeys(check_keys={"ovh-api-credentials": dict, "check-interval": int, "ddos-alert-roles-ids": list, "embeds": dict, "servers": list}, path="modules.ovh-ddos-alerts-module")
    SBDS.settings.checkKeys(check_keys={"endpoint": str, "application-key": str, "application-secret": str, "consumer-key": str}, path="modules.ovh-ddos-alerts-module.ovh-api-credentials")
    SBDS.settings.checkKeys(check_keys={"DDOS_DETECTED": dict, "DDOS_END": dict}, path="modules.ovh-ddos-alerts-module.embeds")

    SBDS.main_bot.add_cog(_cog)