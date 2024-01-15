import traceback

import disnake
from disnake.ext import commands
from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")

class _InformationModuleCog(commands.Cog):

    def __init__(self):
        self.custom_buttons: dict[str, str] = {}

    def cog_load(self) -> None:

        try:

            embeds = _settings.get("embeds")

            for embed in embeds:
                try:
                    for button in embeds[embed]['buttons']: self.custom_buttons.update({button['name']: button['send-embed']})
                except KeyError: continue

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, place="cog_load()", module=this_module_name)
            SBDS.modules.unloadModule(name=this_module_name, tracebackk=tracebackk, error=error, crashed=True)

    @commands.slash_command(name="send-information-message", description="Відправити головне інформаційне повідомлення у поточний канал.")
    async def send_command(ctx: disnake.ApplicationCommandInteraction):

        try:

            if not SBDS.utils.checkUserRoles(roles_id_list=_settings.get("allowed-roles-ids"), member=ctx.user):
                await ctx.send(":no_entry:", ephemeral=True, delete_after=10)
                return

            channel = ctx.channel
            main_embed_name = _settings.get("main-embed-name")

            embed = SBDS.utils.buildEmbed(path_to_embed=f"modules.information-module.embeds.{main_embed_name}")
            buttons = SBDS.utils.buttonsBuilder(path_to_embed=f"modules.information-module.embeds.{main_embed_name}.buttons")

            await channel.send(embed=embed, view=buttons)
            await ctx.send(":white_check_mark:", ephemeral=True, delete_after=True)

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="commands.send_command()")
            SBDS.utils.sendErrorToUser(error=error, ctx=ctx, ephemeral=True, edit=False)
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="commands.send_command()")

    @staticmethod
    @SBDS.main_bot.listen("on_button_click")
    async def button_clicked(inter: disnake.MessageInteraction):

        try:

            custom_id = inter.data['custom_id']

            if not custom_id in _cog.custom_buttons: return

            buttons = None
            embed_name = _cog.custom_buttons[custom_id]
            embed = SBDS.utils.buildEmbed(f"modules.information-module.embeds.{embed_name}")
            if "buttons" in _settings.get(f"embeds.{embed_name}"): buttons = SBDS.utils.buttonsBuilder(path_to_embed=f"modules.information-module.embeds.{embed_name}")

            if buttons is None: await inter.send(embed=embed, ephemeral=True, delete_after=_settings.get("delete-ephemeral-after"))
            else: await inter.send(embed=embed, view=buttons, ephemeral=True, delete_after=_settings.get("delete-ephemeral-after"))

        except Exception as error:

            tracebackk = traceback.format_exc()
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=tracebackk, module=this_module_name, place="events.button_clicked()")
            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="events.button_clicked()")
            SBDS.utils.sendErrorToUser(error=error, ctx=inter, ephemeral=True, edit=False)





_settings: SBDS.settings.SettingsSection = ...
_cog = _InformationModuleCog()

def setup(bot: commands.InteractionBot):

    global _settings
    _settings = SBDS.settings.createSection("modules.information-module")
    _settings.checkKeys(check_keys={"main-embed-name": str, "embeds": dict, "allowed-roles-ids": list, "delete-ephemeral-after": int})

    bot.add_cog(_cog)