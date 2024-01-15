import asyncio
import json
import random
import time
import traceback

import aiohttp.abc
import disnake
import asyncrcon
import mysql.connector

from main import SurvivalBoomDiscordService as SBDS
from aiohttp import web
from disnake.ext import commands, tasks


this_module_name = f"{__name__}".removeprefix("modules.")

class _LuckPermsDatabase:

    def __init__(self, host: str, port: str, user: str, password: str, database: str, users_table: str, permissions_table: str) -> None:

        self._names_dict = {}
        self._database = mysql.connector.connect(host=host, port=port, user=user, passwd=password, database=database)
        self._lp_permissions_table_name = permissions_table
        self._lp_users_table_name = users_table
        self._database_guard_task = asyncio.create_task(self._database_guard(), name=f"{this_module_name} - LuckPermsDatabaseGuard")

    def close(self):

        self._database_guard_task.cancel()
        self._database.close()

    async def _database_guard(self):
        while True:
            await asyncio.sleep(5)
            try:

                # Перепідключення до бази даних якщо підключення від'єднано.
                try:
                    cursor = self._database.cursor(buffered=True)
                    cursor.close()
                except mysql.connector.errors.OperationalError:
                    self._database.reconnect(attempts=5, delay=1)
                    _cog.logger.warn(f"Перепідключено до бази даних &aLuckPerms&e.")

            except Exception as error:
                tracebackkkk = traceback.format_exc()
                SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackkkk, place="LuckPermsDatabase.database_guard()", module=this_module_name)
                SBDS.modules.unloadModule(name=this_module_name, error=error, tracebackk=tracebackkkk)

    def create_dict(self):

        cursor = self._database.cursor(buffered=True)
        cursor.execute(f"SELECT * FROM {self._lp_users_table_name}")

        raw_data = cursor.fetchall()
        out = {}

        for d in raw_data: out.update({d[0]: d[1]})

        self._names_dict = out

    def getUsersWithGroup(self, group: str) -> dict[str, str]:

        cursor = self._database.cursor(buffered=True)
        cursor.execute(f"SELECT * FROM luckperms_user_permissions WHERE permission LIKE '%group.{group}%'")
        cursor.close()

        result = cursor.fetchall()
        out = {}

        for d in result:

            username = self._names_dict[d[1]]
            out.update({username: d[2]})

        return out
