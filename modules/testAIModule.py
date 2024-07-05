import traceback

import aiohttp
import disnake

from main import SurvivalBoomDiscordService as SBDS
from disnake.ext import commands
from google import generativeai as genai

GOOGLE_API_KEY = "AIzaSyAJvJn3qBTODbgGjK7aPVZDEm_b4H_u2IU"

this_module_name = f"{__name__}".removeprefix("modules.")
logger = SBDS.mainlogger.createModuleLogger(this_module_name)

genai.configure(api_key=GOOGLE_API_KEY)

is_busy = False

class TestAIModule(commands.Cog):

    @commands.slash_command(name="ai", description="SurvivalBoom AI Service?", options=[
        disnake.Option(name="prompt", description="prompt", type=3, required=True)
    ])
    async def aiCommand(self, ctx: disnake.ApplicationCommandInteraction, prompt: str):

        global is_busy
        if is_busy:
            await ctx.send("SurvivalBoom AI Service зараз зайнятий...")
            return

        await ctx.send("Здійснюємо запит до SurvivalBoom AI Service...")

        try:

            is_busy = True
            response, session = await self.httpRequest(f"http://149.202.89.70:25514/send/{prompt}")

            if response.status != 200:
                await ctx.edit_original_response(f"Сталась помилка при спробі зв'язатись з SurvivalBoom AI Service: {await response.text()}")
                is_busy = False
                return

            text = await response.text()

            if len(text) <= 2000:
                await ctx.edit_original_response(text)
            else:
                # Разбиваем сообщение на части
                chunks = [text[i:i + 2000] for i in range(0, len(text), 2000)]
                for chunk in chunks:
                    await ctx.send(chunk)

            await session.close()


        except Exception as error:
            await ctx.edit_original_response(f"Упсь! Сталась помилка! {error}")
            logger.error(traceback.format_exc())

        is_busy = False


    @staticmethod
    async def httpRequest(url):
        session = aiohttp.ClientSession()
        resp = await session.get(url)
        return resp, session

def setup(bot: commands.Bot):
    bot.add_cog(TestAIModule())
