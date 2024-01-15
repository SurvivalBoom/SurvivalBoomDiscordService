import asyncio
import disnake
import random
import traceback

from main import SurvivalBoomDiscordService as SBDS
from disnake.ext import commands, tasks


this_module_name = f"{__name__}".removeprefix("modules.")

class ChangeStatusCog(commands.Cog):

    def __init__(self):
        self.logger = ...

    # Дії при завантаженні модуля.
    def cog_load(self):
        self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
        self.change_status_task.change_interval(seconds=SBDS.settings.get("modules.change-status-module.bot-status-changing-interval"))
        self.change_status_task.start()

    # Дії при вивантаженні модуля.
    def cog_unload(self):
        self.change_status_task.stop()  # Вимикаємо таск зміни статуса.

    @tasks.loop(hours=999) # Таск встановлення випадкового статусу бота.
    async def change_status_task(self):
        try:
            await SBDS.main_bot.wait_until_ready()
            await asyncio.sleep(5)
            status, activity_type, activity_text = setRandomStatus()
            self.logger.info(f"Встановлено статус головного бота {status}: {activity_type} {activity_text}.")

        except Exception as error:
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="change_status_task()")

def setRandomStatus() -> tuple[disnake.Status, disnake.ActivityType, str]:

    statuses = SBDS.settings.get("modules.change-status-module.bot-statuses")

    # Перевіряємо скільки статусів у боті
    if len(statuses) == 1: status_number = 0
    else: status_number = int(random.randint(0, len(statuses) - 1))  # Вибираємо випадковий статус зі списку.

    # Отримаємо дані від статусу.
    activity_text = statuses[status_number]['text']
    activity_type = statuses[status_number]['type']
    status = statuses[status_number]['status']

    activity_type_obj = None
    status_obj = None

    # Трансформуємо назву активностей в об'єкти.
    if activity_type == "PLAYING": activity_type_obj = disnake.ActivityType.playing
    elif activity_type == "WATCHING": activity_type_obj = disnake.ActivityType.watching
    elif activity_type == "STREAMING": activity_type_obj = disnake.ActivityType.streaming
    elif activity_type == "LISTENING": activity_type_obj = disnake.ActivityType.listening

    # Трансформуємо назву статусів в об'єкти.
    if status == "IDLE": status_obj = disnake.Status.idle
    elif status == "DND": status_obj = disnake.Status.dnd
    elif status == "INVISIBLE": status_obj = disnake.Status.idle
    elif status == "STREAMING": status_obj = disnake.Status.streaming

    # Ставимо статус.
    asyncio.create_task(SBDS.main_bot.change_presence(activity=disnake.Activity(name=activity_text, type=activity_type_obj), status=status_obj), name="")

    return status, activity_type, activity_text


def setup(self: commands.AutoShardedInteractionBot):
    SBDS.settings.checkKeys(check_keys={"bot-statuses": list, "bot-status-changing-interval": int}, path="modules.change-status-module")
    self.add_cog(ChangeStatusCog())