class _LinkedUser:

    def __init__(self, userid: int, linked_username: str):
        self.linked_user = linked_username
        self.user = SBDS.utils.getGuild().get_member(int(userid))
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

            try: r_json = await request.json()
            except: r_json = "ERROR"
            _cog.logger.info(f"[HTTP] &5{request.method} &d{request.path}&r: &b{r_json}")

            if request.path == "/add-link-code":

                if not request.method == "POST": return web.Response(status=400, text="{\"error\": \"HTTP запит повинен бути POST\"}")

                if r_json == "ERROR": return web.Response(status=400, text="{\"error\": \"Невірний JSON\"}")

                try:
                    # noinspection PyTypeChecker
                    code = r_json["code"]
                    # noinspection PyTypeChecker
                    username = r_json["username"]
                except KeyError as error: return web.Response(status=400, text="{\"error\": \"Ключ {{key}} не знайдено\"}".replace("{{key}}", str(error)))

                linked_user = [user.linked_user for user in _cog.linked_users if user.linked_user == username]
                if linked_user:
                    return web.Response(status=403, text="{\"error\": \"Гравець вже прив'язаний\"}")

                try: _cog.link_codes.update({int(code): username})
                except ValueError: return web.Response(status=403, text="{\"error\": \"Код повинен бути числом\"}")

                return web.Response(status=200, text="OK")

            if request.path == "/get-user-info":

                if not request.method == "POST": return web.Response(status=400, text="{\"error\": \"HTTP запит повинен бути POST\"}")

                if r_json == "ERROR": return web.Response(status=400, text="{\"error\": \"Невірний JSON\"}")

                try:
                    # noinspection PyTypeChecker
                    search = r_json["query"]
                except KeyError as error: return web.Response(status=400, text="{\"error\": \"Ключ {{key}} не знайдено\"}".replace("{{key}}", str(error)))

                try: user = LinkingAPI.searchUser(search)
                except LinkingAPI.NoResultsFound: return web.Response(status=403, text="{\"error\": \"Гравця за запитом {{user}} не знайдено\"}".replace("{{user}}", search))

                text = "{\"nickname\": \"{{NICKNAME}}\", \"user-id\": \"{{USER-ID}}\", \"user-name\": \"{{USER-NAME}}\", \"user-display-name\": \"{{USER-DISPLAY-NAME}}\"}"
                return web.Response(status=200, text=text.replace("{{NICKNAME}}", user.linked_user).replace("{{USER-ID}}", str(user.user.id)).replace("{{USER-NAME}}", user.user.name).replace("{{USER-DISPLAY-NAME}}", user.user.display_name))

            if request.path == "/link-user":

                if not request.method == "POST": return web.Response(status=400, text="{\"error\": \"HTTP запит повинен бути POST\"}")

                if r_json == "ERROR": return web.Response(status=400, text="{\"error\": \"Невірний JSON\"}")

                try:
                    # noinspection PyTypeChecker
                    user_id = r_json["user-id"]
                    # noinspection PyTypeChecker
                    username = r_json["username"]
                except KeyError as error: return web.Response(status=400, text="{\"error\": \"Ключ {{key}} не знайдено\"}".replace("{{key}}", str(error)))

                try: LinkingAPI.linkUser(nickname=username, user_id=user_id)
                except LinkingAPI.UserAlreadyLinked: return web.Response(status=403, text="{\"error\": \"Гравець вже прив'язаний\"}")

                return web.Response(status=200, text="OK")

            if request.path == "/unlink-user":

                if not request.method == "POST": return web.Response(status=400, text="{\"error\": \"HTTP запит повинен бути POST\"}")

                if r_json == "ERROR": return web.Response(status=400, text="{\"error\": \"Невірний JSON\"}")

                try:
                    # noinspection PyTypeChecker
                    query = r_json["query"]
                except KeyError as error: return web.Response(status=400, text="{\"error\": \"Ключ {{key}} не знайдено\"}".replace("{{key}}", str(error)))

                try: LinkingAPI.unlinkUser(query=query)
                except LinkingAPI.NoResultsFound: return web.Response(status=403, text="{\"error\": \"Гравця за запитом {{user}} не знайдено\"}".replace("{{user}}", query))

                return web.Response(status=200, text="OK")

            return web.Response(text="Not Found", status=404)

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="internalWebServer()")

            return web.Response(status=500, text="{\"error\": \"Виникла помилка у SurvivalBoomDiscordService при обробці вашого запиту\"}")
