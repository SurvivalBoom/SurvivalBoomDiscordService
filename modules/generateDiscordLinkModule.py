import aiohttp.abc
import asyncio
import traceback

from aiohttp import web
from disnake.ext import commands
from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")

class _InternalWebServer:

    def __init__(self):
        self.server: web.Server = ...
        self.runner: web.ServerRunner = ...
        self.site: web.TCPSite = ...
        self.running = False

    def start(self, host: str, port: int):

        async def startt():
            self.server = web.Server(self.http_handler)
            self.runner = web.ServerRunner(self.server)
            await self.runner.setup()
            self.site = web.TCPSite(self.runner, host, port)
            await self.site.start()

            self.running = True

            while self.running: await asyncio.sleep(1)

        asyncio.create_task(coro=startt(), name=f"{this_module_name} - InternalWebServer")

    async def stop(self):

        await self.site.stop()
        await self.runner.shutdown()
        await self.server.shutdown()

        self.running = False

    @staticmethod
    async def http_handler(request: aiohttp.abc.BaseRequest):

        try:

            if request.path == "/favicon.ico": return

            try: link = await SBDS.utils.getGuild().vanity_invite()
            except: link = await SBDS.main_bot.get_channel(983846821275271248).create_invite(reason="Генерація нового посилання discord.survivalboom.net")

            _cog.logger.info(f"[HTTP] &5{request.method} &d{request.path}&r: Згенеровано Discord посилання: &3{link}&r.")

            return web.HTTPFound(str(link))

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="internalWebServer()")

            return web.Response(status=500, text="{\"error\": \"Виникла помилка у SurvivalBoomDiscordService при обробці вашого запиту\"}")

class _GenerateDiscordLinkModuleCog(commands.Cog):

    def __init__(self):
        self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
        self.server = _InternalWebServer()

    def cog_load(self) -> None:
        try:
            self.server.start(host="0.0.0.0", port=SBDS.settings.get("modules.generate-discord-link-module.web-server-port"))
        except Exception as error:
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="server_web_server()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    def cog_unload(self) -> None:
        self.server.stop()

_cog = _GenerateDiscordLinkModuleCog()
def setup(bot: commands.Bot):

    SBDS.settings.checkKeys(check_keys={"generate-discord-link-module": dict}, path="modules")
    SBDS.settings.checkKeys(check_keys={"web-server-port": int}, path="modules.generate-discord-link-module")

    bot.add_cog(_cog)