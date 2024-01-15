#    _____                  _             ______
#   / ___/__  ________   __(_)   ______ _/ / __ )____  ____  ____ ___
#   \__ \/ / / / ___/ | / / / | / / __ `/ / __  / __ \/ __ \/ __ `__ \
#  ___/ / /_/ / /   | |/ / /| |/ / /_/ / / /_/ / /_/ / /_/ / / / / / /
# /____/\__,_/_/    |___/_/ |___/\__,_/_/_____/\____/\____/_/ /_/ /_/
# SurvivalBoom Network 2023 | SurvivalBoom Discord Service
#   Console Commands Module | By TIMURishche
#
#



import inspect
import traceback
import aioconsole
import asyncio
from main import SurvivalBoomDiscordService as SBDS
from disnake.ext import commands


this_module_name = f"{__name__}".removeprefix("modules.")

# Класс кожної консольної команди.
class ConsoleCommand:
    """
    Об'єкт консольної команди.
    Зберігає у собі усю інформацію про консольну команду.

    Більше мені нічого тут написати -- TIMURishche
    """

    def __init__(self, name: str, func: callable, description: str, arguments: str):
        self.name = name
        self.func = func
        self.description = description
        self.arguments = arguments

# Вбудовані у модуль консольних команд команди.
class _DefaultCommands:

    @staticmethod
    def modules_command(args: list[str]):

        # Якщо немає аргументів відправляємо допомогу по менеджеру модулів.
        if len(args) < 2:
            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("---       Менеджер модулів        ---")
            SBDS.mainlogger.info(f"&emodules list &r- &3Відображає список усіх модулів і їх статус.")
            SBDS.mainlogger.info(f"&emodules info [Назва модуля] &r- &3Відображає детальні дані про вказаний модуль.")
            SBDS.mainlogger.info(f"&emodules unload [Назва модуля] &r- &3Відвантажує вказаний модуль.")
            SBDS.mainlogger.info(f"&emodules load [Назва модуля] &r- &3Завантажує вказаний модуль.")
            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")

            return

        # /modules list
        if args[1] == "list":

            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("--- Модулі SurvivalBoom Discord Service ---")

            # Виведення усіх модулів які завантажені у боті (ті які завантажені 100% працюють)
            for module in SBDS.modules.loaded_modules: SBDS.mainlogger.info(f"&e{module.name} &r(&5{module.file}&r) -- [ &aOK&r ]")

            # Виведення модулів які є у папці /modules, але не завантажились.
            for module in SBDS.modules.unloaded_modules: SBDS.mainlogger.info(f"&e{module.name} &r(&5{module.file}&r) -- [ &4{module.status}&r ]")

            # Виведення підказки про команду modules info.
            SBDS.mainlogger.info("Щоб дізнатись інформацію про модуль напишіть \"modules info <Модуль>\".")

            SBDS.mainlogger.info("")

        # /modules info
        elif args[1] == "info":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: modules info <Модуль>")
                return

            try:
                module = SBDS.modules.getModule(args[2])
            except SBDS.modules.ModuleNotFound:
                SBDS.mainlogger.info(f"Модуль {args[2]} не знайдено.")
                return

            SBDS.mainlogger.info("")
            SBDS.mainlogger.info(f"--- Інформація про модуль {module.name} ---")
            SBDS.mainlogger.info(f"Назва: &e{module.name}")
            SBDS.mainlogger.info(f"Файл: &e{module.file}")
            if module.status == SBDS.modules.StatusOK:
                SBDS.mainlogger.info(f"Статус: [ &a{module.status}&r ]")
            else:
                SBDS.mainlogger.info(f"Статус: [ &4{module.status}&r ]")
            if module.status != SBDS.modules.StatusOK: SBDS.mainlogger.info(f"Помилка: &5{module.error}&r")
            if module.status != SBDS.modules.StatusOK: SBDS.mainlogger.info(f"Stacktrace: &c{module.traceback}&r")
            SBDS.mainlogger.info("")

        elif args[1] == "reload":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: modules load <Модуль>")
                return

            # Захист від дебіла. Якщо користувач намагається відвантажити модуль який відповідає за консольні команди
            # вивести попередження про це і запитати користувача чи дійсно він хоче це зробити.
            if args[2] == this_module_name:

                SBDS.mainlogger.info("")
                SBDS.mainlogger.info("Ви впевнені що хочете відвантажити модуль який відповідає за консольні команди Discord Service?")
                SBDS.mainlogger.info("Це призведе до того що Discord Service перестане оброблювати консольні команди.")
                SBDS.mainlogger.info("Продовжити? [yes/no]")
                anser = input("")

                # Якщо користувач не погодився продовжити, завершити виконання команди.
                if anser != "yes":
                    SBDS.mainlogger.info("Скасовано!")
                    return

            try:
                SBDS.modules.unloadModule(name=args[2])  # Відвантажуємо модуль.

            # Якщо вказаний модуль не завантажений, вивести про це повідомлення.
            except commands.ExtensionNotLoaded:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне завантажений.")
                return

            # Якщо вказаний модуль не знайдений, вивести про це повідомлення.
            except commands.ExtensionNotFound:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне знайдено.")
                return

            SBDS.mainlogger.info(f"Модуль &3{args[2]} &rуспішно вивантажено.")  # Відправлення повідомлення про успіх.


            try:
                SBDS.modules.loadModule(args[2])

            except commands.ExtensionNotFound:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне знайдено.")
                return

            except commands.ExtensionFailed:
                SBDS.mainlogger.info(f"При завантаженні модуля &3{args[2]} &rвиникла невідома помилка.")
                SBDS.mainlogger.info(f"Напишіть &5modules info {args[2]} &rщоб дізнатись помилку.")
                return

            SBDS.mainlogger.info(f"Модуль &3{args[2]} &rуспішно завантажено.")

        # /modules unload
        elif args[1] == "unload":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: modules unload <Модуль>")
                return

            # Захист від дебіла. Якщо користувач намагається відвантажити модуль який відповідає за консольні команди
            # вивести попередження про це і запитати користувача чи дійсно він хоче це зробити.
            if args[2] == this_module_name:

                SBDS.mainlogger.info("")
                SBDS.mainlogger.info("Ви впевнені що хочете відвантажити модуль який відповідає за консольні команди Discord Service?")
                SBDS.mainlogger.info("Це призведе до того що Discord Service перестане оброблювати консольні команди.")
                SBDS.mainlogger.info("Продовжити? [yes/no]")
                anser = input("")

                # Якщо користувач не погодився продовжити, завершити виконання команди.
                if anser != "yes":
                    SBDS.mainlogger.info("Скасовано!")
                    return

            try:
                SBDS.modules.unloadModule(name=args[2])  # Відвантажуємо модуль

            # Якщо вказаний модуль не завантажений, вивести про це повідомлення.
            except commands.ExtensionNotLoaded:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне завантажений.")
                return

            # Якщо вказаний модуль не знайдений, вивести про це повідомлення.
            except commands.ExtensionNotFound:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне знайдено.")
                return

            SBDS.mainlogger.info(f"Модуль &3{args[2]} &rуспішно вивантажено.")  # Відправлення повідомлення про успіх.

        # /modules load
        elif args[1] == "load":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: modules load <Модуль>")
                return

            try:
                SBDS.modules.loadModule(args[2])

            except commands.ExtensionAlreadyLoaded:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rвже завантажений.")
                return

            except commands.ExtensionNotFound:
                SBDS.mainlogger.info(f"Модуль &3{args[2]} &rне знайдено.")
                return

            except commands.ExtensionFailed:
                SBDS.mainlogger.info(f"При завантаженні модуля &3{args[2]} &rвиникла невідома помилка.")
                SBDS.mainlogger.info(f"Напишіть &5modules info {args[2]} &rщоб дізнатись помилку.")
                return

            SBDS.mainlogger.info(f"Модуль &3{args[2]} &rуспішно завантажено.")

        # Якщо ніякий аргумент не спрацював, вивести довідку по командах.
        else:

            # Відправляємо із допомогою по менеджеру модулів.
            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("---       Менеджер модулів        ---")
            SBDS.mainlogger.info(f"&emodules list &r- &3Відображає список усіх модулів і їх статус.")
            SBDS.mainlogger.info(f"&emodules info [Назва модуля] &r- &3Відображає детальні дані про вказаний модуль.")
            SBDS.mainlogger.info(f"&emodules unload [Назва модуля] &r- &3Відвантажує вказаний модуль.")
            SBDS.mainlogger.info(f"&emodules load [Назва модуля] &r- &3Завантажує вказаний модуль.")
            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")

    @staticmethod
    def stop_command():
        asyncio.create_task(SBDS.stop(), name="StopAll")

    @staticmethod
    def help_command():

        SBDS.mainlogger.info("")
        SBDS.mainlogger.info("--- Команди SurvivalBoom Discord Service ---")

        for command in _cog.registered_commands:
            command = _cog.registered_commands[command]
            SBDS.mainlogger.info(f"&e{command.name} {str(command.arguments).replace('None', '')} &r- &3{command.description}.")

        SBDS.mainlogger.info("---                                      ---")

    @staticmethod
    def tasks_command():

        SBDS.mainlogger.info("")
        SBDS.mainlogger.info("--- Менеджер тасків ---")
        for task in asyncio.all_tasks(): SBDS.mainlogger.info(task.get_name())
        SBDS.mainlogger.info("---                 ---")
        SBDS.mainlogger.info("")

    @staticmethod
    def userdata_command(args: list[str]):

        if len(args) < 3:

            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("---  Менеджер даних користувачів  ---")
            SBDS.mainlogger.info(f"&euserdata get <ID користувача> [Ключ] [Шлях] &r- &3Відображає список усіх модулів і їх статус.")
            SBDS.mainlogger.info(f"&euserdata set <ID користувача> <Ключ> <Значення> &r- &3Встановлює вказаному ключу вказане значення.")
            SBDS.mainlogger.info(f"&euserdata add <ID користувача> <Ключ> &r- &3Додає ключ до інформації користувача.")
            SBDS.mainlogger.info(f"&euserdata del <ID користувача> <Ключ> &r- &3Видаляє ключ з інформації користувача.")
            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")

            return

        if args[1] == "get":

            if len(args) < 3:
                SBDS.mainlogger.info("Використання: userdata get <ID користувача> [Ключ] [Шлях]")
                return

            key = None
            path = None

            if len(args) > 3: key = args[3]
            if len(args) > 4: path = args[4]

            SBDS.mainlogger.info("Здійснюємо запит до бази даних...")

            try:

                result = SBDS.database.getUserData(user_id=args[2], key=key, path=path)

                SBDS.mainlogger.info(f"Дані користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&r.")
                SBDS.mainlogger.info(f"&2{result}")

            except SBDS.database.KeyNotFound as error:
                SBDS.mainlogger.info(f"{error}")

            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача із ID &b{args[2]} &rне знайдено у базі даних.")

        elif args[1] == "set":

            if len(args) < 5:
                SBDS.mainlogger.info("Використання: userdata set <ID користувача> <Ключ> <Значення>")
                return

            SBDS.mainlogger.info("Здійснюємо запит до бази даних...")

            try:
                SBDS.database.setUserKey(user_id=args[2], key=args[3], value=args[4])
                SBDS.mainlogger.info(f"&aУспішно змінено значення ключа &b{args[3]} &aна &b{args[4]} &aдля користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&a.")

            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача із ID &b{args[2]} &rне знайдено у базі даних.")

            except SBDS.database.KeyNotFound:
                SBDS.mainlogger.info(f"Ключ &b{args[3]} &rне знайдено у даних користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&r.")

        elif args[1] == "add":

            if len(args) < 4:
                SBDS.mainlogger.info("Використання: userdata add <ID користувача> [Назва ключа]")
                return

            SBDS.mainlogger.info("Здійснюємо запит до бази даних...")

            try:

                SBDS.database.addUserKey(user_id=args[2], key=args[3])
                SBDS.mainlogger.info(f"&aУспішно додано ключ &b{args[3]} &aдля користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&a.")

            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача із ID &b{args[2]} &rне знайдено у базі даних.")

            except SBDS.database.KeyAlreadyExist:
                SBDS.mainlogger.info(f"Ключ &b{args[3]} &rвже існує у даних користувача &b{SBDS.main_bot.get_user(int(args[2])).name}.")

        elif args[1] == "del":

            if len(args) < 4:
                SBDS.mainlogger.info("Використання: userdata del <ID користувача> [Назва ключа]")
                return

            SBDS.mainlogger.info("Здійснюємо запит до бази даних...")

            try:
                SBDS.database.delUserKey(user_id=args[2], key=args[3])
                SBDS.mainlogger.info(f"&aУспішно видалено ключ &b{args[3]} &aдля користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&a.")

            except SBDS.database.NoResultFromDatabase:
                SBDS.mainlogger.info(f"Користувача із ID &b{args[2]} &rне знайдено у базі даних.")

            except SBDS.database.KeyNotFound:
                SBDS.mainlogger.info(f"Ключ &b{args[3]} &rне знайдено у даних користувача &b{SBDS.main_bot.get_user(int(args[2])).name}&r.")

        else:

            SBDS.mainlogger.info("")
            SBDS.mainlogger.info("---  Менеджер даних користувачів  ---")
            SBDS.mainlogger.info(f"&euserdata get <ID користувача> [Ключ] [Шлях] &r- &3Відображає список усіх модулів і їх статус.")
            SBDS.mainlogger.info(f"&euserdata set <ID користувача> <Ключ> <Значення> &r- &3Встановлює вказаному ключу вказане значення.")
            SBDS.mainlogger.info(f"&euserdata add <ID користувача> <Ключ> &r- &3Додає ключ до інформації користувача.")
            SBDS.mainlogger.info(f"&euserdata del <ID користувача> <Ключ> &r- &3Видаляє ключ з інформації користувача.")
            SBDS.mainlogger.info("---                               ---")
            SBDS.mainlogger.info("")

    @staticmethod
    def reloadsettings_command():

        SBDS.mainlogger.loading("Перезавантажуємо Settings.yml")
        SBDS.settings.reloadSettings()
        SBDS.mainlogger.completed("Settings.yml успішно перезавантажено!")
        SBDS.mainlogger.info("Але щоб деякі зміни застосувались потрібно перезавантажити модулі.")

    @staticmethod
    def botsay_command(args: list[str]):

        if len(args) < 3: return

        channel = SBDS.main_bot.get_channel(int(args[1]))

        asyncio.create_task(channel.send(content=args[2].replace("_", " ")), name=f"{this_module_name} - botsay_command()")