class _LinkingModuleCog(commands.Cog):

    def __init__(self):
        self.webserver: _InternalWebServer = ...
        self.link_codes: dict[int, str] = {}
        self.linked_users: list[_LinkedUser] = []
        self.logger: SBDS.mainlogger.ModuleLogger = ...
        self.module_settings: SBDS.settings.SettingsSection = ...
        self.luckperms: _LuckPermsDatabase = ...

        self.booster_group_name: str = ...
        self.linked_user_role: disnake.Role = ...
        self.donate_roles: dict[str, disnake.Role] = {}

        self.console_module_support = False

        self.rcon_settings = {}

    def cog_load(self) -> None:
        try:
            self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
            self.module_settings = SBDS.settings.createSection("modules.linking-module")

            self.webserver = _InternalWebServer()
            self.webserver.start(host="0.0.0.0", port=self.module_settings.get("linking-web-server-port"))

            self.linked_users_update_list_task.start()

            self.booster_group_name = self.module_settings.get("sync-booster.booster-group-name")

            self.luckperms = _LuckPermsDatabase(host=self.module_settings.get("luckperms-database.host"), port=self.module_settings.get("luckperms-database.port"), database=self.module_settings.get("luckperms-database.database"), user=self.module_settings.get("luckperms-database.user"), password=self.module_settings.get("luckperms-database.password"), users_table=self.module_settings.get("luckperms-database.lp-users-table-name"), permissions_table=self.module_settings.get("luckperms-database.lp-permissions-table-name"))
            self.luckperms.create_dict()

            self.create_lp_names_dict.start()
            self.create_lp_names_dict.get_task().set_name(f"{this_module_name} - create_lp_names_dict()")

            task_settings = self.module_settings.get("sync-nickname")
            if task_settings['enabled'] is True:
                self.set_nickname_task.change_interval(seconds=int(task_settings['sync-task-interval']))
                self.set_nickname_task.start()
                self.set_nickname_task.get_task().set_name(f"{this_module_name} - set_nickname_task()")

            task_settings = self.module_settings.get("sync-booster")
            if task_settings['enabled'] is True:
                self.rcon_settings.update({"host": task_settings['rcon-connection']['host'], "port": task_settings['rcon-connection']['port'], "password": task_settings['rcon-connection']['password'], "lpgive_cmd": task_settings['give-booster-group-command'], "lprevoke_cmd": task_settings['revoke-booster-group-command']})
                self.booster_sync_task.change_interval(seconds=int(task_settings['sync-task-interval']))
                self.booster_sync_task.start()
                self.booster_sync_task.get_task().set_name(f"{this_module_name} - booster_sync_task()")

            task_settings = self.module_settings.get("sync-linked-role")
            if task_settings['enabled'] is True:
                self.linked_role_sync_task.change_interval(seconds=int(task_settings['sync-task-interval']))
                self.linked_role_sync_task.start()
                self.linked_role_sync_task.get_task().set_name(f"{this_module_name} - linked_role_sync()")

            task_settings = self.module_settings.get("sync-groups-to-roles")
            if task_settings['enabled'] is True:
                self.groups_sync_task.change_interval(seconds=int(task_settings['sync-task-interval']))
                self.groups_sync_task.start()
                self.groups_sync_task.get_task().set_name(f"{this_module_name} - groups_sync_task()")

            if not SBDS.main_bot.is_ready(): return

            try:
                from modules import consoleModule
                consoleModule.registerCommand(name="linking", command_executor=_cog.linking_console_command, description="linkingModule: Інструмент для роботи із прив'язками гравців", arguments="[codes/linklist/newcode/delcode/link/unlink/search]")
                _cog.console_module_support = True
            except Exception as error:
                _cog.logger.warn(f"Імпорт &dconsoleModule &rне вдався із помилкою: &c{error}&r.")
                _cog.logger.warn("Схоже що модуль &dconsoleModule &eне встановлено. Ігнорування реєстрації команди &blinking&r.")
                return




        except Exception as error:
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

    def cog_unload(self) -> None:
        asyncio.create_task(self.webserver.stop(), name=f"{this_module_name} - StopInternalServer")
        self.linked_users_update_list_task.stop()
        self.set_nickname_task.stop()
        self.booster_sync_task.stop()
        self.create_lp_names_dict.stop()
        self.linked_role_sync_task.stop()
        self.groups_sync_task.stop()
        self.luckperms.close()

        if self.console_module_support is True:
            from modules import consoleModule
            consoleModule.unregisterCommand(name="linking")

    @commands.slash_command(name="link", description="Прив'язує ваш дискорд акаунт до майнкрафт акаунта.", options=[disnake.Option(name="code", description="Код прив'язку з майнкрафт сервера.", type=3, required=True)])
    async def link_command(self, ctx: disnake.ApplicationCommandInteraction, code: str):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.LOADING"))

            try:
                data = SBDS.database.getUserData(user_id=ctx.user.id, key="minecraft-link")
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.DISCORD-ALREADY-LINKED", placehoders={"{{MINECRAFT_USERNAME}}": json.loads(data)['username']}))
                return
            except SBDS.database.KeyNotFound: pass

            try: code = int(code)
            except:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.INVALID-CODE"))
                return

            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.SEARCHING-IN-DATABASE", placehoders={"{{CODE}}": code}))
            await asyncio.sleep(1)

            if not code in self.link_codes:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.CODE-NOT-FOUND", placehoders={"{{CODE}}": code}))
                return

            username = self.link_codes[code]
            linked_users_with_this_username = [user for user in self.linked_users if user.linked_user == username]
            if linked_users_with_this_username:
                linked_user_with_this_username = linked_users_with_this_username[0]
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.MINECRAFT-ALREADY-LINKED", placehoders={"{{MINECRAFT_USERNAME}}": linked_user_with_this_username.linked_user, "{{DISCORD_USERNAME}}": linked_user_with_this_username.user.mention}))
                self.link_codes.pop(code)
                return

            SBDS.database.addUserKey(user_id=ctx.user.id, key="minecraft-link")
            SBDS.database.setUserKey(user_id=ctx.user.id, key="minecraft-link", value=json.dumps({"username": username, "linked-time": int(time.time())}))

            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.SUCCESSFULY-LINKED", placehoders={"{{MINECRAFT_USERNAME}}": username, "{{DISCORD_USERNAME}}": ctx.user.mention}))
            await ctx.user.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.SUCCESSFULY-LINKED-DM"))

            self.linked_users_update_list()
            self.link_codes.pop(code)

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(module=this_module_name, tracebackk=tracebackk, place="commands.link_command()", error=error)
            SBDS.utils.sendErrorToUser(error=error, ctx=ctx, edit=True)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="commands.link_command()")

    @commands.slash_command(name="unlink", description="Відв'язує ваш дискорд акаунт від майнкрафт акаунта.")
    async def unlink_command(self, ctx: disnake.ApplicationCommandInteraction):

        try:

            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.LOADING"))

            try:
                link_data = SBDS.database.getUserData(user_id=ctx.user.id, path="minecraft-link")
                link_data = json.loads(link_data)
            except SBDS.database.KeyNotFound:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.NOT-LINKED"))
                return

            SBDS.database.delUserKey(user_id=ctx.user.id, key="minecraft-link")

            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.SUCCESSFULY-UNLINKED", placehoders={"{{MINECRAFT_USERNAME}}": link_data['username'], "{{DISCORD_USERNAME}}": ctx.user.mention}))

            self.linked_users_update_list()

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(module=this_module_name, tracebackk=tracebackk, place="commands.unlink_command()", error=error)
            SBDS.utils.sendErrorToUser(error=error, ctx=ctx, edit=True)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="commands.unlink_command()")

    def linked_users_update_list(self):

        linked_users = []

        users = SBDS.database.executeSqlWithResult(f"SELECT * FROM {SBDS.database.userdata_table_name} WHERE Data LIKE '%minecraft-link%'")
        for user in users:
            linked_users.append(_LinkedUser(userid=user[0], linked_username=json.loads(json.loads(user[1])["minecraft-link"])['username']))

        self.linked_users = linked_users
    @tasks.loop(seconds=10)
    async def linked_users_update_list_task(self):

        await SBDS.main_bot.wait_until_ready()

        try: self.linked_users_update_list()
        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(module=this_module_name, tracebackk=tracebackk, place="tasks.linked_users_update_list_task()", error=error)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.linked_users_update_list_task()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, tracebackk=tracebackk, error=error)

    @tasks.loop(hours=99999)
    async def set_nickname_task(self):

        await SBDS.main_bot.wait_until_ready()

        try:

            for user in self.linked_users:

                if user.linked_user.lower() in user.user.display_name.lower(): continue

                old_nickname = user.user.display_name
                nickname = self.module_settings.get('sync-nickname.nickname-format').replace("{{DISCORD_NICKNAME}}", user.user.display_name).replace("{{MINECRAFT_NICKNAME}}", user.linked_user)

                if not len(nickname) < 30: nickname = self.module_settings.get('sync-nickname.nickname-format').replace("{{DISCORD_NICKNAME}}", f"{user.user.display_name[:26 - len(nickname)]}...").replace("{{MINECRAFT_NICKNAME}}", user.linked_user)

                try: await user.user.edit(nick=nickname)
                except disnake.Forbidden: continue

                self.logger.info(f"Змінено нікнейм учасника &3{user.user.name} &rз &5{old_nickname} &rна &d{nickname}&r.")

                await user.user.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.linking-module.embeds.NICKNAME-CHANGED", placehoders={"{{NICKNAME}}": nickname, "{{OLD_NICKNAME}}": old_nickname}))

        except Exception as error:

            self.set_nickname_task.stop()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="tasks.set_nickname()")
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.set_nickname()")

    @tasks.loop(hours=99999)
    async def booster_sync_task(self):

        await SBDS.main_bot.wait_until_ready()

        try:

            minecraft_boosters = self.luckperms.getUsersWithGroup(self.booster_group_name)

            for booster in SBDS.utils.getGuild().premium_subscriber_role.members:

                linked_booster = [member for member in self.linked_users if member.user.id == booster.id]
                if not linked_booster: continue
                linked_booster = linked_booster[0]

                if linked_booster.linked_user.lower() in minecraft_boosters: continue

                rcon = asyncrcon.AsyncRCON(self.rcon_settings['host'] + ":" + str(self.rcon_settings['port']), self.rcon_settings['password'])

                await rcon.open_connection()
                for command in self.rcon_settings['lpgive_cmd']: await rcon.command(command.replace("{PLAYER}", linked_booster.linked_user))

                rcon.close()

                _cog.logger.info(f"Видано привілегію &dбустер &rна майнкрафт сервері бустеру &b{booster.name}&r.")

            for booster in minecraft_boosters:

                linked_booster = [member for member in self.linked_users if member.linked_user.lower() == booster]
                a = 0
                if not linked_booster: a = 1

                if a != 1 and not SBDS.utils.getGuild().premium_subscriber_role in linked_booster[0].user.roles: a = 1

                if a == 0: continue

                rcon = asyncrcon.AsyncRCON(self.rcon_settings['host'] + ":" + str(self.rcon_settings['port']), self.rcon_settings['password'])

                await rcon.open_connection()
                for abobus in linked_booster:
                    for command in self.rcon_settings['lprevoke_cmd']: await rcon.command(command.replace("{PLAYER}", abobus.linked_user))

                rcon.close()

                _cog.logger.info(f"Знято привілегію &dбустер &rна майнкрафт сервері у &b{booster}&r.")

        except Exception as error:

            self.booster_sync_task.stop()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="tasks.booster_sync_task()")
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.booster_sync_task()")

    @tasks.loop(hours=99999)
    async def linked_role_sync_task(self):

        await SBDS.main_bot.wait_until_ready()

        try:

            if self.linked_user_role is ...: self.linked_user_role = SBDS.utils.getGuild().get_role(int(self.module_settings.get("sync-linked-role.linked-role-id")))

            for user in self.linked_users:
                if self.linked_user_role in user.user.roles: continue
                await user.user.add_roles(self.linked_user_role, reason="SBDS - LinkingModule")
                _cog.logger.info(f"Видано роль прив'язаного учасника користувачу &b{user.user.name}&r.")

            for user in self.linked_user_role.members:
                linked_user = [member.user for member in self.linked_users if member.user.id == user.id]
                if linked_user: continue
                await user.remove_roles(self.linked_user_role, reason="SBDS - LinkingModule")
                _cog.logger.info(f"Знято роль прив'язаного учасника у користувача &b{user.name}&r.")

        except Exception as error:

            self.linked_role_sync_task.stop()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="tasks.linked_role_sync_task()")
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.linked_role_sync_task()")

    @tasks.loop(hours=99999)
    async def groups_sync_task(self):

        await SBDS.main_bot.wait_until_ready()

        try:

            if not self.donate_roles:
                for group in self.module_settings.get("sync-groups-to-roles.groups-list"): self.donate_roles.update({group["group-name"]: SBDS.utils.getGuild().get_role(int(group["role-id"]))})

            for group in self.donate_roles:

                role = self.donate_roles[group]

                players_donate = self.luckperms.getUsersWithGroup(group)
                for player in players_donate:

                    linked_player = [user for user in self.linked_users if user.linked_user.lower() == player]
                    if not linked_player: continue

                    linked_player = linked_player[0]

                    if role in linked_player.user.roles: continue

                    await linked_player.user.add_roles(role, reason="SBDS - LinkingModule")

                    _cog.logger.info(f"Видано донат роль &d{group} &rкористувачу &b{linked_player.user.name}&r.")

                for member in self.donate_roles[group].members:

                    linked_player = [user for user in self.linked_users if user.user.id == member.id]

                    a = 0
                    if not linked_player: a = 1

                    if a != 1 and not linked_player[0].linked_user.lower() in players_donate: a = 1

                    if a == 0: continue

                    await member.remove_roles(role, reason="SBDS - LinkingModule")

                    _cog.logger.info(f"Знято донат роль &d{group} &rз користувача &b{member.name}&r.")

        except Exception as error:

            self.groups_sync_task.stop()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="tasks.groups_sync_task()")
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.groups_sync_task()")

    @tasks.loop(seconds=10)
    async def create_lp_names_dict(self):
        try: self.luckperms.create_dict()
        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(module=this_module_name, tracebackk=tracebackk, place="tasks.create_lp_names_dict()", error=error)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="tasks.create_lp_names_dict()")
            SBDS.modules.unloadModule(name=this_module_name, crashed=True, tracebackk=tracebackk, error=error)

    @staticmethod
    def linking_console_command(args: list[str]):
        if len(args) < 2:
            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("---  Допомога по команді linking  ---")
            SBDS.mainlogger.info(f"&elinking codes &r- &3Відображає список усіх активних кодів для прив'язки.")
            SBDS.mainlogger.info(f"&elinking linklist &r- &3Відображає список усіх прив'язаних учасників.")
            SBDS.mainlogger.info(f"&elinking newcode [Нікнейм] [Код] &r- &3Створити новий код для вказаного нікнейма. Якщо параметр КОД не вказаний, код буде згенерований автоматично.")
            SBDS.mainlogger.info(f"&elinking delcode [Код] &r- &3Видаляє вказаний код.")
            SBDS.mainlogger.info(f"&elinking link [Дискорд ID] [Нікнейм] &r- &3Примусово прив'язує користувача до майнкрафт нікнейма.")
            SBDS.mainlogger.info(f"&elinking unlink [Дискорд ID] &r- &3Примусово відв'язує користувача від майнкрафт прив'язки.")
            SBDS.mainlogger.info(f"&elinking search [Дискорд ID/Нікнейм] &r- &3Знайти прив'язку по ніку або ID користувача.")
            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")
            return

        if args[1] == "codes":

            if len(_cog.link_codes) == 0:
                SBDS.mainlogger.info("Схоже, ніяких кодів прив'язки поки що не було згенеровано.")
                return

            SBDS.mainlogger.info("---   Згенеровані коди прив'язки  ---")

            for code in _cog.link_codes:
                SBDS.mainlogger.info(f"&3{code} -- &a{_cog.link_codes[code]}")

            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")
            return

        if args[1] == "newcode":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: &elinking newcode <Нікнейм> [Код]")
                SBDS.mainlogger.info("Якщо параметр КОД не вказаний, код буде згенерований автоматично.")
                return
            if len(args) < 4: code = random.randint(1000, 9999)
            else: code = args[3]

            try: code = int(code)
            except:
                SBDS.mainlogger.info("Код повинен бути числом, а не хуйнею яку ви написали.")
                return

            _cog.link_codes.update({code: args[2]})

            SBDS.mainlogger.info(f"Код &5{code} &rуспішно створено! Напиіть &3linking codes &rщоб отримати список усіх кодів.")
            return

        if args[1] == "delcode":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: &elinking delcode [Код]")
                return

            try: code = int(args[2])
            except ValueError:
                SBDS.mainlogger.info("Код повинен бути числом, а не хуйнею яку ви написали.")
                return

            if not code in _cog.link_codes:
                SBDS.mainlogger.info(f"Код &d{code} &rне знайдено.")
                return

            _cog.link_codes.pop(code)

            SBDS.mainlogger.info(f"Код &5{code} &rуспішно видалено.")
            return

        if args[1] == "link":

            if len(args) < 4:
                SBDS.mainlogger.info("Використання: &elinking link [Дискорд ID] [Нікнейм]")
                return

            try: user_id = int(args[2])
            except ValueError:
                SBDS.mainlogger.info("Дискорд ID повинен бути числом, а не хуйнею яку ви написали.")
                return

            try: SBDS.database.delUserKey(user_id=user_id, key="minecraft-link")
            except SBDS.database.KeyNotFound: pass
            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача за ID &5{user_id} &rне знайдено.")
                return

            username = args[3]
            user_obj = SBDS.main_bot.get_user(user_id)

            SBDS.database.addUserKey(user_id=user_id, key="minecraft-link")
            SBDS.database.setUserKey(user_id=user_id, key="minecraft-link", value=json.dumps({"username": username, "linked-time": int(time.time())}))

            SBDS.mainlogger.info(f"Успішно прив'язано користувача &5{user_obj.name} &rдо майнкрафт акаунта &3{username}&r.")

            return

        if args[1] == "unlink":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: &elinking unlink [Дискорд ID]")

            try: user_id = int(args[2])
            except ValueError:
                SBDS.mainlogger.info("Дискорд ID повинен бути числом, а не хуйнею яку ви написали.")
                return

            try:
                SBDS.database.delUserKey(user_id=user_id, key="minecraft-link")
            except SBDS.database.KeyNotFound:
                SBDS.mainlogger.info(f"Користувач &5{SBDS.main_bot.get_user(user_id).name} &rне прив'язаний.")
                return
            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача за ID &5{user_id} &rне знайдено.")
                return

            SBDS.mainlogger.info(f"Користувача {SBDS.main_bot.get_user(user_id).name} успішно відв'язано від майнкрафт акаунта.")
            return

        if args[1] == "search":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: &elinking search [Дискорд ID/Нікнейм]")
                return

            try:
                user_id = int(args[2])
                results = [user for user in _cog.linked_users if user.user.id == user_id]
                if not results:
                    SBDS.mainlogger.info(f"Інформацію за ID &5{user_id} &rне знайдено.")
                    return
                result = results[0]

                SBDS.mainlogger.info("")
                SBDS.mainlogger.info("---       Результати пошуку       ---")
                SBDS.mainlogger.info(f"Дискорд: &5{result.user.name}&r(&d{result.user.display_name}&r)")
                SBDS.mainlogger.info(f"Майнкрафт: &3{result.linked_user}&r.")
                SBDS.mainlogger.info("---                               ---")
                SBDS.mainlogger.info("")

            except ValueError:

                results = [user for user in _cog.linked_users if user.linked_user == args[2]]
                if not results:
                    SBDS.mainlogger.info(f"Інформацію за нікнеймом &5{args[2]} &rне знайдено.")
                    return
                result = results[0]

                SBDS.mainlogger.info("")
                SBDS.mainlogger.info("---       Результати пошуку       ---")
                SBDS.mainlogger.info(f"Дискорд: &5{result.user.name}&r(&d{result.user.display_name}&r)")
                SBDS.mainlogger.info(f"Майнкрафт: &3{result.linked_user}&r.")
                SBDS.mainlogger.info("---                               ---")
                SBDS.mainlogger.info("")

            return

        if args[1] == "linklist":
            SBDS.mainlogger.info("---  Список прив'язаних учасників ---")
            for user in _cog.linked_users:
                SBDS.mainlogger.info(f"&3{user.user.display_name} &r(&b{user.user.name}&r) &r- &d{user.linked_user}&r.")
            SBDS.mainlogger.info("---                               ---")
            return

        SBDS.mainlogger.info("")
        SBDS.mainlogger.info("---  Допомога по команді linking  ---")
        SBDS.mainlogger.info(f"&elinking codes &r- &3Відображає список усіх активних кодів для прив'язки.")
        SBDS.mainlogger.info(f"&elinking linklist &r- &3Відображає список усіх прив'язаних учасників.")
        SBDS.mainlogger.info(f"&elinking newcode [Нікнейм] [Код] &r- &3Створити новий код для вказаного нікнейма. Якщо параметр КОД не вказаний, код буде згенерований автоматично.")
        SBDS.mainlogger.info(f"&elinking delcode [Код] &r- &3Видаляє вказаний код.")
        SBDS.mainlogger.info(f"&elinking link [Дискорд ID] [Нікнейм] &r- &3Примусово прив'язує користувача до майнкрафт нікнейма.")
        SBDS.mainlogger.info(f"&elinking unlink [Дискорд ID] &r- &3Примусово відв'язує користувача від майнкрафт прив'язки.")
        SBDS.mainlogger.info(f"&elinking search [Дискорд ID/Нікнейм] &r- &3Знайти прив'язку по ніку або ID користувача.")
        SBDS.mainlogger.info("---                               ---")
        SBDS.mainlogger.info("")

    @staticmethod
    @SBDS.main_bot.listen("on_ready")
    async def ready():
        await asyncio.sleep(2)

        try:
            from modules import consoleModule
            consoleModule.registerCommand(name="linking", command_executor=_cog.linking_console_command, description="linkingModule: Інструмент для роботи із прив'язками гравців", arguments="[codes/newcode/delcode/link/unlink/search]")
            _cog.console_module_support = True
        except Exception as error:
            _cog.logger.warn(f"Імпорт &dconsoleModule &rне вдався із помилкою: &c{error}&r.")
            _cog.logger.warn("Схоже що модуль &dconsoleModule &eне встановлено. Ігнорування реєстрації команди &blinking&r.")
            return

