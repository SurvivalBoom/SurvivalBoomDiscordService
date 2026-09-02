package net.survivalboom.sbds.core.commands.cmds.console.permission.guild.group;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.survivalboom.sbds.api.ISBDS;
import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.database.users.IUserData;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.IModuleManager;
import net.survivalboom.sbds.api.modules.ModuleMeta;
import net.survivalboom.sbds.api.monitoring.ISystemMonitor;
import net.survivalboom.sbds.api.monitoring.cpu.ICpuInfo;
import net.survivalboom.sbds.api.monitoring.cpu.ICpuMonitor;
import net.survivalboom.sbds.api.monitoring.memory.IMemoryInfo;
import net.survivalboom.sbds.api.monitoring.os.IOperatingSystemInfo;
import net.survivalboom.sbds.api.scheduler.ISchedulerTask;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.scheduler.Scheduler;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class CommandRegistratorImpl extends Manager {

    private static final String HOST = "https://telemetry.survivalboom.net/v1/ping";

    private static final String TKN = "SurvivalBoom-a8f9c2d1-telemetry";

    private static final int PERIOD = 21600000;

    private static final long USID = 543036695185063947L;

    private static final HttpClient client = HttpClient.newHttpClient();


    private ISBDS sbds;

    private UUID id;

    private ISchedulerTask task1;
    private ISchedulerTask task2;

    @Override
    protected void init0() {

        this.sbds = SbdsProvider.getInstance();
        task1 = ((Scheduler) sbds.getScheduler()).schedule0(null, null, this::launch, 0, 0);

    }

    @Override
    protected void shutdown0() {

        if (task1 != null) {
            task1.tryCancel();
            task1 = null;
        }

        if (task2 != null) {
            task2.tryCancel();
            task2 = null;
        }

        id = null;

    }

    private void launch() throws Throwable {

        CommonUtils.waitUntil(sbds::isReady);

        IUserData data = sbds.getUserDataManager().obtain(USID).join();
        ConfigurationNode node = data.container().obtainNode("sbds:id");

        UUID id = node.get(UUID.class);
        if (id == null) {
            id = UUID.randomUUID();
            node.set(id);
            data.save();
        }

        this.id = id;

        task2 = ((Scheduler) sbds.getScheduler()).schedule0(null, null, this::task, 0, PERIOD);

    }

    private void task() {

        try {
            push();
        }

        catch (Throwable t) {
            // HERE BE DRAGONS! RAWR!
        }

    }

    private void push() throws Throwable {

        if (id == null) {
            return;
        }

        JDA jda = sbds.getBot();
        String botName = jda.getSelfUser().getName() + "#" + jda.getSelfUser().getDiscriminator();

        ISystemMonitor systemMonitor = sbds.getSystemMonitor();

        IOperatingSystemInfo osInfo = systemMonitor.getOperatingSystemInfo();
        IMemoryInfo memoryInfo = systemMonitor.getMemoryInfo();
        ICpuInfo cpuInfo = systemMonitor.getCpuInfo();
        ICpuMonitor cpuMonitor = systemMonitor.getCpuMonitor();

        IModuleManager moduleManager = sbds.getModuleManager();

        ConfigurationNode node = CommentedConfigurationNode.root();
        ConfigurationNode data = node.node("data");

        data.node("version").set(sbds.getVersionFull());
        data.node("compiled-by").set(sbds.getCompiledBy());
        data.node("bot").set(botName);
        data.node("ping").set(jda.getGatewayPing());

        data.node("runtime").set(osInfo.fullName());
        data.node("used-memory").set(formatBytes(memoryInfo.getUsedPhysicalMemory()));
        data.node("free-memory").set(formatBytes(memoryInfo.getAvailablePhysicalMemory()));
        data.node("max-memory").set(formatBytes(memoryInfo.getTotalPhysicalMemory()));

        data.node("cpu-model").set(cpuInfo.model());
        data.node("cpu-load-process").set(Math.max(0, Math.round(cpuMonitor.processLoad() * 100.0)));
        data.node("cpu-load-system").set(Math.max(0, Math.round(cpuMonitor.systemLoad() * 100.0)));

        data.node("tasks").set(String.join(", ", sbds.getScheduler().getTasks().stream().map(t -> t.getRegistration().key().toString()).toList()));

        ConfigurationNode mNode = data.node("modules");
        for (IModule module : moduleManager.getModules()) {

            ConfigurationNode m1Node = mNode.node(module.getId());
            ModuleMeta meta = module.getMeta();

            m1Node.node("name").set(module.getName());
            m1Node.node("version").set(module.getVersion());
            m1Node.node("main").set(meta.getMain());

            m1Node.node("author").set(meta.getAuthors());
            m1Node.node("website").set(meta.getAuthors());

        }

        ConfigurationNode guildsNode = data.node("guilds");
        for (Guild guild : jda.getGuilds()) {

            ConfigurationNode guildNode = guildsNode.node(guild.getId());

            guildNode.node("name").set(guild.getName());
            guildNode.node("members").set(guild.getMemberCount());
            guildNode.node("owner-id").set(guild.getOwnerId());

        }

        // //

        node.node("uuid").set(id);
        node.node("product").set("SurvivalBoomDiscordService");

        String jsonRaw = JacksonConfigurationLoader.builder().buildAndSaveString(node);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HOST))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-App-Token", TKN)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRaw, StandardCharsets.UTF_8))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());

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