# Disnake модуль консольних команд.
class _ConsoleHandlerCog(commands.Cog):

    def __init__(self):

        self.console_listener_task: asyncio.Task = ... # Таск прослуховувача консольних команд.
        self.registered_commands: dict[str, ConsoleCommand] = {} # Список зареєстрованих команд.
        self.logger = SBDS.mainlogger.createModuleLogger(this_module_name) # Логгер.

    def cog_load(self):

        # Запускаємо таск прослуховування консольних команд
        self.console_listener_task = asyncio.create_task(self.console_listener(), name=f"{this_module_name} - ConsoleListener")

        # Реєструємо стандартні консольні команди із цього модуля.
        registerCommand(name="help", command_executor=_DefaultCommands.help_command, description="Виводить усі доступні команди")
        registerCommand(name="stop", command_executor=_DefaultCommands.stop_command, description="Відключає усі модулі, й вимикає SurvivalBoomDiscordService")
        registerCommand(name="reloadsettings", command_executor=_DefaultCommands.reloadsettings_command, description="Перезавантажує settings.yml")
        registerCommand(name="tasks", command_executor=_DefaultCommands.tasks_command, description="Виводить список усіх тасків")
        registerCommand(name="modules", command_executor=_DefaultCommands.modules_command, description="Менеджер модулів. Основний інструмент для роботи й керування модулями", arguments="[help/list/info/load/reload/unload] [Модуль]")
        registerCommand(name="userdata", command_executor=_DefaultCommands.userdata_command, description="Менеджер даних користувачів. Основний інструмент для роботи з інформацією користувачів у базі даних", arguments="[get/set/del] [ID користувача] [Ключ]")
        registerCommand(name="botsay", command_executor=_DefaultCommands.botsay_command, description="Undefined", arguments="Undefined")

    def cog_unload(self):

        # Вимикаємо прослуховування консолі.
        self.console_listener_task.cancel()
        # Очищаємо список зареєстрованих команд.
        self.registered_commands.clear()

    @staticmethod # Повідомлення про команду help
    @SBDS.main_bot.listen("on_ready")
    async def started():

        await asyncio.sleep(1)

        if not SBDS.modules.unloaded_modules: SBDS.mainlogger.info("Напишіть \"help\" у консоль щоб подивитись усі доступні команди.")
        else:
            SBDS.mainlogger.warn("Деякі модулі SurvivalBoom Discord Service не були завантажені.")
            SBDS.mainlogger.warn("Напишіть \"modules list\" щоб подивитись статус усіх модулів.")

    # Прослуховувач консолі.
    async def console_listener(self):

        while True:

            # Отримуємо консольну команду.
            console_command: str = await aioconsole.ainput("")

            # Отримуємо агрументи команди.
            args = console_command.split(" ")

            # Перевіряємо чи є отримана команда у списку зареєстрованих команд.
            if not args[0] in self.registered_commands:
                SBDS.mainlogger.info("Команду не знайдено. Напишіть \"help\" щоб отримати список доступних команд.")
                continue

            # Знаходимо відповідну зраеєстровану команду.
            command = self.registered_commands[args[0]]

            # Виконуємо команду.
            try:
                if 'args' in inspect.signature(command.func).parameters: command.func(args) # Якщо команда приймає аргументи, надати у парметрах функції аргументи.
                else: command.func() # Якщо ж не приймає, просто виконати функцію прив'язану до команди.

            # Якщо виник якийсь пиздець і помилки при виконанні команди, відправити повідомлення про помилку.
            except Exception as error:
                self.logger.error(f"Помилка при виконанні команди &5\"{console_command}\"&4: &3{error}.")
                if SBDS.tracebackAllowed: self.logger.error(traceback.format_exc())