class _Api:

    class NoResultsFound(Exception):
        def __init__(self, query: str): super().__init__(f"Інформації за запитом {query} не знайдено")

    class UserNotLinked(Exception):
        def __init__(self, user: str): super().__init__(f"Користувач {user} не прив'язаний")

    class UserAlreadyLinked(Exception):
        def __init__(self, user: str): super().__init__(f"Користувач {user} вже прив'язаний")

    def searchUser(self, query: str) -> _LinkedUser:
        try:
            query = int(query)
            results = [user for user in _cog.linked_users if user.user.id == query]
            if not results: raise self.NoResultsFound(f"Інформації за ID {query} не знайдено")
            return results[0]
        except ValueError:
            results = [user for user in _cog.linked_users if user.linked_user == query]
            if not results: raise self.NoResultsFound(f"Інформації за нікном {query} не знайдено")
            return results[0]

    def unlinkUser(self, query: str) -> None:

        user = self.searchUser(query=query)

        SBDS.database.delUserKey(user_id=user.user.id, key='minecraft-link')

        _cog.linked_users_update_list()

    def linkUser(self, nickname: str, user_id: str) -> None:

        try:
            user = self.searchUser(query=user_id)
            raise self.UserAlreadyLinked(user.user.name)
        except self.NoResultsFound: pass

        SBDS.database.addUserKey(user_id=user_id, key="minecraft-link")
        SBDS.database.setUserKey(user_id=user_id, key="minecraft-link", value=json.dumps({"username": nickname, "linked-time": int(time.time())}))

        _cog.linked_users_update_list()





