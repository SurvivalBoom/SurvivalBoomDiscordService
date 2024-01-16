#     ____        ___ __ _       __           
#    / __ \____  / (_) /| |     / /___ ___  __
#   / /_/ / __ \/ / / __/ | /| / / __ `/ / / /
#  / ____/ /_/ / / / /_ | |/ |/ / /_/ / /_/ / 
# /_/    \____/_/_/\__/ |__/|__/\__,_/\__, /  
#                                    /____/   
# PolitWay Network 2023 | PolitWay Discord Service
#              Ticket Module | By stefanbanderovych
import asyncio
import time
import traceback
import aiohttp
import disnake
from disnake.ext import commands, tasks

from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")

class closes(disnake.ui.View):
	def __init__(self) -> None:
		super().__init__(timeout = None)

	@disnake.ui.button(label = 'Закрити', emoji="🔒", style = disnake.ButtonStyle.red, custom_id = 'ticket_close')
	async def close(self, button: disnake.ui.Button, interaction: disnake.Interaction):
		await interaction.response.send_message(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.CONFIRM-CLOSE"), view=confirm(), ephemeral=True)

class confirm(disnake.ui.View):
    def __init__(self) -> None:
        super().__init__(timeout=None)

    @disnake.ui.button(label='Підтвердити', style=disnake.ButtonStyle.green, custom_id='ticket_confirm_close')
    async def confirm_button_test(self, button: disnake.ui.Button, interaction: disnake.Interaction):
        try:
            await interaction.response.send_message(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKET-DELETED"))
            await asyncio.sleep(3)
            await interaction.channel.delete()
            _cog.logger.info(f"Користувач &b{interaction.user.name} &rзакрив тікет.")

        except Exception as error:

            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="buttons.ticket_create()")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="buttons.ticket_create()")
            SBDS.utils.sendErrorToUser(error=error, edit=False)
            SBDS.modules.unloadModule(name=this_module_name, tracebackk=traceback.format_exc(), error=error, crashed=True)


