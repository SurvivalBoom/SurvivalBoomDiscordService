package net.survivalboom.sbds.core.commands.cmds.console.database.member;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.argument.sbds.NamespacedKeyArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.database.guilds.IGuildData;
import net.survivalboom.sbds.api.database.members.IMemberData;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.container.INamespacedDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;

@CommandClass(name = "read", description = "Read user's data on a guild from the database")
public class DatabaseMemberReadCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        Guild guild = info.arguments().getCast("guild", Guild.class).orElseThrow();
        User user = info.arguments().getCast("user", User.class).orElseThrow();
        NamespacedKey key = info.arguments().getCast("key", NamespacedKey.class).orElse(null);
        String path = info.arguments().getCast("path", String.class).orElse(null);

        info.logger().info("Retrieving member data of `{}` on the guild `{}`...", user.getEffectiveName(), guild.getName());

        IMemberData memberData = info.sbds().getMemberDataManager().get(guild, user).join();
        if (memberData == null) {
            info.logger().info("There is no data for the user `{}` on the guild `{}`.", user.getEffectiveName(), guild.getName());
            return;
        }

        INamespacedDataContainer container = memberData.container();

        if (key == null) {

            var map = container.getAsMap();
            if (map.isEmpty()) {
                info.logger().info("There is no data for the user `{}` on the guild `{}`.", user.getEffectiveName(), guild.getName());
            }

            print(info, guild, user, () -> {

                for (var entry : map.entrySet()) {
                    var map2 = CommonUtils.getStringMapFromYaml(entry.getValue());
                    print0(info, entry.getKey(), map2);
                }

            });

            return;

        }

        if (path == null) {

            ConfigurationNode node = container.getNode(key).orElse(null);
            if (node == null) {
                info.logger().info("There is no data by key `{}` for the user `{}` on the guild `{}`.", key, user.getEffectiveName(), guild.getName());
                return;
            }

            var map = CommonUtils.getStringMapFromYaml(node);
            print(info, guild, user, () -> print0(info, key, map));

            return;

        }

        ConfigurationNode node = container.getNode(key).orElse(null);
        if (node == null) {
            info.logger().info("There is no data by key `{}` for the user `{}` on the guild `{}`.", key, user.getEffectiveName(), guild.getName());
            return;
        }

        String[] path0 = CommonUtils.splitString(path, "\\.");

        ConfigurationNode target = node.node((Object[]) path0);
        if (target.virtual()) {
            info.logger().info("There is no data by path `{}:{}` for the user `{}` on the guild `{}`.", key, path, user.getEffectiveName(), guild.getName());
            return;
        }

        Object value = target.get(Object.class);

        info.logger().info("[{}:{}] -> {}", key, path, value);

    }

    private void print(@NotNull ConsoleExecutionInfo info, @NotNull Guild guild, @NotNull User user, @NotNull Runnable runnable) {

        info.logger().info("--- --- < Member Data > --- ---");
        info.logger().info("> Guild: {}", guild.getName());
        info.logger().info("> User: {}", user.getEffectiveName());
        info.logger().info(" ");

        runnable.run();

        info.logger().info("---- ---- ---- ---- ---- ----");

    }

    private void print0(@NotNull ConsoleExecutionInfo info, @NotNull NamespacedKey key, @NotNull Map<String, String> map) {

        info.logger().info("> {}", key);

        for (var entry : map.entrySet()) {
            info.logger().info("* {}: {}", entry.getKey(), entry.getValue());
        }

        info.logger().info(" ");

    }

    @ArgumentMethod(index = 1, required = false)
    public NamespacedKeyArgument key() {
        return new NamespacedKeyArgument();
    }

    @ArgumentMethod(index = 2, required = false)
    public StringArgument path() {
        return new StringArgument();
    }

}
