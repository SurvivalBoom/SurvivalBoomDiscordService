package net.survivalboom.sbds.core.commands.cmds.console.database.user;

import net.dv8tion.jda.api.entities.User;
import net.survivalboom.sbds.api.commands.argument.primitive.StringArgument;
import net.survivalboom.sbds.api.commands.argument.sbds.NamespacedKeyArgument;
import net.survivalboom.sbds.api.commands.base.ArgumentMethod;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.database.users.IUserData;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.container.INamespacedDataContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;

@CommandClass(name = "read", description = "Read user data from the database")
public class DatabaseUserReadCommand extends CommandBase implements ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) throws Throwable {

        User user = info.arguments().getCast("user", User.class).orElseThrow();
        NamespacedKey key = info.arguments().getCast("key", NamespacedKey.class).orElse(null);
        String path = info.arguments().getCast("path", String.class).orElse(null);

        info.logger().info("Retrieving user data of `{}`...", user.getEffectiveName());

        IUserData userData = info.sbds().getUserDataManager().get(user).join();
        if (userData == null) {
            info.logger().info("There is no data for user `{}`.", user.getEffectiveName());
            return;
        }

        INamespacedDataContainer container = userData.container();

        if (key == null) {

            var map = container.getAsMap();
            if (map.isEmpty()) {
                info.logger().info("There is no data for user `{}`.", user.getEffectiveName());
            }

            print(info, user, () -> {

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
                info.logger().info("There is no data by key `{}` in user `{}`.", key, user.getEffectiveName());
                return;
            }

            var map = CommonUtils.getStringMapFromYaml(node);
            print(info, user, () -> print0(info, key, map));

            return;

        }

        ConfigurationNode node = container.getNode(key).orElse(null);
        if (node == null) {
            info.logger().info("There is no data by key `{}` in user `{}`.", key, user.getEffectiveName());
            return;
        }

        String[] path0 = CommonUtils.splitString(path, "\\.");

        ConfigurationNode target = node.node((Object[]) path0);
        if (target.virtual()) {
            info.logger().info("There is no data by path `{}:{}` in user `{}`.", key, path, user.getEffectiveName());
            return;
        }

        Object value = target.get(Object.class);

        info.logger().info("[{}:{}] -> {}", key, path, value);

    }

    private void print(@NotNull ConsoleExecutionInfo info, @NotNull User user, @NotNull Runnable runnable) {

        info.logger().info("--- --- < User Data > --- ---");
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
