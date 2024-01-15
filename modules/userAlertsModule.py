import disnake
from disnake.ext import commands
from main import SurvivalBoomDiscordService as SBDS

this_module_name = f"{__name__}".removeprefix("modules.")

class _UserAlertsModuleCog(commands.Cog):

    @staticmethod
    async def buildAndSendMessage(destination: disnake.TextChannel | disnake.User | disnake.Member, message: str | dict, placeholders = None):

        if placeholders is None: placeholders = {}

        if isinstance(message, dict):

            # Намагаємось отримати кольор embed'y.
            embed_color = 0x000000
            if "color" in message: embed_color = message['color']

            # Генерація базового embed повідомлення.
            embed = disnake.Embed(title=SBDS.utils.replacePlaceholders(text=message['title'], placeholders=placeholders), color=embed_color, description=SBDS.utils.replacePlaceholders(text=message['description'], placeholders=placeholders))

            # Намагаємось встановити thumbnail нашого embedу.
            if "thumbnail_url" in message: embed.set_thumbnail(url=message['thumbnail_url'])

            # Додаємо поля embed повідомлення.
            if "fields" in message:

                for field in message['fields']:
                    try:
                        if field['inline']: embed.add_field(name=SBDS.utils.replacePlaceholders(text=field['title'], placeholders=placeholders), value=SBDS.utils.replacePlaceholders(text=field['value'], placeholders=placeholders), inline=True)
                        else: embed.add_field(name=SBDS.utils.replacePlaceholders(text=field['title'], placeholders=placeholders), value=SBDS.utils.replacePlaceholders(text=field['value'], placeholders=placeholders), inline=False)
                    except:
                        embed.add_field(name=SBDS.utils.replacePlaceholders(text=field['title'], placeholders=placeholders), value=SBDS.utils.replacePlaceholders(text=field['value'], placeholders=placeholders), inline=False)

            # Додаємо футер до embed повідомлення.
            if "footer" in message:

                footer_image_url = None
                footer_text = None
                if "icon_url" in message['footer']: footer_image_url = message['footer']['icon_url']

                if "text" in message['footer']: footer_text = SBDS.utils.replacePlaceholders(text=message['footer']['text'], placeholders=placeholders)

                if footer_text is not None and footer_image_url is not None: embed.set_footer(text=footer_text, icon_url=footer_image_url)

                if footer_text is not None and footer_image_url is None: embed.set_footer(text=footer_text)

            destination = await destination.send(content="⠀")
            await destination.edit(content="", embed=embed)

        else:
            destination = await destination.send(content="⠀")
            await destination.edit(content=SBDS.utils.replacePlaceholders(message, placeholders=placeholders))

    @staticmethod
    def countLevelFromBoost(boost_count: int):
        if boost_count >= 14: return 3
        elif boost_count >= 7: return 2
        elif boost_count >= 2: return 1
        else: return 0

    def __init__(self):
        self.module_settings = SBDS.settings.createSection(path="modules.user-alerts-module")
        self.logger = SBDS.mainlogger.createModuleLogger(module_name=this_module_name)

    @staticmethod
    @SBDS.main_bot.listen("on_member_join")
    async def member_joined(member: disnake.Member):

        if member.bot: return

        _cog.logger.info(f"Користувач &b{member.name} &rприєднався до сервера.")

        channels: list[dict[str, str | int | dict]] = _cog.module_settings.get("member-join.guild-messages")
        for channel_info in channels:

            channel = SBDS.main_bot.get_channel(channel_info["channel-id"])
            message = channel_info['message']
            await _cog.buildAndSendMessage(destination=channel, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)

        message = _cog.module_settings.get("member-join.dm-message")
        if message is None or "": return
        try: await _cog.buildAndSendMessage(destination=member, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)
        except disnake.Forbidden: pass

    @staticmethod
    @SBDS.main_bot.listen("on_member_remove")
    async def member_leave(member: disnake.Member):

        if member.bot: return

        _cog.logger.info(f"Користувач &b{member.name} &rпокинув сервер.")

        channels: list[dict[str, str | int | dict]] = _cog.module_settings.get("member-leave.guild-messages")
        for channel_info in channels:

            channel = SBDS.main_bot.get_channel(channel_info["channel-id"])
            message = channel_info['message']
            await _cog.buildAndSendMessage(destination=channel, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)

        message = _cog.module_settings.get("member-leave.dm-message")
        if message is None or "": return
        try: await _cog.buildAndSendMessage(destination=member, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)
        except disnake.Forbidden: pass

    @staticmethod
    @SBDS.main_bot.listen("on_member_update")
    async def server_boosted(before: disnake.Member, after: disnake.Member):

        if after.bot: return
        if not set(after.roles) - set(before.roles): return

        role = SBDS.utils.getGuild().premium_subscriber_role
        # role = SBDS.utils.getGuild().get_role(1072466910702206976)

        if not role in before.roles and role in after.roles:

            member = after

            _cog.logger.info(f"Користувач &b{member.name} &rзабустив сервер! Рівень сервера: &d{SBDS.utils.getGuild().premium_tier}&r, кількість бустів &d{SBDS.utils.getGuild().premium_subscription_count}&r.")

            channels: list[dict[str, str | int | dict]] = _cog.module_settings.get("member-boost.guild-messages")
            for channel_info in channels:
                channel = SBDS.main_bot.get_channel(channel_info["channel-id"])
                message = channel_info['message']
                await _cog.buildAndSendMessage(destination=channel, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)

            message = _cog.module_settings.get("member-boost.dm-message")
            if message is None or "": return
            try: await _cog.buildAndSendMessage(destination=member, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)
            except disnake.Forbidden: pass

    @staticmethod
    @SBDS.main_bot.listen("on_member_update")
    async def server_unboosted(before: disnake.Member, after: disnake.Member):

        if after.bot: return

        role = SBDS.utils.getGuild().premium_subscriber_role
        # role = SBDS.utils.getGuild().get_role(1072466910702206976)

        if not role in after.roles and role in before.roles:

            member = after

            _cog.logger.info(f"Користувач &b{member.name} &rперестав бустити сервер. Рівень сервера: &d{SBDS.utils.getGuild().premium_tier}&r, кількість бустів &d{SBDS.utils.getGuild().premium_subscription_count}&r.")

            channels: list[dict[str, str | int | dict]] = _cog.module_settings.get("member-unboost.guild-messages")
            for channel_info in channels:
                channel = SBDS.main_bot.get_channel(channel_info["channel-id"])
                message = channel_info['message']
                await _cog.buildAndSendMessage(destination=channel, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)

            message = _cog.module_settings.get("member-unboost.dm-message")
            if message is None or "": return
            try: await _cog.buildAndSendMessage(destination=member, placeholders={"{{USERNAME}}": member.name, "{{DISPLAYNAME}}": member.display_name, "{{USER-MENTION}}": member.mention, "{{GUILDNAME}}": member.guild.name, "{{BOOST-COUNT}}": member.guild.premium_subscription_count, "{{BOOST-LEVEL}}": member.guild.premium_tier}, message=message)
            except disnake.Forbidden: pass




        

_cog = _UserAlertsModuleCog()

def setup(bot: commands.InteractionBot):

    SBDS.settings.checkKeys(check_keys={"user-alerts-module": dict}, path="modules")

    SBDS.settings.checkKeys(check_keys={"member-join": dict, "member-leave": dict, "member-boost": dict, "member-unboost": dict, "buttons-responses": dict}, path="modules.user-alerts-module")

    SBDS.settings.checkKeys(check_keys={"guild-messages": list}, path="modules.user-alerts-module.member-join")
    SBDS.settings.checkKeys(check_keys={"guild-messages": list}, path="modules.user-alerts-module.member-leave")
    SBDS.settings.checkKeys(check_keys={"guild-messages": list}, path="modules.user-alerts-module.member-boost")
    SBDS.settings.checkKeys(check_keys={"guild-messages": list}, path="modules.user-alerts-module.member-unboost")

    bot.add_cog(_cog)