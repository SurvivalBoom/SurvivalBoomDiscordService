#    _____                  _             ______
#   / ___/__  ________   __(_)   ______ _/ / __ )____  ____  ____ ___
#   \__ \/ / / / ___/ | / / / | / / __ `/ / __  / __ \/ __ \/ __ `__ \
#  ___/ / /_/ / /   | |/ / /| |/ / /_/ / / /_/ / /_/ / /_/ / / / / / /
# /____/\__,_/_/    |___/_/ |___/\__,_/_/_____/\____/\____/_/ /_/ /_/
# SurvivalBoom Network 2023 | SurvivalBoom Discord Service
#             Main Bot File | By TIMURishche
#
#
import io
import json
import sys
import asyncio
import traceback
import os
import time
import datetime

try:
    import disnake
    import mysql.connector
    import yaml
    from mysql import connector
    from colorama import Fore, Style
    from disnake.ext import commands
except ImportError as e:
    print(f"ПОМИЛКА ІНІЦІАЛІЗАЦІЇ SURVIVALBOOM DISCORD SERVICE: Бібліотеку {e.name} не знайдено!")
    print(f"Створено файл requirements.txt із необхідними бібліотеками.")

    with open("requirements.txt" 'x') as file:
        file.write("""
        disnake
        mysql-connector-python
        PyYAML
        colorama
        requests
        validators
        spotipy
        aioconsole
        aiohttp
        wavelink
        mcstatus
        asyncrcon
        tiktok_downloader
        """)

    sys.exit()