class TicketModule(commands.Cog):
    def __init__(self):
        self.bot = bot


    def __init__(self):
        self.persistent_views_added = False
        self.logger: SBDS.mainlogger.ModuleLogger = ...

    def cog_load(self) -> None:
            self.ticket_channel = None
            self.ticket_channel_message = None

            self.logger = SBDS.mainlogger.createModuleLogger(this_module_name)
            self.module_settings = SBDS.settings.createSection("modules.ticket-module")

            self.add_enabled = self.module_settings.get("commands.add.enabled")
            self.add_allowed_roles_ids = self.module_settings.get("roles-ticket-mod")
            self.add_self_block = self.module_settings.get("commands.add.block-self-use")
            self.add_stuff_block = self.module_settings.get("commands.add.block-stuff-use")

    # Метод генерування dropdown списків під повідомленням контрольної панелі.
    def _generate_actions_dropdown(self) -> disnake.ui.View:

        try:

            items = disnake.ui.View() # Ствоюємо об'єкт кнопочок.

            help_label = self.module_settings.get("tickets-create-panel.dropdowns.Help.title")
            help_description = self.module_settings.get("tickets-create-panel.dropdowns.Help.description") 
            help_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Help.emoji") 

            nation_label = self.module_settings.get("tickets-create-panel.dropdowns.Nation.title")
            nation_description = self.module_settings.get("tickets-create-panel.dropdowns.Nation.description") 
            nation_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Nation.emoji") 

            structure_label = self.module_settings.get("tickets-create-panel.dropdowns.Structure.title")
            structure_description = self.module_settings.get("tickets-create-panel.dropdowns.Structure.description") 
            structure_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Structure.emoji") 

            gazeta_label = self.module_settings.get("tickets-create-panel.dropdowns.Gazeta.title")
            gazeta_description = self.module_settings.get("tickets-create-panel.dropdowns.Gazeta.description") 
            gazeta_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Gazeta.emoji") 

            war_label = self.module_settings.get("tickets-create-panel.dropdowns.War.title")
            war_description = self.module_settings.get("tickets-create-panel.dropdowns.War.description") 
            war_emoji = self.module_settings.get("tickets-create-panel.dropdowns.War.emoji") 

            revolt_label = self.module_settings.get("tickets-create-panel.dropdowns.Revolt.title")
            revolt_description = self.module_settings.get("tickets-create-panel.dropdowns.Revolt.description") 
            revolt_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Revolt.emoji") 

            report_label = self.module_settings.get("tickets-create-panel.dropdowns.Report.title")
            report_description = self.module_settings.get("tickets-create-panel.dropdowns.Report.description") 
            report_emoji = self.module_settings.get("tickets-create-panel.dropdowns.Report.emoji") 

            tickets_create = [
                disnake.SelectOption(label=help_label, description=help_description, emoji=help_emoji, value="Help"),
                disnake.SelectOption(label=nation_label, description=nation_description, emoji=nation_emoji, value="Nation"),
                disnake.SelectOption(label=structure_label, description=structure_description, emoji=structure_emoji, value="Structure"),
                disnake.SelectOption(label=gazeta_label, description=gazeta_description, emoji=gazeta_emoji, value="Gazeta"),
                disnake.SelectOption(label=war_label, description=war_description, emoji=war_emoji, value="War"),
                disnake.SelectOption(label=revolt_label, description=revolt_description, emoji=revolt_emoji, value="Revolt"),
                disnake.SelectOption(label=report_label, description=report_description, emoji=report_emoji, value="Report")
            ]

            items.add_item(disnake.ui.StringSelect(min_values=1, max_values=1, options=tickets_create, custom_id="PolitWayTickets_Create"))

            return items

        except Exception as error:

            SBDS.utils.sendErrorToDebugChannel(file_name=f"{__name__}".replace(".", "/"), error=error, place="generate.dropdown()")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), place="generate.dropdown()", module=this_module_name)

    # Метод оновлення контрольної панелі.
    # Банально: Генерує embed повідомлення і кнопки під ним. Потім змінює що існує повідомлення.
    def update_control_panel(self):
        asyncio.create_task(self._control_panel_msg.edit(embed=self._generate_control_panel(), view=self._generate_actions_dropdown())).set_name(f"{module} - Update Control Panel")

    @staticmethod # Обробка дій про натисканні кнопок.
    @SBDS.main_bot.listen("on_dropdown")
    async def control_panel(inter: disnake.MessageInteraction):

        try:

            value = inter.data['values'][0]

            if not inter.data['custom_id'] == "PolitWayTickets_Create": return

            if value == "Help":

                channel_name = f"тікет-{inter.user.name}-help"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))

                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="❕ Питання та допомога")
                embed.description = '> **1. Ваш нікнейм у грі**\n> **2. Ваше запитання**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Питання та допомога", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "Nation":

                channel_name = f"тікет-{inter.user.name}-nation"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))
            
                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                await channel.set_permissions(inter.guild.default_role, view_channel=False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="👑 Реєстрація країн")
                embed.description = '> **1. Назва країни**\n> **2. Форма правління**\n> **3. Столиця**\n> **4. Нікнейм лідера**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Реєстрація країн", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "Structure":

                channel_name = f"тікет-{inter.user.name}-structure"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))
            
                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="🕍 Реєстрація будівель")
                embed.description = '> **1. Назва будівлі**\n> **2. Місто**\n> **3. Фото будівлі**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Реєстрація будівель", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "Gazeta":

                channel_name = f"тікет-{inter.user.name}-gazeta"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))
            
                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="📰 Реєстрація газети")
                embed.description = '> **1. Дата публікації**\n> **2. Текст публікації**\n> **3. Фотографія до публікації**\n> **4. Докази що ви лідер міста/нації**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Реєстрація газети", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "War":

                channel_name = f"тікет-{inter.user.name}-war"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))
            
                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="🔥 Реєстрація війни")
                embed.description = '> **1. Ваша держава**\n> **2. Держава якій ви оголошуєте війну **\n> **3. Причина війни**\n> **4. Докази що ви лідер країни**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())


                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Реєстрація війни", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "Revolt":

                channel_name = f"тікет-{inter.user.name}-revolt"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))

                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="✊ Реєстрація повстання")
                embed.description = '> **1. Ваша організація**\n> **2. Держава у якій буде повстання**\n> **3. Причина повстання**\n> **4. Ваш нікнейм**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Реєстрація повстання", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

            elif value == "Report":

                channel_name = f"тікет-{inter.user.name}-report"
                guild = inter.guild
                category = guild.get_channel(SBDS.settings.get("modules.ticket-module.category-id"))
            
                channel = await guild.create_text_channel(channel_name, category=category)

                role_ids = SBDS.settings.get("modules.ticket-module.roles-ticket-mod")
                ticket_mod_roles = [inter.guild.get_role(role_id) for role_id in role_ids]

                for role in ticket_mod_roles:

                    await channel.set_permissions(role, view_channel=True, send_messages=True, read_messages=True)

                await channel.set_permissions(inter.guild.default_role, view_channel = False)

                await channel.set_permissions(inter.user, read_messages=True, send_messages=True)

                await inter.send(f'✔ Тікет створено {channel.mention}', ephemeral = True)

                embed = disnake.Embed(color=0x2F3136)
                embed.set_author(name="🤬 Подання скарги")
                embed.description = '> **1. Ваш нікнейм**\n> **2. Нікнейм порушника**\n> **3. Текст скарги**\n> **4. Докази**\n\nПерсонал незабаром зв’яжеться з вами.\nЩоб закрити цей тікет, натисніть на кнопку "🔒 Закрити"'
                embed.set_footer(text="Якщо кнопка 'Закрити' не працює, використовуйте /close")

                await channel.send(f"Привіт {inter.user.mention}!", embed=embed, view=closes())

                log_channel = SBDS.main_bot.get_channel(int(SBDS.settings.get("modules.ticket-module.log-channel-id")))

                await log_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.TICKED-CREATED", placehoders={"{{MEMBER_MENTION}}": f"{inter.user.mention}", "{{CHANNEL_MENTION}}": f"{channel.mention}", "{{TYPE_TICKET}}": "Подання скарги", "{{CHANNEL_NAME}}": f"{channel.name}"}))
                _cog.logger.info(f"Користувач &b{inter.user.name} &rвідкрив тікет.")

        except Exception as error:

            SBDS.utils.sendErrorToDebugChannel(file_name=f"{__name__}".replace(".", "/"), error=error, place="listeners.dropdown()")
            SBDS.utils.sendErrorToUser(error=error, ctx=inter, ephemeral=True, edit=False)
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), place="listeners.dropdown()", module=this_module_name)

    @commands.slash_command(name="add", description="Додати користувача, у тікет")
    async def add(self, ctx, 
                  member: disnake.Member = commands.Param(name="користувач", description="Користувач, якого буде додано у тікет")):
        
        try:

            if "тікет-" in ctx.channel.name:

                await ctx.send(embed=SBDS.utils.buildEmbed("modules.ticket-module.embeds.LOADING")) # Відправляємо повідомлення про обробку команди.

                # Перевіряємо чи увімкнена команда.
                if not self.add_enabled:

                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.ticket-module.embeds.COMMAND-DISABLED"))

                    return

                # Перевірка чи вибраний користувач це бот.
                if member.bot:

                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.ticket-module.embeds.CANNOT-USE-BOT"))

                    return

                # Перевіряємо чи хоче користувач додати сам себе.
                if str(ctx.user.id) == str(member.id) and self.add_self_block:

                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.ticket-module.embeds.CANNOT-USE-SELF"))

                    return

                if self.add_stuff_block and SBDS.utils.checkUserRoles(
                        roles_id_list=self.add_allowed_roles_ids, member=member):

                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed("modules.ticket-module.embeds.CANNOT-USE-STUFF"))

                    return

                    await ctx.channel.set_permissions(member, read_messages=True, send_messages=True)
                    await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.SUCCESSFULY-ADDED", placehoders={"{{MEMBER_MENTION}}": f"{member.mention}"}))
                else:
                    await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.ERROR-OUTSIDE-THE-TICKET"))

        except Exception as error:

            SBDS.utils.sendErrorToDebugChannel(error=error, file_name=f"{__name__}".replace(".", "/"), place="commands.add_command()")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=this_module_name, place="commands.add-command()")
            SBDS.utils.sendErrorToUser(error=error, ctx=ctx, ephemeral=True, edit=True)


    @commands.slash_command(name="remove", description="Видалити користувача з тікета")
    async def remove(self, ctx: disnake.ApplicationCommandInteraction, 
                     member: disnake.Member = commands.Param(name="користувач", description="Користувач, якого буде видалено")):
        if 'тікет-' in ctx.channel.name:

            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.LOADING"))

            if member.id == ctx.author.id:
                await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.ERROR-CANNOT-REMOVE-ME-FROM-TICKET"))
                return
                
            await ctx.channel.set_permissions(member, view_channel = False)
            await ctx.edit_original_response(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.SUCCESSFULY-REMOVED", placehoders={"{{MEMBER_MENTION}}": f"{member.mention}"}))
        else:
            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.ERROR-OUTSIDE-THE-TICKET"))

    @commands.slash_command(name="close", description="Закрити тікет")
    async def close(self, ctx):
        if 'тікет-' in ctx.channel.name:
            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.CONFIRM-CLOSE", placehoders={"{{GUILD_AVATAR}}": f"{ctx.guild.icon.url}"}), view=confirm())
        else:
            await ctx.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.ERROR-OUTSIDE-THE-TICKET"))

    @commands.Cog.listener()
    async def on_ready(self):

        try:

            if not SBDS.main_bot.is_ready(): return

            if self.ticket_channel is None:

                self.ticket_channel = SBDS.main_bot.get_channel(int(self.module_settings.get("ticket-channel-id")))

                await self.ticket_channel.purge()

            if self.ticket_channel_message is None: self.ticket_channel_message = await self.ticket_channel.send(embed=SBDS.utils.buildEmbed(path_to_embed="modules.ticket-module.embeds.MAIN-EMBED"), view=self._generate_actions_dropdown())

        except Exception as error:

            file_name = f"{__name__}".replace(".", "/")
            file_name2 = f"{__name__}".removeprefix("modules.")
            SBDS.utils.sendErrorToConsole(error=error, tracebackk=traceback.format_exc(), module=file_name2, place="ticket_message()")
            SBDS.utils.sendErrorToDebugChannel(error=error, place="ticket_message()", file_name=file_name)

            SBDS.modules.unloadModule(name=this_module_name, crashed=True, error=error, tracebackk=traceback.format_exc())

_cog = TicketModule()


def setup(bot):
    SBDS.settings.checkKeys({"ticket-channel-id": int, "category-id": int, "log-channel-id": int}, path="modules.ticket-module")

    SBDS.settings.checkKeys(check_keys={"roles-ticket-mod": list}, path="modules.ticket-module")

    SBDS.settings.checkKeys(check_keys={"add": dict}, path="modules.ticket-module.commands")
    SBDS.settings.checkKeys(check_keys={"enabled": bool, "block-self-use": bool, "block-stuff-use": bool}, path="modules.ticket-module.commands.add")
    # Перевірка існування embed повідомлень модуля.
    SBDS.settings.checkKeys(check_keys={
        "ERROR-OUTSIDE-THE-TICKET": dict,
        "ERROR-CANNOT-REMOVE-ME-FROM-TICKET": dict,
        "SUCCESSFULY-ADDED": dict,
        "SUCCESSFULY-REMOVED": dict,
        "LOADING": dict,
        "COMMAND-DISABLED": dict,
        "CANNOT-USE-SELF": dict,
        "SUCCESSFULY-REMOVED": dict,
        "CANNOT-USE-BOT": dict,
        "CANNOT-USE-STUFF": dict,
        "CONFIRM-CLOSE": dict,
        "TICKET-DELETED": dict,
        "MAIN-EMBED": dict,
        "TICKED-CREATED": dict
    }, path="modules.ticket-module.embeds")
    SBDS.settings.checkKeys(check_keys={"tickets-create-panel": dict}, path="modules.ticket-module")
    SBDS.settings.checkKeys(check_keys={"dropdowns": dict}, path="modules.ticket-module.tickets-create-panel")
    SBDS.settings.checkKeys(check_keys={"Help": dict, "Nation": dict, "Structure": dict, "Gazeta": dict, "War": dict, "Revolt": dict, "Report": dict}, path="modules.ticket-module.tickets-create-panel.dropdowns")
    bot.add_cog(_cog)