LinkingAPI = _Api()
_cog = _LinkingModuleCog()
def setup(bot: commands.InteractionBot):

    SBDS.settings.checkKeys(check_keys={"linking-module": dict}, path="modules")
    SBDS.settings.checkKeys(check_keys={"linking-web-server-port": int, "luckperms-database": dict, "sync-groups-to-roles": dict, "sync-booster": dict, "sync-linked-role": dict, "sync-nickname": dict, "embeds": dict}, path="modules.linking-module")

    SBDS.settings.checkKeys(check_keys={"host": str, "port": int, "database": str, "user": str, "password": str, "lp-users-table-name": str, "lp-permissions-table-name": str}, path="modules.linking-module.luckperms-database")

    # Перевірка sync-groups-to-roles.
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "sync-task-interval": int, "groups-list": list}, path="modules.linking-module.sync-groups-to-roles")
    # Перевірка sync-booster.
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "sync-task-interval": int, "rcon-connection": dict, "give-booster-group-command": list, "revoke-booster-group-command": list, "booster-group-name": str}, path="modules.linking-module.sync-booster")
    SBDS.settings.checkKeys(check_keys={"host": str, "port": int, "password": str}, path="modules.linking-module.sync-booster.rcon-connection")
    # Перевірка sync-linked-role.
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "sync-task-interval": int, "linked-role-id": int}, path="modules.linking-module.sync-linked-role")
    # Перевірка sync-nickname.
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "sync-task-interval": int, "nickname-format": str}, path="modules.linking-module.sync-nickname")

    SBDS.settings.checkKeys(check_keys={"LOADING": dict, "SEARCHING-IN-DATABASE": dict, "CODE-NOT-FOUND": dict, "INVALID-CODE": dict, "TIMED-OUT": dict, "DISCORD-ALREADY-LINKED": dict, "MINECRAFT-ALREADY-LINKED": dict, "NOT-LINKED": dict, "SUCCESSFULY-LINKED": dict, "SUCCESSFULY-LINKED-DM": dict, "SUCCESSFULY-UNLINKED": dict, "NICKNAME-CHANGED": dict}, path="modules.linking-module.embeds")

    bot.add_cog(_cog)