class Main:
    """
    Головний клас API SurvivalBoomDiscordService.
    """

    # Ініціалізація бота.
    # noinspection PyTypeChecker
    def __init__(self):

        self._logging: Main._Logging = None
        self._start: Main._Start = None
        self._utils: Main._Utils = None
        self._main_bot: commands.InteractionBot = None

        self._start: Main._Start = None

        self._settings: Main.Settings = None
        self._modules: Main.Modules = None

        self._database: Main.Database = None

    # Запуск SurvivalBoomDiscordService
    def start(self) -> None:
        """
         Запуск SurvivalBoomDiscordService.
        """

        self._logging = Main._Logging()
        self._start = Main._Start(self)
        self._utils = Main._Utils(self)
        self._main_bot: commands.InteractionBot = commands.InteractionBot(intents=disnake.Intents.all(), test_guilds=[983845556407382057])

        self._start.send_logo()

        self._settings = Main.Settings(self)
        self._modules = Main.Modules(self)

        db_sect = self.settings.get(path="main-discord-bot-settings.discord-service-database")
        lg_cred = db_sect['login-credentials']
        self._database = Main.Database(mainclass=self, host=db_sect['database-host'], user=lg_cred['username'], password=lg_cred['password'], database_name=lg_cred['database-name'])

        self._start.final_start()

    # Метод зупинення роботи SurvivalBoomDiscordService
    async def stop(self) -> None:
        """
        Завершення роботи SurvivalBoomDiscordService і вихід з програми.
        """

        self._logging.loading("Вимикаємо SurvivalBoomDiscordService...")
        try:
            self._logging.info("Зупиняємо модулі...")

            for module in self._modules.loaded_modules:
                self._modules.unloadModule(name=module.name)
                self._logging.info(f" - &6{module.name}&r - [ &4STOPPED &r]")

            self._logging.info("Чекаємо поки усі модулі завершать роботу...")
            await asyncio.sleep(6)

            self._logging.info("Відключаємось від бази даних...")
            self.database.close()

            self._logging.info("Зупиняємо головного дискорд бота...")
            await self._main_bot.change_presence(status=disnake.Status.invisible)

            self._logging.completed("Роботу SurvivalBoomDiscordService успішно завершено!")

        except Exception as error:

            self._logging.error("Вникла помилка при спробі правильно завершити роботу SurvivalBoomDiscordService!")
            self._logging.error(str(error))
            self._logging.error("Примусово вбиваємо процес SurvivalBoomDiscordService!")

        self._logging.close()

        sys.exit("stopped")

    @property # Увімкнено відправлення traceback у консоль?
    def tracebackAllowed(self) -> bool:
        """
        Отримати значення налаштування traceback. Увімкнений він чи ні.
        :return: True якщо увімкнено, False якщо вимкнено
        """
        return self.settings.get('main-discord-bot-settings.errors.send-error-traceback-to-console')

    @property # Отримати об'єкт налаштувань.
    def settings(self):
        """
        Головний API для роботи із settings.yml
        :return: Об'єкт ініціалізованого та повністю готового API settings.yml
        """
        return self._settings
    @property # Отримати об'єкт логера.
    def mainlogger(self):
        """
        Логер SurvivalBoom DisordService.
        Відповідає за надсилання повідомлень у консоль й збереження логів у файл.
        :return: Об'єкт ініціалізованого та повністю готового логера.
        """
        return self._logging
    @property # Отримати об'єкт бази даних.
    def database(self):
        """
        Головний API для взаємодії SurvivalBoomDiscordService із базою даних.
        За допомогою цього API можна працювати із даними користувачів.
        :return: Об'єкт ініціалізованого та повністю готового API баз даних.
        """
        return self._database
    @property # Отримати об'єкт головного дискорд бота.
    def main_bot(self):
        """
        Головний Discord бот із яким працює SurvivalBoomDiscordService.
        :return: Об'єкт :class:`commands.InteractionBot` головного Discord бота.
        """
        return self._main_bot
    @property # Отримати об'єкт утиліт дискорд бота.
    def utils(self):
        """
        API для допомоги, спрощення й зменьшення коду. Має у собі купу методів які спростять вам життя.
        :return: Об'єкт API із різними допоміжними методами.
        """
        return self._utils
    @property # Отримати об'єкт модулів дискорд бота.
    def modules(self):
        """
        Головний API менеджера модулів. Через цей API ви можете керувати модулями SurvivalBoomDiscordService.
        :return: Об'єкт API менеджера модулів.
        """
        return self._modules
    @property
    def saveLogsAllowed(self):
        return self.settings.get("main-discord-bot-settings.logging.enable-logs-saving")

    # Клас методів запуску SurvivalBoomDiscordService.
    class _Start:

        def __init__(self, mainclass):
            self.mainclass: Main = mainclass

        # Метод відправлення лога SurvivalBoom Discord Service.
        def send_logo(self):

            # Відправляємо лого SurvivalBoomBADABUUUMMM!!!!
            logo_image = f""" \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n \n 

                   ____              _           _____                
                  / __/_ _______  __(_)  _____ _/ / _ )___  ___  __  _ 
                 _\ \/ // / __/ |/ / / |/ / _ `/ / _  / _ \/ _ \/  '  /
                /___/\_,_/_/  |___/_/|___/\_,_/_/____/\___/\___/_/_/_/

                SurvivalBoom Network 2023 | SurvivalBoom Discord Service
                                    By TIMURishche 🦖

                                    Version &a2.0

                """.split("\n")

            for line in logo_image:

                if not line == " ": self.mainclass.mainlogger.blue(line)
                else: print(" ")

            self.mainclass.mainlogger.loading("Завантаження SurvivalBoom Discord Service...")

        # Фінальний етап завантаження і повний запуск SurvivalBoom Discord Service.
        def final_start(self):

            @self.mainclass.main_bot.listen("on_ready")
            async def bot_started():

                self.mainclass.mainlogger.info("")
                self.mainclass.mainlogger.completed(f"Discord Service успішно запущено! (started)")
                self.mainclass.mainlogger.info("")

                await asyncio.sleep(1)

                self.mainclass.database.databaseguard_task = asyncio.create_task(self.mainclass.database.database_guard(), name="Main - DatabaseGuard")
                self.mainclass.database.check_users()

                if self.mainclass.saveLogsAllowed:
                    if not os.path.exists("logs"): os.mkdir("logs")

                    current_datetime = datetime.datetime.now().strftime("%Y.%m.%d-%H:%M:%S")
                    self.mainclass.mainlogger.log_file = open(f"logs/SBDS-LOG-{current_datetime}.log", "a")

                    for record in self.mainclass.mainlogger.startup_logs: self.mainclass.mainlogger.log_file.write(record)

                    for log_file in os.listdir("./logs"):
                        if log_file.startswith("SBDS-LOG-") and log_file.endswith(".log"):
                            try:
                                file_datetime_str = log_file.removeprefix("SBDS-LOG-").removesuffix(".log")
                                file_datetime = datetime.datetime.strptime(file_datetime_str, "%Y.%m.%d-%H:%M:%S")
                                age = datetime.datetime.now() - file_datetime

                                if age.days > self.mainclass._settings.get("main-discord-bot-settings.logging.delete-saved-logs-after"):
                                    os.remove(os.path.join("./logs", log_file))
                                    self.mainclass.mainlogger.info(f"Видалено старий лог файл &3{log_file}")

                            except ValueError: SurvivalBoomDiscordService.mainlogger.error(f"Неможливо розпізнати дату файла &3{log_file}")

            @self.mainclass.main_bot.listen("on_application_command")
            async def on_command(inter: disnake.ApplicationCommandInteraction):
                self.mainclass.mainlogger.info(f"Користувач &2{inter.user.name} &rвиконав команду &b/{inter.application_command.name}&r.")

            @self.mainclass.main_bot.listen("on_button_click")
            async def button_click(interaction: disnake.MessageInteraction):
                self.mainclass.mainlogger.info(f"Користувач &2{interaction.user.name} &rнатиснув кнопку &b{interaction.data['custom_id']}&r.")

            @self.mainclass.main_bot.listen("on_dropdown")
            async def button_click(inter: disnake.MessageInteraction):
                a = ", ".join(inter.data['values'])
                self.mainclass.mainlogger.info(f"Користувач &2{inter.user.name} &rвикликав dropdown &b{inter.data['custom_id']} &r(&3{a}&r).")

            try: asyncio.get_event_loop().run_until_complete(self._discord_bot())
            except: sys.exit('stopped')

        # Запуск дискорд бота.
        async def _discord_bot(self):

            try:

                self.mainclass.mainlogger.loading("Запускаємо дискорд бота...")
                await self.mainclass.main_bot.start(self.mainclass.settings.get("main-discord-bot-settings.token"))

            except disnake.LoginFailure as error2:

                if error2 == "Improper token has been passed": self.mainclass.mainlogger.error(f"Помилка при логіні дискорд бота: Неправильний токен головного бота.")

                else: self.mainclass.mainlogger.error(f"Помилка при логіні дискорд бота: {error2}")

                self.mainclass.mainlogger.error("Роботу SurvivalBoom Discord Service завершено.")

                sys.exit("error")

    # Клас утилітних модулів.
    class _Utils:

        def __init__(self, mainclass):
            self.mainclass: Main = mainclass

        @staticmethod  # Допоміжний метод для заміни плейсхолдерів у тексті.
        def replacePlaceholders(text: str, placeholders: dict):

            for placeholder, value in placeholders.items():
                text = text.replace(placeholder, str(value))

            return text

        # Метод створення embed повідомлення із конфігураційного файлу.
        def buildEmbed(self, path_to_embed: str, placehoders = None):

            # Перевіряємо вказаний embed на правильність.
            self.mainclass.settings.checkKeys(check_keys={"title": str, "description": str}, path=path_to_embed)

            # Якщо плейсхолдери не вказані, створюємо пустий список.
            if placehoders is None: placehoders = {}

            # Розбиваємо шлях до embedy на список.
            path_to_embed = path_to_embed.split(".")
            embed_info = self.mainclass.settings.get()

            # Отримання інформації про embed.
            for path in path_to_embed: embed_info = embed_info[path]

            # Намагаємось отримати кольор embed'y.
            embed_color = 0x000000
            if "color" in embed_info: embed_color = embed_info['color']

            # Гнерація базового embed повідомлення.
            embed = disnake.Embed(title=self.replacePlaceholders(text=embed_info['title'], placeholders=placehoders), color=embed_color, description=self.replacePlaceholders(text=embed_info['description'], placeholders=placehoders))

            # Намагаємось встановити thumbnail нашого embedу.
            if "thumbnail_url" in embed_info: embed.set_thumbnail(url=embed_info['thumbnail_url'])

            # Додаємо поля embed повідомлення.
            if "fields" in embed_info:

                for field in embed_info['fields']:
                    try:
                        if field['inline']: embed.add_field(name=self.replacePlaceholders(text=field['title'], placeholders=placehoders), value=self.replacePlaceholders(text=field['value'], placeholders=placehoders), inline=True)
                        else: embed.add_field(name=self.replacePlaceholders(text=field['title'], placeholders=placehoders), value=self.replacePlaceholders(text=field['value'], placeholders=placehoders), inline=False)
                    except: embed.add_field(name=self.replacePlaceholders(text=field['title'], placeholders=placehoders), value=self.replacePlaceholders(text=field['value'], placeholders=placehoders), inline=False)

            # Додаємо футер до embed повідомлення.
            if "footer" in embed_info:

                footer_image_url = None
                footer_text = None
                if "icon_url" in embed_info['footer']: footer_image_url = embed_info['footer']['icon_url']

                if "text" in embed_info['footer']: footer_text = self.replacePlaceholders(text=embed_info['footer']['text'], placeholders=placehoders)

                if footer_text is not None and footer_image_url is not None: embed.set_footer(text=footer_text, icon_url=footer_image_url)

                if footer_text is not None and footer_image_url is None: embed.set_footer(text=footer_text)

            # Повертаємо embed повідомлення.
            return embed

        # Метод створення кнопок із конфігураційного файлу.
        def buttonsBuilder(self, path_to_embed: str):

            # Розбиваємо шлях до embedy на список.
            path_to_embed = path_to_embed.split(".")
            buttons_info = self.mainclass.settings.get()

            for path in path_to_embed:
                buttons_info = buttons_info[path]

            buttons = disnake.ui.View()  # Створюємо об'єкт кнопок.

            # Додаємо кнопки.
            for button_info in buttons_info:

                style = button_info['style']  # Отримуємо стиль кнопки написаний у конфігурації кнопки.
                row = button_info['row']  # Отримуємо лінію кнопки написану у конфігурації кнопки.
                url = None

                if row > 5 or row < 1: row = 1  # Перевіряємо значення лінії кнопки. Якщо значення неправильне, приводимо в нормальне.

                if "url" in button_info: url = button_info['url']

                # Трансформуємо str у ButtonStyle кнопки.
                if style == "DANGER": style = disnake.ButtonStyle.danger
                elif style == "SUCCESS": style = disnake.ButtonStyle.success
                elif style == "BLURPLE": style = disnake.ButtonStyle.blurple
                elif style == "GRAY": style = disnake.ButtonStyle.gray
                elif style == "GREEN": style = disnake.ButtonStyle.green
                elif style == "RED": style = disnake.ButtonStyle.red
                elif style == "PRIMARY": style = disnake.ButtonStyle.primary
                elif style == "LINK": style = disnake.ButtonStyle.link
                else: style = disnake.ButtonStyle.secondary

                # Додаємо нашу кнопку.
                if url is None: buttons.add_item(disnake.ui.Button(style=style, custom_id=button_info['name'], label=button_info['text'], emoji=button_info['emoji'], row=row))
                else: buttons.add_item(disnake.ui.Button(style=style, label=button_info['text'], emoji=button_info['emoji'], row=row, url=url))

            return buttons

        # Метод відправлення помилки у спеціальний канал написаний у settings.yml
        def sendErrorToDebugChannel(self, file_name: str, error: Exception, place: str):

            try:

                needed_section = self.mainclass.settings.get('main-discord-bot-settings.errors.send-to-debug-channel')

                if not needed_section['enabled']: return

                embed = self.buildEmbed(path_to_embed="main-discord-bot-settings.errors.embeds.ERROR-TO-DEBUG-CHANNEL", placehoders={"{{ERROR}}": error, "{{PLACE}}": place, "{{MODULE}}": file_name})

                channel = self.mainclass.main_bot.get_channel(self.mainclass.settings.get('main-discord-bot-settings.errors.send-to-debug-channel.errors-channel-id'))

                asyncio.create_task(channel.send(embed=embed), name="SurvivalBoomDiscordService - SendErrorToDebugChannel")

            except Exception as error:

                self.mainclass.mainlogger.error("Сталася помилка при спробі відправити повідомлення про помилку у спеціальний канал.")
                self.mainclass.mainlogger.error(f"Помилка: &b{error}&4")

                if self.mainclass.settings.get('main-discord-bot-settings.errors.send-error-traceback-to-console'): self.mainclass.mainlogger.error(f"&3{traceback.format_exc()}")

                self.mainclass.mainlogger.error("")

        # Метод відправлення помилки користувачу.
        def sendErrorToUser(self, error: Exception, ctx: disnake.ApplicationCommandInteraction | disnake.MessageInteraction, edit: bool = True, ephemeral: bool = False):

            try:

                if self.mainclass.settings.get('main-discord-bot-settings.errors.send-error-details-to-user'): embed = self.buildEmbed(path_to_embed="main-discord-bot-settings.errors.embeds.ERROR-TO-USER-WITH-DETAILS", placehoders={"{{ERROR}}": error})
                else: embed = self.buildEmbed(path_to_embed="main-discord-bot-settings.errors.embeds.ERROR-TO-USER-NO-DETAILS")

                if edit is False: asyncio.create_task(ctx.send(embed=embed, ephemeral=ephemeral), name="SurvivalBoomDiscordService - SendErrorToUser")
                else: asyncio.create_task(ctx.edit_original_response(embed=embed), name="SurvivalBoomDiscordService - SendErrorToUser")

            except Exception as error:

                self.mainclass.mainlogger.error("Сталася помилка при спробі відправити повідомлення про помилку користувачу.")
                self.mainclass.mainlogger.error(f"Помилка: &b{error}&4")

                if self.mainclass.settings.get('main-discord-bot-settings.errors.send-error-traceback-to-console'): self.mainclass.mainlogger.error(traceback.format_exc())

                self.mainclass.mainlogger.error("")

        # Метод відправлення помилки у консоль SurvivalBoom Discord Service.
        def sendErrorToConsole(self, error: Exception, tracebackk: str, module: str, place: str):

            self.mainclass.mainlogger.error("")
            self.mainclass.mainlogger.error(f"--- Виникла помилка у &2{module} &c---")
            self.mainclass.mainlogger.error(f"Помилка: &2{error}.")
            self.mainclass.mainlogger.error(f"Де саме: &2{place}.")
            if self.mainclass.tracebackAllowed: self.mainclass.mainlogger.error(f"Stacktrace: &3{tracebackk}")
            self.mainclass.mainlogger.error("")

        # Метод перевірки чи має користувач ролі.
        def checkUserRoles(self, roles_id_list: list, member: disnake.Member):

            for role_id in roles_id_list:

                members = self.getGuild().get_role(role_id).members

                if member in members: return True

            return False

        def getGuild(self):
            return self.mainclass.main_bot.guilds[0]

    # Клас методів для роботи із settings.yml.
    class Settings:

        # Помилка 'вказаний ключ не зайдено'.
        class KeyNotFound(Exception):
            """
            Помилка 'вказаний ключ не зайдено'. Виникає коли вказаний ключ не знайдено у settings.yml за вказаним шляхом.
            """
            def __init__(self, key: str, path: str = None):

                if path is not None: message = f"Settings.yml: Ключ {key} за шляхом {path} не знайдено у settings.yml"
                else: message = f"Settings.yml: Ключ {key} не знайдено."

                super().__init__(message)

        # Помилка 'шлях не знайдено'.
        class PathNotFound(Exception):
            """
            Помилка 'шлях не знайдено'. Виникає коли ключів з інформацією у settings.yml за вказаним шляхом не знайдено.
            """
            def __init__(self, path: str):
                super().__init__(f"Settings.yml: Ключів за шляхом {path} не знайдено.")

        # Помилка 'неправильний тип ключа'.
        class IncorrectKeyType(Exception):
            """
            Помилка 'неправильний тип ключа'. Виникає коли вказаний ключ не відповідає необхідному типу даних. Наприклад якщо значення ключа це String, а потрібно Integer.
            """
            def __init__(self, key: str, req_type, prov_key, path: str = None):

                prov_key = str(prov_key).removeprefix("<class '").removesuffix("'>")
                req_type = str(req_type).removeprefix("<class '").removesuffix("'>")

                if path is None: message = f"Settings.yml: Тип значення ключа '{key}' повинно бути {req_type}. Отримано {prov_key}."
                else: message = f"Settings.yml: Тип значення ключа '{key}' за шляхом {path} повинно бути {req_type}. Отримано {prov_key}."

                super().__init__(message)

        class SettingsSection:

            def __init__(self, parentclass, path: str):
                self._section_data_path = path
                self._parentclass: Main.Settings = parentclass

            # Метод отримання інформацій із вказанної секції settings.yml
            def get(self, path: str = None):
                """
                Отримати інформацію із секції у setting.yml
                :param path: :class:`str` шлях до ключа з якого ви хочете отримати інформацію. Можете не заповнювати, щоб отримати інформацію з усієї секції.
                :return: :class:`dict` інформація із секції settings.yml.
                """
                data = self._parentclass.get(self._section_data_path)

                # Якщо шлях до ключів вказаний, перейти до тих ключів і передати їх.
                if path is not None:

                    path_str = path  # Відображене ім'я шляху.

                    if "." in path:
                        path = path.split(".")
                        for path_section in path:
                            try: data = data[path_section]
                            except: raise self._parentclass.PathNotFound(path=path_str)

                    # Якщо path вказаний, але там немає точки, виправити дані.
                    else:
                        try: data = data[path]
                        except: raise self._parentclass.PathNotFound(path=path_str)

                return data

            # Метод перевірки ключів settings.yml і їх типів у секції.
            def checkKeys(self, check_keys: dict, path: str = None) -> bool:
                """
                Перевірка секції settings.yml на наявність ключів і правильності типу їх значень.
                Застосовується для перевірки settings.yml перед тим як запустити модуль. Забезпечує чітку помилку, а не :class:`KeyError`.
                :param check_keys: :class:`dict[str, object]` ключі й типи які у них повинні бути.
                :param path: :class:`str` шлях до ключів.
                :return: True якщо перевірка пройшла успішно.
                :exception KeyNotFound: :class:`KeyNotFound` якщо вказаний ключ не знайдено.
                :exception PathNotFound: :class:`PathNotFound` якщо ключів за вказаним шляхом не знайдено.
                :exception IncorrectKeyType: :class:`IncorrectKeyType` якщо тип ключа не відповідає потрібному вказаному типу.
                """
                data = self._parentclass.get(self._section_data_path)  # Ну тіпа засовуємо дані із settings.yml у тимчасову змінну.

                path_str = path  # Відображене ім'я шляху.
                if path_str is not None: path_str = path_str.replace(".", "/")

                # Якщо шлях до ключів вказаний, перейти до тих ключів і записати їх у змінну.
                if path is not None:

                    if "." in path:
                        path = path.split(".")
                        for path_section in path:

                            try:
                                data = data[path_section]
                            except:
                                raise self._parentclass.PathNotFound(path=path_str)

                    # Якщо path вказаний, але там немає точки, виправити дані.
                    else:
                        try:
                            data = data[path]
                        except:
                            raise self._parentclass.PathNotFound(path=path_str)

                # Перевіряємо кожен необхідний ключ..
                for key in check_keys:

                    # Якщо ключа не знайдено у settings.yml за вказаним шляхом, вивести помилку.
                    if not key in data:
                        if path is None: raise self._parentclass.KeyNotFound(key=key)  # Якщо шлях не вказано, просто вивести який ключ не знайдено.
                        if path is not None: raise self._parentclass.KeyNotFound(key=key, path=path_str)  # Якщо шлях вказано, вивести ключ який не знайдено і шлях.

                    # Якщо ключ знайдено, але тип значення цього ключа не відповідає вказаному, вивести помилку.
                    if type(data[key]) != check_keys[key]:
                        if path is None: raise self._parentclass.IncorrectKeyType(key=key, prov_key=data[key], req_type=check_keys[key])  # Якщо шлях не вказано, просто вивести неправильний ключ.
                        if path is not None: raise self._parentclass.IncorrectKeyType(key=key, prov_key=data[key], req_type=check_keys[key], path=path_str)  # Якщо шлях вказано, вивести неправильний ключ і шлях.

                # Якщо все пройшло нормально, повернути True
                return True

        # Ініціалізація та завантаження settings.yml
        def __init__(self, mainclass):

            self.mainclass: Main = mainclass

            try:

                self._data = self._loadSettingsFile()
                self._checkSettings()

            except Exception as error:

                self.mainclass.mainlogger.error("Виникла помилка при завантаженні Settings.yml!")
                self.mainclass.mainlogger.error(str(error))
                self.mainclass.mainlogger.error("")
                self.mainclass.mainlogger.error("SurvivalBoom Discord Service зупинено!")

                sys.exit("error")

        # Метод отримання налаштувань settings.yml
        def get(self, path: str = None):
            """
            Отримати інформацію із settings.yml.
            :param path: :class:`str` шлях до ключа з якого ви хочете отримати інформацію. Можете не заповнювати, щоб отримати інформацію з усього файлу.
            :return: :class:`dict` інформація із settings.yml
            """
            data = self._data

            # Якщо шлях до ключів вказаний, перейти до тих ключів і передати їх.
            if path is not None:

                path_str = path  # Відображене ім'я шляху.

                if "." in path:
                    path = path.split(".")
                    for path_section in path:
                        try: data = data[path_section]
                        except: raise self.PathNotFound(path=path_str)

                # Якщо path вказаний, але там немає точки, виправити дані.
                else:
                    try: data = data[path]
                    except: raise self.PathNotFound(path=path_str)

            return data

        # Метод перевірки ключів settings.yml і їх типів.
        def checkKeys(self, check_keys: dict, path: str = None, custom_data: dict = None) -> bool:
            """
            Перевірка settings.yml (або кастомний :class:`dict`) на наявність ключів і правильності типу їх значень.
            Застосовується для перевірки settings.yml перед тим як запустити модуль. Забезпечує чітку помилку, а не :class:`KeyError`.
            :param check_keys: :class:`dict[str, object]` ключі й типи які у них повинні бути.
            :param path: :class:`str` шлях до ключів.
            :param custom_data: :class:`dict` кастомна інформація яку потрібно перевірити.
            :return: True якщо перевірка пройшла успішно.
            :exception KeyNotFound: :class:`KeyNotFound` якщо вказаний ключ не знайдено.
            :exception PathNotFound: :class:`PathNotFound` якщо ключів за вказаним шляхом не знайдено.
            :exception IncorrectKeyType: :class:`IncorrectKeyType` якщо тип ключа не відповідає потрібному вказаному типу.
            """
            if custom_data is None: data = self._data  # Ну тіпа засовуємо дані із settings.yml у тимчасову змінну.
            else: data = custom_data

            path_str = path  # Відображене ім'я шляху.
            if path_str is not None: path_str = path_str.replace(".", "/")

            # Якщо шлях до ключів вказаний, перейти до тих ключів і записати їх у змінну.
            if path is not None:

                if "." in path:
                    path = path.split(".")
                    for path_section in path:

                        try: data = data[path_section]
                        except: raise self.PathNotFound(path=path_str)

                # Якщо path вказаний, але там немає точки, виправити дані.
                else:
                    try: data = data[path]
                    except: raise self.PathNotFound(path=path_str)

            # Перевіряємо кожен необхідний ключ..
            for key in check_keys:

                # Якщо ключа не знайдено у settings.yml за вказаним шляхом, вивести помилку.
                if not key in data:
                    if path is None: raise self.KeyNotFound(key=key)  # Якщо шлях не вказано, просто вивести який ключ не знайдено.
                    if path is not None: raise self.KeyNotFound(key=key, path=path_str)  # Якщо шлях вказано, вивести ключ який не знайдено і шлях.

                # Якщо ключ знайдено, але тип значення цього ключа не відповідає вказаному, вивести помилку.
                if type(data[key]) != check_keys[key]:
                    if path is None: raise self.IncorrectKeyType(key=key, prov_key=data[key], req_type=check_keys[key])  # Якщо шлях не вказано, просто вивести неправильний ключ.
                    if path is not None: raise self.IncorrectKeyType(key=key, prov_key=data[key], req_type=check_keys[key], path=path_str)  # Якщо шлях вказано, вивести неправильний ключ і шлях.

            # Якщо все пройшло нормально, повернути True
            return True

        @staticmethod # Метод завантаження settings.yml
        def _loadSettingsFile() -> dict:

            with open("settings.yml", 'r', encoding='utf-8') as file:

                return yaml.load(file, yaml.FullLoader)

        # Метод перевірки settings.yml на необхідні ключі.
        def _checkSettings(self, custom_data: dict = None) -> bool:

            self.checkKeys(check_keys={"token": str, "errors": dict, "discord-service-database": dict, "logging": dict}, path="main-discord-bot-settings", custom_data=custom_data)
            self.checkKeys(check_keys={"delete-saved-logs-after": int, "enable-logs-saving": bool}, path="main-discord-bot-settings.logging", custom_data=custom_data)
            self.checkKeys(check_keys={"send-to-debug-channel": dict, "send-error-details-to-user": bool, "send-error-traceback-to-console": bool, "embeds": dict}, path="main-discord-bot-settings.errors", custom_data=custom_data)
            self.checkKeys(check_keys={"enabled": bool, "errors-channel-id": int}, path="main-discord-bot-settings.errors.send-to-debug-channel", custom_data=custom_data)
            self.checkKeys(check_keys={"login-credentials": dict, "database-host": str, "table-settings": dict}, path="main-discord-bot-settings.discord-service-database", custom_data=custom_data)
            self.checkKeys(check_keys={"database-name": str, "username": str, "password": str}, path="main-discord-bot-settings.discord-service-database.login-credentials", custom_data=custom_data)
            self.checkKeys(check_keys={"table-prefix": str, "userdata-table-name": str}, path="main-discord-bot-settings.discord-service-database.table-settings", custom_data=custom_data)
            self.checkKeys(check_keys={"modules": dict})

            return True

        # Метод перезавантаження налаштувань із settings.yml
        def reloadSettings(self) -> None:
            """
            Перезавантажити settings.yml.
            """

            data = self._loadSettingsFile()

            self._checkSettings(custom_data=data)

            self._data = data
        
        # Створює секцію і повертає об'єкт.
        def createSection(self, path: str) -> SettingsSection:
            self.get(path=path)
            return self.SettingsSection(parentclass=self, path=path)

    # Клас логера SurvivalBOomDiscordService.
    class _Logging:

        # Клас логера конктретноо модуля.
        class ModuleLogger:
            """
            Об'єкт логера конкретного модуля. Зручно тим що назва модуля пишеться перед повідомленням.
            """

            def __init__(self, name: str, mainlogger):
                self.mainlogger = mainlogger
                self.loggername: str = name

            def info(self, text: str):
                """
                Відправити у консоль повідомлення:\n
                "[12:34:16 INFO] [module_name]: This Is Info!"\n
                Рівень логування: INFO

                Кольорові коди:
                ----
                &0 -> BLACK\n
                &2 -> GREEN\n
                &3 -> CYAN\n
                &4 -> RED\n
                &5 -> MAGENTA\n
                &6 -> LIGHTYELLOW_EX\n
                &a -> LIGHTGREEN_EX\n
                &b -> BLUE\n
                &c -> LIGHTRED_EX\n
                &d -> LIGHTMAGENTA_EX\n
                &e -> YELLOW\n
                &r -> RESET_ALL\n

                """
                self.mainlogger.print(f"[{self.mainlogger.time_formatted()} &rINFO] [&2{self.loggername}&r]: {text}")

            def error(self, text: str):
                """
                Відправити у консоль повідомлення:\n
                "[12:34:16 ERROR] [module_name]: This Is Info!"\n
                Рівень логування: ERROR

                Кольорові коди:
                ----
                &0 -> BLACK\n
                &2 -> GREEN\n
                &3 -> CYAN\n
                &4 -> RED\n
                &5 -> MAGENTA\n
                &6 -> LIGHTYELLOW_EX\n
                &a -> LIGHTGREEN_EX\n
                &b -> BLUE\n
                &c -> LIGHTRED_EX\n
                &d -> LIGHTMAGENTA_EX\n
                &e -> YELLOW\n
                &r -> RESET_ALL\n
                """
                self.mainlogger.print(f"&4[{self.mainlogger.time_formatted()} &4ERROR] [&2{self.loggername}&4]: {text}")

            def completed(self, text: str):
                """
                Відправити у консоль повідомлення:\n
                "[12:34:16 DONE] [module_name]: This Is Info!"\n
                Рівень логування: COMPLETED

                Кольорові коди:
                ----
                &0 -> BLACK\n
                &2 -> GREEN\n
                &3 -> CYAN\n
                &4 -> RED\n
                &5 -> MAGENTA\n
                &6 -> LIGHTYELLOW_EX\n
                &a -> LIGHTGREEN_EX\n
                &b -> BLUE\n
                &c -> LIGHTRED_EX\n
                &d -> LIGHTMAGENTA_EX\n
                &e -> YELLOW\n
                &r -> RESET_ALL\n
                """
                self.mainlogger.print(f"&a[{self.mainlogger.time_formatted()} &aDONE] [&2{self.loggername}&a]: {text}")

            def loading(self, text: str):
                """
                Відправити у консоль повідомлення:\n
                "[12:34:16 LOADING] [module_name]: This Is Info!"\n
                Рівень логування: LOADING

                Кольорові коди:
                ----
                &0 -> BLACK\n
                &2 -> GREEN\n
                &3 -> CYAN\n
                &4 -> RED\n
                &5 -> MAGENTA\n
                &6 -> LIGHTYELLOW_EX\n
                &a -> LIGHTGREEN_EX\n
                &b -> BLUE\n
                &c -> LIGHTRED_EX\n
                &d -> LIGHTMAGENTA_EX\n
                &e -> YELLOW\n
                &r -> RESET_ALL\n
                """
                self.mainlogger.print(f"&6[{self.mainlogger.time_formatted()} &6LOADING] [&2{self.loggername}&6]: {text}")

            def warn(self, text: str):
                """
                Відправити у консоль повідомлення:\n
                "[12:34:16 WARN] [module_name]: This Is Info!"\n
                Рівень логування: COMPLETED

                Кольорові коди:
                ----
                &0 -> BLACK\n
                &2 -> GREEN\n
                &3 -> CYAN\n
                &4 -> RED\n
                &5 -> MAGENTA\n
                &6 -> LIGHTYELLOW_EX\n
                &a -> LIGHTGREEN_EX\n
                &b -> BLUE\n
                &c -> LIGHTRED_EX\n
                &d -> LIGHTMAGENTA_EX\n
                &e -> YELLOW\n
                &r -> RESET_ALL\n
                """
                self.mainlogger.print(f"&e[{self.mainlogger.time_formatted()} &eWARN] [&2{self.loggername}&e]: {text}")

        def __init__(self):
            self.log_file: io.FileIO = ...
            self.startup_logs = []

        def close(self):
            self.log_file.close()

        def info(self, text: str):
            """
            Відправити у консоль повідомлення:\n
            "[12:34:16 INFO] [module_name]: This Is Info!"\n
            Рівень логування: INFO

            Кольорові коди:
            ----
            &0 -> BLACK\n
            &2 -> GREEN\n
            &3 -> CYAN\n
            &4 -> RED\n
            &5 -> MAGENTA\n
            &6 -> LIGHTYELLOW_EX\n
            &a -> LIGHTGREEN_EX\n
            &b -> BLUE\n
            &c -> LIGHTRED_EX\n
            &d -> LIGHTMAGENTA_EX\n
            &e -> YELLOW\n
            &r -> RESET_ALL\n
            """
            self.print(f"[{self.time_formatted()} &rINFO]: {text}")

        def error(self, text: str):
            """
            Відправити у консоль повідомлення:\n
            "[12:34:16 ERROR] [module_name]: This Is Info!"\n
            Рівень логування: ERROR

            Кольорові коди:
            ----
            &0 -> BLACK\n
            &2 -> GREEN\n
            &3 -> CYAN\n
            &4 -> RED\n
            &5 -> MAGENTA\n
            &6 -> LIGHTYELLOW_EX\n
            &a -> LIGHTGREEN_EX\n
            &b -> BLUE\n
            &c -> LIGHTRED_EX\n
            &d -> LIGHTMAGENTA_EX\n
            &e -> YELLOW\n
            &r -> RESET_ALL\n
            """
            self.print(f"&4[{self.time_formatted()} &4ERROR]: {text}")

        def completed(self, text: str):
            """
            Відправити у консоль повідомлення:\n
            "[12:34:16 DONE] [module_name]: This Is Info!"\n
            Рівень логування: COMPLETED

            Кольорові коди:
            ----
            &0 -> BLACK\n
            &2 -> GREEN\n
            &3 -> CYAN\n
            &4 -> RED\n
            &5 -> MAGENTA\n
            &6 -> LIGHTYELLOW_EX\n
            &a -> LIGHTGREEN_EX\n
            &b -> BLUE\n
            &c -> LIGHTRED_EX\n
            &d -> LIGHTMAGENTA_EX\n
            &e -> YELLOW\n
            &r -> RESET_ALL\n
            """
            self.print(f"&a[{self.time_formatted()} &aDONE]: {text}")

        def loading(self, text: str):
            """
            Відправити у консоль повідомлення:\n
            "[12:34:16 LOADING] [module_name]: This Is Info!"\n
            Рівень логування: LOADING

            Кольорові коди:
            ----
            &0 -> BLACK\n
            &2 -> GREEN\n
            &3 -> CYAN\n
            &4 -> RED\n
            &5 -> MAGENTA\n
            &6 -> LIGHTYELLOW_EX\n
            &a -> LIGHTGREEN_EX\n
            &b -> BLUE\n
            &c -> LIGHTRED_EX\n
            &d -> LIGHTMAGENTA_EX\n
            &e -> YELLOW\n
            &r -> RESET_ALL\n
            """
            self.print(f"&6[{self.time_formatted()} &6LOADING]: {text}")

        def warn(self, text: str):
            """
            Відправити у консоль повідомлення:\n
            "[12:34:16 WARN] [module_name]: This Is Info!"\n
            Рівень логування: COMPLETED

            Кольорові коди:
            ----
            &0 -> BLACK\n
            &2 -> GREEN\n
            &3 -> CYAN\n
            &4 -> RED\n
            &5 -> MAGENTA\n
            &6 -> LIGHTYELLOW_EX\n
            &a -> LIGHTGREEN_EX\n
            &b -> BLUE\n
            &c -> LIGHTRED_EX\n
            &d -> LIGHTMAGENTA_EX\n
            &e -> YELLOW\n
            &r -> RESET_ALL\n
            """
            self.print(f"&e[{self.time_formatted()} &eWARN]: {text}")

        def blue(self, text: str):
            self.print(f"{Fore.LIGHTCYAN_EX}[{self.time_formatted()}]: {text}")

        # Створити кастомний логер для модуля.
        def createModuleLogger(self, module_name: str): return self.ModuleLogger(mainlogger=self, name=module_name)

        @staticmethod
        def replace_color_codes(text: str):

            return text \
                .replace("&0", Fore.BLACK) \
                .replace("&2", Fore.GREEN) \
                .replace("&3", Fore.CYAN) \
                .replace("&4", Fore.RED) \
                .replace("&5", Fore.MAGENTA) \
                .replace("&6", Fore.LIGHTYELLOW_EX) \
                .replace("&a", Fore.LIGHTGREEN_EX) \
                .replace("&b", Fore.BLUE) \
                .replace("&c", Fore.LIGHTRED_EX) \
                .replace("&d", Fore.LIGHTMAGENTA_EX) \
                .replace("&e", Fore.YELLOW) \
                .replace("&f", Fore.WHITE) \
                .replace("&r", Style.RESET_ALL)

        @staticmethod
        def remove_color_codes(text: str):
            return text \
                .replace("&0", "") \
                .replace("&2", "") \
                .replace("&3", "") \
                .replace("&4", "") \
                .replace("&5", "") \
                .replace("&6", "") \
                .replace("&a", "") \
                .replace("&b", "") \
                .replace("&c", "") \
                .replace("&d", "") \
                .replace("&e", "") \
                .replace("&f", "") \
                .replace("&r", "") \
                .replace(f"{Fore.LIGHTCYAN_EX}", "")

        @staticmethod
        def time_formatted():
            return time.strftime("%H:%M:%S")

        def print(self, text: str):
            if self.log_file is not ...: self.log_file.write(self.remove_color_codes(text) + "\n")
            else: self.startup_logs.append(self.remove_color_codes(text) + "\n")
            text = text + "&r"
            print(self.replace_color_codes(text))

    # Клас менеджера бази даних SurvivalBoomDiscordService.
    class Database:

        class KeyNotFound(Exception):

            def __init__(self, key: str, path: str = None):

                if path is None: message = f"Ключ '{key}' не знайдено даних користувача."
                else: message = f"Ключ '{key}' за шляхом '{path}' не знайдено даних користувача."

                super().__init__(message)

        class NoResultFromDatabase(Exception):

            def __init__(self, data: str): super().__init__(f"Результатів за ID '{data}' не знайдено")

        class KeyAlreadyExist(Exception):
            def __init__(self, key: str): super().__init__(f"Ключ {key} вже існує у даних користувача.")

        def __init__(self, host: str, database_name: str, user: str, password: str, mainclass):

            self.mainclass: Main = mainclass
            self.mainclass.mainlogger.info("Ініціалізація бази даних...")
            self.logger: SurvivalBoomDiscordService.mainlogger.ModuleLogger = self.mainclass.mainlogger.createModuleLogger("Database")
            self.databaseguard_task: asyncio.Task = ...

            try:

                self.database: connector.MySQLConnection = connector.connect(host=host, database=database_name, user=user, passwd=password)
                self.userdata_table_name: str = self.mainclass.settings.get('main-discord-bot-settings.discord-service-database.table-settings.table-prefix') + self.mainclass.settings.get('main-discord-bot-settings.discord-service-database.table-settings.userdata-table-name')

                self.check_tables()

            except Exception as error:

                self.logger.error("Виникла помилка при ініціалізації бази даних!")
                self.logger.error(str(error))
                self.logger.error("")
                self.logger.error("SurvivalBoom Discord Service зупинено!")

                sys.exit("error")

            @self.mainclass.main_bot.listen("on_member_join")
            async def add_user_to_db(member: disnake.Member):

                if member.bot: return

                try:

                    result = self.executeSqlWithResult(f"SELECT UserID FROM {self.userdata_table_name} WHERE UserID = '{member.id}'")

                    if result: return

                    self.executeSql(f"INSERT INTO {self.userdata_table_name} (UserID, Data) VALUES ('{member.id}', '%s')".replace("%s", "{}"))

                    self.logger.info(f"Додано користувача &b{member.name} &rу базу даних.")

                except Exception as errorr:

                    self.logger.error(f"Виникла помилка при додаванні у базу даних користувача &b{member.name}&4.")
                    self.logger.error(f"{errorr}")
                    self.logger.error(traceback.format_exc())
                    self.mainclass.utils.sendErrorToDebugChannel(file_name="MAIN", error=errorr, place="database.events.add_user_to_db()")

            @self.mainclass.main_bot.listen("on_member_remove")
            async def remove_user_from_db(member: disnake.Member):

                try:

                    result = self.executeSqlWithResult(f"SELECT UserID FROM {self.userdata_table_name} WHERE UserID = '{member.id}'")

                    if not result: return

                    self.executeSql(f"DELETE FROM {self.userdata_table_name} WHERE UserID = '{member.id}'")

                    self.logger.info(f"Видалено користувача &b{member.name} &rіз бази даних.")

                except Exception as errorr:

                    self.logger.error(f"Виникла помилка при видаленні користувача &b{member.name} &4з бази даних.")
                    self.logger.error(f"{errorr}")
                    self.logger.error(traceback.format_exc())
                    self.mainclass.utils.sendErrorToDebugChannel(file_name="MAIN", error=errorr, place="database.events.remove_user_from_db()")

        def close(self):

            self.databaseguard_task.cancel()
            self.database.close()

        # DataBaseGuard - Захист бази даних. Забезбечує роботу бази даних і керування даними користувачів.
        async def database_guard(self):

            while True:
                await asyncio.sleep(5)
                try:

                    # Перепідключення до бази даних якщо підключення від'єднано.
                    try:

                        cursor = self.database.cursor()
                        cursor.close()

                    except connector.errors.OperationalError:

                        self.database.reconnect(attempts=5, delay=1)
                        self.logger.warn("Перепідключено до бази даних.")

                except Exception as error:

                    self.logger.error("")
                    self.logger.error(f"&5DataBaseGuard&r: Виникла критична помилка: &b{error}&c.")
                    if self.mainclass.settings.get('main-discord-bot-settings.errors.send-error-traceback-to-console'): self.mainclass.mainlogger.error(f"Traceback: &3{traceback.format_exc()}")
                    self.logger.error("")
                    self.logger.error("SurvivalBoom Discord Service аварійно зупинено!")

                    await self.mainclass.stop()

        # Перевірка наявності таблиці із даними користувачів.
        # Якщо немає, створити таблицю.
        def check_tables(self):

            cursor = self.database.cursor(buffered=True)

            try: cursor.execute(f"SELECT * FROM {self.userdata_table_name}")

            except mysql.connector.ProgrammingError:
                cursor.execute(f"CREATE TABLE {self.userdata_table_name} (UserID varchar(255), Data mediumtext);")
                self.mainclass.mainlogger.info(f"Створено таблицю {self.userdata_table_name} у базі даних.")
                self.database.commit()

            finally: cursor.close()

        # Додавання усіх користувачів дискорд сервера у базу даних які не записані у базі даних.
        def check_users(self):

            guild_members = self.mainclass.utils.getGuild().members

            result = self.executeSqlWithResult(f"SELECT UserID FROM {self.userdata_table_name}")

            result = str(result).replace("[", "").replace("]", "").replace("'", "").replace("(", "").replace(")", "".replace(",", "")).replace(",", "").split(" ")

            for member in guild_members:

                if not str(member.id) in result and not member.bot:

                    self.executeSql(f"INSERT INTO {self.userdata_table_name} (UserID, Data) VALUES ('{member.id}', '%s')".replace("%s", "{}"))
                    self.logger.info(f"Додано користувача &b{member.name} &rу базу даних.")

            guild_members_ids = [str(member.id) for member in guild_members]
            for r in result:

                if not r in guild_members_ids:

                    self.executeSql(f"DELETE FROM {self.userdata_table_name} WHERE UserID = '{r}'")

                    self.logger.info(f"Видалено користувача &b{r} &rіз бази даних.")

        # Метод отримання даних користувача із бази даних SurvivalBoom Discord Service.
        def getUserData(self, user_id, key: str = None, path: str = None):

            path_str = path

            if path is None: path = []
            elif not "." in path: path = [path]
            else: path = path.split(".")

            cursor = self.database.cursor(buffered=True)
            cursor.execute(f"SELECT * FROM {self.userdata_table_name} WHERE UserID='{user_id}'")
            result = cursor.fetchall()
            cursor.close()

            if result:
                result = json.loads(result[0][1])

                for section in path:
                    try: result = result[section]
                    except KeyError: raise self.KeyNotFound(section, path_str)

                try:
                    if key is not None: result = result[key]
                except KeyError: raise self.KeyNotFound(key)

                return result

            else: raise self.NoResultFromDatabase(f"Результатів за ID {user_id} не знайдено")

        # Метод встановлення конкретного ключа даних користувача у базі даних SurvivalBoom Discord Service.
        def setUserKey(self, user_id: str, value: str, key: str):

            cursor = self.database.cursor(buffered=True)

            cursor.execute(f"SELECT Data FROM {self.userdata_table_name} WHERE UserID = '{user_id}'")
            result = cursor.fetchone()

            if result is None:
                cursor.close()
                raise self.NoResultFromDatabase(user_id)

            json_data: dict = json.loads(result[0])

            if not key in json_data:
                cursor.close()
                raise self.KeyNotFound(key)

            json_data.update({key: value})

            cursor.execute(f"UPDATE {self.userdata_table_name} SET UserID = %s, Data = %s WHERE UserID = %s", (user_id, json.dumps(json_data), user_id))
            self.database.commit()

            cursor.close()

        def addUserKey(self, user_id: str, key: str):

            cursor = self.database.cursor(buffered=True)

            cursor.execute(f"SELECT Data FROM {self.userdata_table_name} WHERE UserID = '{user_id}'")
            result = cursor.fetchone()

            if result is None: raise self.NoResultFromDatabase(f"Результатів за ID {user_id} не знайдено")

            json_data: dict = json.loads(result[0])

            if key in json_data:
                cursor.close()

                raise self.KeyAlreadyExist(key)

            json_data.update({key: "None"})

            cursor.execute(f"UPDATE {self.userdata_table_name} SET UserID = %s, Data = %s WHERE UserID = %s", (user_id, json.dumps(json_data), user_id))
            self.database.commit()

            cursor.close()

        def delUserKey(self, user_id: str, key: str):

            cursor = self.database.cursor(buffered=True)

            cursor.execute(f"SELECT Data FROM {self.userdata_table_name} WHERE UserID = '{user_id}'")
            result = cursor.fetchone()

            if result is None:
                cursor.close()

                raise self.NoResultFromDatabase(f"Результатів за ID {user_id} не знайдено")

            json_data: dict = json.loads(result[0])

            if not key in json_data:
                cursor.close()

                raise self.KeyNotFound(key)

            json_data.pop(key)

            cursor.execute(f"UPDATE {self.userdata_table_name} SET UserID = %s, Data = %s WHERE UserID = %s", (user_id, json.dumps(json_data), user_id))
            self.database.commit()

            cursor.close()

        def executeSqlWithResult(self, sql: str, commit: bool = False):

            cursor = self.database.cursor(buffered=True)

            try:

                cursor.execute(sql.replace("{USERDATA_TABLE}", self.userdata_table_name))

                if commit is True: self.database.commit()

                return cursor.fetchall()

            finally: cursor.close()

        def executeSql(self, sql: str):
            cursor = self.database.cursor(buffered=True)
            try:
                cursor.execute(sql.replace("{USERDATA_TABLE}", self.userdata_table_name))
                self.database.commit()
            finally: cursor.close()

    # Клас менеджера модулів SurvivalBoomDiscordService.
    class Modules:
        """
        Менеджер модулів SurvivalBoomDiscordService. Має у собі методи для керування модулями.
        Дуже зручна фігня!
        """

        # Клас модуля.
        class Module:
            """Об'єкт модуля SurvivalBoomDiscordService."""

            def __init__(self, file: str, name: str):

                self._file: str = file
                self._name: str = name

                self._status: str = "OK"

                self._error: Exception = ...
                self._traceback: str = ...

            @property # Отримати шлях до модуля.
            def file(self) -> str:
                """
                Отримати шлях до файлу модуля.
                :return: str шлях до модуля.
                """
                return self._file

            @property # Отримати назву модуля.
            def name(self) -> str:
                """
                Отримати назву модуля.
                :return: str назва модуля.
                """
                return self._name

            @property # Отримати статус модуля
            def status(self) -> str:
                """
                Отримати статус модуля. Не рекумедуємо використовувати просто так. Використовуйте із modules.Status...
                :return: str статус модуля.
                """
                return self._status

            @property # Отримати помилку модуля.
            def error(self) -> Exception | None:
                """
                Отримати помилку із-за якої модуль перестав працювати.
                Повертає None якщо модуль все ще працює, або помилка не вказана.
                :return: Exception | None помилка модуля.
                """
                return self._error

            @property # Отримати traceback.
            def traceback(self) -> str | None:
                """
                Отримати traceback до помилки яка привела до вимкненя модуля. Повертає None якзо модуль все ще працює або якщо traceback не вказаний.
                :return: str traceback до помилки.
                """
                return self._traceback

            # Метод встановлення помилки.
            def set_error(self, error: Exception | None):
                """
                Встановлює помилку через яку модуль не працює.
                :param error: :class:`Exception` помилка.
                """
                self._error = error

            # Метод встановленя traceback до помилки.
            def set_tracebacck(self, trace: str | None):
                """
                Встановлює traceback до помилки яка привела до вимкнення модуля.
                :param trace: :class:`str` traceback.
                """
                self._traceback = trace

            # Метод встановлення статуса модуля.
            def set_status(self, status):
                """
                Встановити статус модуля. Не рекомендуємо встановлювати самому. Використовуйте modules.Status...
                :param status: modules.Status...
                """
                self._status = status

        # Клас помилки незнайденного модуля.
        class ModuleNotFound(Exception):
            """
            Помилка незнайденного модуля. Виникає коли ви намагаєтись отримати інформацію про відсутній модуль.
            """
            def __init__(self, name: str):
                super().__init__(f"Модуль {name} не знайдено")

        # Функція ініціалізації менеджера модулів й завантаження усіх модулів
        def __init__(modules, mainclass):

            modules.mainclass: Main = mainclass
            modules._modules: dict[str, modules.Module] = {}

            modules.mainclass.mainlogger.info("Завантажуємо модулі SurvivalBoomDiscordService...")

            for file in os.listdir(f"./modules"):

                if file.endswith('.py') and file != "__init__.py":

                    name = file[:-3]

                    modules._modules.update({name: modules.Module(file="modules/" + file[:-3], name=name)})
                    module = modules._modules[name]

                    try: modules.mainclass.main_bot.load_extension(f"modules.{name}")

                    except SystemExit: raise SystemExit

                    except KeyboardInterrupt: pass

                    except Exception as error:

                        module.set_status(modules.StatusCrashed)
                        module.set_tracebacck(traceback.format_exc())
                        module.set_error(error)

        @property # Отримати str значення статусу OK
        def StatusOK(self):
            """
            Отримати :class:`str` значення статусу OK модуля.
            Дуже зручно коли вам потрібно перевірити статус модуля.
            :return: :class:`str` значення статусу OK.
            """
            return "OK"

        @property # Отримати str значення статусу CRASHED
        def StatusCrashed(self):
            """
            Отримати :class:`str` значення статусу CRASHED модуля.
            Дуже зручно коли вам потрібно перевірити статус модуля.
            :return: :class:`str` значення статусу CRADHED.
            """
            return "CRASHED"

        @property # Отримати str значення статусу UNLOADED
        def StatusUnloaded(self):
            """
            Отримати :class:`str` значення статусу UNLOADED модуля.
            Дуже зручно коли вам потрібно перевірити статус модуля.
            :return: :class:`str` значення статусу UNLOADED.
            """
            return "UNLOADED"

        @property # Отримати список усіх завантажених модулів.
        def loaded_modules(self) -> list[Module]:
            """
            Отримати список усіх завантажених (і працюючих) модулів.
            :return: :class:`list[Module]`
            """
            return [self._modules[module] for module in self._modules if self._modules[module].status == self.StatusOK]

        @property # Отримати список усіх вивантажених модулів.
        def unloaded_modules(self) -> list[Module]:
            """
            Отримати список усіх вивантажених (не працюючих) модулів.
            :return: :class:`list[Module]`
            """
            return [self._modules[module] for module in self._modules if self._modules[module].status != self.StatusOK]

        @property # Отримати список абсолютно всіх модулів.
        def all_modules(self) -> list[Module]:
            """
            Отримати список абсолютно усіх модулів.
            :return: :class:`list[Module]`
            """
            return [self._modules[module] for module in self._modules]

        # Метод отримання модуля по назві.
        def getModule(self, name: str) -> Module:
            """
            Отримати модуль за вказанною назвою.
            :param name: :class:`str` назва модуля.
            :return: :class:`Module` об'єкт знайденного модуля.
            :exception ModuleNotFound: :class:`ModuleNotFound` якщо модуль із такоб назвоюб не знайдено.
            """

            try: return self._modules[name]
            except KeyError: raise self.ModuleNotFound

        # Метод відвантаження модуля.
        def unloadModule(self, name: str, crashed: bool = False, error: Exception = "Не відомо", tracebackk: str = "Не відомо"):
            """
            Відвантажує модуль із вказаною назвою.
            :param name: :class:`str` назва модуля.
            :param crashed: :class:`bool` чи крашнувся модуль? Якщо так, статус модуля буде CRASHED, якщо ні UNLOADED
            :param error: :class:`Exception` помилка яка призвела до краша модуля.
            :param tracebackk: :class:`str` traceback до помилки.
            :exception ExtensionNotFound: :class:`disnake.ExtensionNotFound` якщо модуль із такою назвою не знайдено.
            :exception ExtentionNotLoaded: :class:`disnake.ExtentionNotLoaded` якщо модуль не завантажений.
            """

            self.mainclass.main_bot.unload_extension(f"modules.{name}")

            module = self._modules[name]

            if crashed:

                module.set_status(self.StatusCrashed)
                module.set_error(error)
                module.set_tracebacck(tracebackk)

            else: module.set_status(self.StatusUnloaded)

        # Метод завантаження.
        def loadModule(self, name: str):
            """
            Завантажити модуль із вказаною назвою.
            :param name: :class:`str` назва модуля.
            :exception ExtensionFailed: :class:`disnake.ExtensionFailed` якщо при завантаженні модуля виникла помилка.
            :exception NoEntryPointError: :class:`disnake.NoEntryPointError` якщо у модулі немає функції setup().
            :exception ExtensionNotFound: :class:`disnake.ExtensionNotFound` якщо модуль за вказанною назвою не знайдено.
            """
            try: self.mainclass.main_bot.load_extension(f"modules.{name}")
            except disnake.ext.commands.ExtensionFailed as error:

                try: module = self._modules[name]
                except KeyError:
                    module = self.Module(file=f"modules/{name}", name=name)
                    self._modules.update({name: module})

                module.set_status(self.StatusCrashed)
                module.set_error(error)
                module.set_tracebacck(traceback.format_exc())

                raise error

            module = self._modules[name]

            module.set_status(self.StatusOK)

SurvivalBoomDiscordService = Main()
SurvivalBoomDiscordService.start()