# Помилка 'команда вже зареєстрована'.
class CommandAlreadyRegistered(Exception):
    """
    Помилка 'Ця команда вже зареєстрована'.
    Виникає тоді коли ви намагаєтесь зареєструвати команду яка вже зареєстрована.
    """
    def __init__(self, command: str): super().__init__(f"Команда {command} вже зареєстрована!")

# Помилка 'команда не знайдена'.
class CommandNotFound(Exception):
    """
    Помилка 'Команду не знайдено'.
    Виникає тоді коли ви намагаєтесь скасувати реєстрацію команди, але цієї команди не існує.
    """
    def __init__(self, command: str): super().__init__(f"Команду {command} не знайдено.")

# Функція реєстрації нової команди.
def registerCommand(name: str, command_executor: callable, description: str = None, arguments: str = None):
    """
    Зареєструвати команду. За допомогою цього методу ви можете зареєструвати команду й функцію яка буде виконуватись при виклику цієї команди.

    :param name: Назва команди вашої команди.
    :param command_executor: Функція яка буде викликатись при виконанні цієї команди.
    :param description: Опис вашої команди. Буде відображатись у help.
    :param arguments: Агрументи вашої команди. Буде відображатись у help. Можете залишити пустим якщо у вашої команди немає аргументів.

    :exception CommandAlreadyRegistered: :class:`CommandAlreadyRegistered` Виникає якщо команда із такою назвою вже зареєстрована.
    """

    # Якщо команда вже зареєстрована, видати помилку.
    if name in _cog.registered_commands: raise CommandAlreadyRegistered(name)

    # Реєструємо команду.
    command_obj = ConsoleCommand(name=name, func=command_executor, description=description, arguments=arguments)
    _cog.registered_commands.update({name: command_obj})

# Функція скасування реєстрації команди.
def unregisterCommand(name: str):
    """
    Скасовує реєстрацію команди, повністю видаляючи її із зареєстрованих команд.
    :param name: Назва команди.
    :exception CommandNotFound: :class:`CommandNotFound` Виникає якщо команду із такою назвою не знайдено у списку зареєстрованих команд.
    """

    # Якщо команду не знайдено, видаємо помилку.
    if not name in _cog.registered_commands: raise CommandNotFound(name)

    # Скасовуємо реєстрацію команди.
    _cog.registered_commands.pop(name)



_cog = _ConsoleHandlerCog() # КостилЬ!!!!!!!!! ФУУУ!!

def setup(self: commands.AutoShardedInteractionBot):
    self.add_cog(_cog)