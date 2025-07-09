package net.survivalboom.sbds.modules.translation.commands;

import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.commands.ArgumentScope;
import net.survivalboom.sbds.api.commands.argument.Argument;
import net.survivalboom.sbds.api.commands.argument.discord.UserArgument;
import net.survivalboom.sbds.api.commands.argument.misc.StringSelectArgument;
import net.survivalboom.sbds.api.commands.argument.misc.TranslationArgument;
import net.survivalboom.sbds.api.commands.base.Command;
import net.survivalboom.sbds.api.commands.base.CommandArgument;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.console.ConsoleCommand;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.slash.SlashCommand;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.users.IUserData;
import net.survivalboom.sbds.api.database.users.IUserRepositoryHandler;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.translations.ITranslationManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Command(name = "translation", description = "Sets translation of the bot for you", translationKey = "translation.command.translation")
public class TranslationCommand extends CommandBase implements SlashCommand, ConsoleCommand {

    private final ITranslationManager translationManager;

    private final IUserRepositoryHandler repository;


    public TranslationCommand(@NotNull IDatabase database, @NotNull ITranslationManager translationManager) {
        this.translationManager = translationManager;
        this.repository = database.getRepositoryHandler("sbds:users", IUserRepositoryHandler.class);
    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {

        String translationRaw = info.arguments().getCastOrNull("translation", String.class);
        IUserData userData = repository.createUser(info.user());

        if (translationRaw == null) {

            ITranslation currentTranslation = userData.translation();
            String displayName = currentTranslation != null ? currentTranslation.displayName() : "[values.none]";

            info.reply("translation.command.translation.show").withPlaceholders("{TRANSLATION}", displayName).queue();

            return;

        }

        ITranslation translation = translationManager.getTranslation(translationRaw);
        Objects.requireNonNull(translation, "Invalid translation");

        userData.translation(translation);
        userData.save();

        info.reply("translation.command.translation.set").withPlaceholders("{TRANSLATION}", translation.displayName()).queue();

    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        User user = info.arguments().getCastOrNull("user", User.class);
        Objects.requireNonNull(user, "user == null");

        IUserData userData = repository.createUser(user);

        ITranslation translation = info.arguments().getCastOrNull("translation0", ITranslation.class);
        if (translation == null) {


            ITranslation currentTranslation = userData.translation();

            if (currentTranslation != null) {
                info.logger().info("Current translation for `{}` is `{}`.", user.getEffectiveName(), currentTranslation.getName());
            }

            else {
                info.logger().info("Translation for `{}` is not set.", user.getEffectiveName());
            }

            return;

        }


        userData.translation(translation);
        userData.save();

        info.logger().info("Successfully set translation for `{}` to `{}`.", user.getEffectiveName(), translation.getName());

    }

    @CommandArgument(name = "translation0", description = "A translation", scope = ArgumentScope.CONSOLE, required = false)
    public Argument<?> translation0() {
        return new TranslationArgument();
    }

    @CommandArgument(name = "user", description = "A user", index = 1, scope = ArgumentScope.CONSOLE)
    public Argument<?> user() {
        return new UserArgument();
    }

    // TODO: Замінити на нормальний TranslationArgument з AbstractSelectArgument.
    // TODO: ! Варіанти вибору не динамічні, це значить що кожен раз після видалення/створення нового перекладу потрібно перереєструвати цю команду !
    @CommandArgument(name = "translation", description = "A translation", scope = ArgumentScope.SLASH, required = false)
    public Argument<?> translation() {

        // Ось що буває, коли сам себе в свої ж костилі заплутуєш.
        // Неможливо викликати translationManager, тому що конструктор CommandBase, який створює аргументи, викликається перед конструктором цього класу.
        // Тому translationManager з цього класу, в CommandBase буде завжди null.
        // А ви ще питаєте чому я стільки часу витрачаю на розробку систем без костилів?
//        return new StringSelectArgument(translationManager.getTranslations().stream().map(ITranslation::getName).toList());

        // Поки що використаю затичку, але треба виправити цей костиль!
        return new StringSelectArgument(SbdsProvider.getInstance().getTranslationManager().getTranslations().stream().map(ITranslation::getName).toList());

    }

}
