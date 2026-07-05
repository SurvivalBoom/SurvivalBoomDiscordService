package net.survivalboom.sbds.core.commands.cmds.common;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.commands.base.CommandClass;
import net.survivalboom.sbds.api.commands.base.CommandBase;
import net.survivalboom.sbds.api.commands.slash.SlashCommandExecutor;
import net.survivalboom.sbds.api.commands.slash.SlashExecutionInfo;
import net.survivalboom.sbds.api.commands.console.ConsoleCommandExecutor;
import net.survivalboom.sbds.api.commands.console.ConsoleExecutionInfo;
import net.survivalboom.sbds.api.commands.string.StringCommandExecutor;
import net.survivalboom.sbds.api.commands.string.StringExecutionInfo;
import net.survivalboom.sbds.api.interaction.InteractionHolder;
import net.survivalboom.sbds.api.modules.IModuleManager;
import net.survivalboom.sbds.api.monitoring.ISystemMonitor;
import net.survivalboom.sbds.api.monitoring.cpu.ICpuInfo;
import net.survivalboom.sbds.api.monitoring.cpu.ICpuMonitor;
import net.survivalboom.sbds.api.monitoring.memory.IMemoryInfo;
import net.survivalboom.sbds.api.monitoring.os.IOperatingSystemInfo;
import net.survivalboom.sbds.api.scheduler.IScheduler;
import net.survivalboom.sbds.api.utils.placeholders.Placeholders;
import net.survivalboom.sbds.core.BuildConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@CommandClass(name = "status", description = "Shows a status of the discord bot.", translationKey = "sbds.command.status", permission = "sbds.commands.status", defaultPermission = true)
public class StatusCommand extends CommandBase implements SlashCommandExecutor, StringCommandExecutor, ConsoleCommandExecutor {

    @Override
    public void executes(@NotNull StringExecutionInfo info) throws Throwable {
        executes0(info);
    }

    @Override
    public void executes(@NotNull SlashExecutionInfo info) {
        executes0(info);
    }

    private void executes0(@NotNull InteractionHolder info) {

        Placeholders placeholders = placeholders(info.sbds());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Rawr!!! \uD83E\uDD96");

        builder.setDescription(placeholders.parse(
                """
                
                *{bot}* is running on __SBDS__ v{version}
                Serving `{servers}` servers.
                
                **System**
                - Runtime: `{runtime}`
                - Memory: `{usedMemory}`**/**`{maxMemory}` *({freeMemory} free)*
                - CPU: `Process: {cpuLoadProcess}% | System: {cpuLoadSystem}% / {threadCount} Threads` *({cpuModel})*
                - Active tasks: `{tasks}`
                - Ping: `{ping}ms` *(to Discord API server)*
                
                **Loaded modules**
                - {modules}
                
                """
        ));

        builder.setColor(Color.MAGENTA);

        builder.setFooter("SurvivalBoom Discord Service | By TIMURishche", "https://cdn.discordapp.com/avatars/1102984687179276288/852ae72b5e79b3df573c8b67b7baada4.webp?size=1024&format=webp");

        info.replyRaw(MessageCreateData.fromEmbeds(builder.build())).queue();

    }

    @Override
    public void executes(@NotNull ConsoleExecutionInfo info) {

        Placeholders placeholders = placeholders(info.sbds());

        List<String> lines = new ArrayList<>();

        lines.add("--- Current Status ---");

        lines.add("{bot} is running on SBDS v{version}");
        lines.add("Serving {servers} servers.");
        lines.add(" ");
        lines.add("> System <");
        lines.add("Runtime: {runtime}");
        lines.add("Memory: {usedMemory}/{maxMemory} ({freeMemory} free)");
        lines.add("CPU: {cpuModel}");
        lines.add("  Process: {cpuLoadProcess}%");
        lines.add("  System: {cpuLoadSystem}%");
        lines.add("  Active threads: {threadCount}");
        lines.add(" ");
        lines.add("> Loaded modules <");
        lines.addAll(info.sbds().getModuleManager().getModules().stream().map(m -> "- " + m.getName() + " v" + m.getMeta().getVersion() + " ~ " + (m.isEnabled() ? "Enabled" : "Disabled")).toList());

        lines = placeholders.parseAll(lines);

        lines.add("--- ---- ---- ---- ---");

        lines.forEach(l -> info.logger().info(l));

    }

    private Placeholders placeholders(@NotNull ISBDS sbds) {

        ISystemMonitor systemMonitor = sbds.getSystemMonitor();

        IOperatingSystemInfo osInfo = systemMonitor.getOperatingSystemInfo();
        IMemoryInfo memoryInfo = systemMonitor.getMemoryInfo();
        ICpuInfo cpuInfo = systemMonitor.getCpuInfo();
        ICpuMonitor cpuMonitor = systemMonitor.getCpuMonitor();

        JDA jda = sbds.getBot();
        IScheduler scheduler = sbds.getScheduler();

        IModuleManager moduleManager = sbds.getModuleManager();

        return Placeholders.of(
                "version", BuildConstants.VERSION,
                "bot", jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator(),
                "servers", jda.getGuilds().size(),
                "runtime", osInfo.fullName(),
                "threadCount", Runtime.getRuntime().availableProcessors(),
                "usedMemory", formatBytes(memoryInfo.getUsedPhysicalMemory()),
                "freeMemory", formatBytes(memoryInfo.getAvailablePhysicalMemory()),
                "maxMemory", formatBytes(memoryInfo.getTotalPhysicalMemory()),
                "cpuModel", cpuInfo.model(),
                "cpuLoadProcess", Math.max(0, Math.round(cpuMonitor.processLoad() * 100.0)),
                "cpuLoadSystem", Math.max(0, Math.round(cpuMonitor.systemLoad() * 100.0)),
                "ping", jda.getGatewayPing(),
                "tasks", scheduler.getTasks().size(),
                "modules", modulesString(moduleManager)
        );

    }

    private String modulesString(@NotNull IModuleManager manager) {
        return String.join("\n", manager.getModules().stream().map(m -> "- " + m.getName() + " v" + m.getMeta().getVersion()).toList());
    }

    private int toMB(double v) {
        return (int) Math.floor(v / 1024 / 1024);
    }

    private int toGB(double v) {
        return (int) Math.floor((double) toMB(v) / 1024);
    }

    private String formatBytes(double v) {
        int i = toMB(v);
        return i > 1024 ? toGB(v) + "GB" : i + "MB";
    }

}
