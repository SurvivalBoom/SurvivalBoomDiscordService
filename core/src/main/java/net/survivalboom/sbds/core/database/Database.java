package net.survivalboom.sbds.core.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.survivalboom.sbds.api.database.DataRecord;
import net.survivalboom.sbds.api.database.IDatabase;
import net.survivalboom.sbds.api.database.IRepository;
import net.survivalboom.sbds.api.database.converters.ChannelConverter;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.CommonUtils;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import net.survivalboom.sbds.core.utils.InternalPushQueue;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Database extends Manager implements IDatabase {

    private static final Logger log = LoggerFactory.getLogger("Database");

    private final SBDS sbds;

    private @Nullable Properties properties = null;
    private @Nullable SessionFactory sessionFactory = null;

    private final InternalRegistrationManager<IRepository<?>> repositoriesRegistry;

    private final InternalRegistrationManager<IRegisteredTypeSerializer<?>> typeSerializersRegistry;

    private final DatabaseQueue queue;

    // repo rebuild queue //

    private final List<IRepository<?>> currentAttachedRepositories = new ArrayList<>();

    private final InternalPushQueue<IRepository<?>> rebuildQueue;

    private boolean failed = false;

    private volatile boolean isRebuilding = false;


    public Database(@NotNull SBDS sbds) {
        this.sbds = sbds;
        this.repositoriesRegistry = new InternalRegistrationManager<>(this, "repository", null, sbds.getRegistrationRegistry());
        this.typeSerializersRegistry = new InternalRegistrationManager<>(this, "serializers", null, sbds.getRegistrationRegistry());
        this.queue = new DatabaseQueue(this, sbds.getScheduler());
        this.rebuildQueue = new InternalPushQueue<>(this::rebuildQueue, "DatabaseRebuild", 500, sbds);
    }

    //
    // MANAGER
    //

    @Override
    protected void init0() {

        log.info("Loading database...");

        new File(sbds.getWorkingDir(), "data").mkdirs();

        reload0(null, true, false, true, null);

        typeSerializersRegistry.init();
        repositoriesRegistry.init();
        rebuildQueue.init();

        registerSerializer0(null, Channel.class, new ChannelConverter());

        queue.init();

    }

    @Override
    protected void shutdown0() {

        log.info("Shutting down database...");

        repositoriesRegistry.shutdown();
        typeSerializersRegistry.shutdown();

        rebuildQueue.shutdown();
        queue.shutdown();

        if (sessionFactory != null) {
            sessionFactory.close();
        }

    }

    public boolean isFailed() {
        return failed;
    }

    // REBUILD QUEUE //

    private void rebuildQueue(InternalPushQueue<IRepository<?>> queue) {

        try {
            reload0(null, true, true, false, queue.getQueue());
        }

        catch (Throwable t) {
            log.error("Failed to build the SessionFactory. Please ensure that database credentials are correct and your database is online.", t);
        }

    }

    // RELOAD //

    @Override
    public void reload(@NotNull IModule module) {
        Objects.requireNonNull(module, "module == null");
        reload0(module, false, true, true, null);
    }

    public void reload0(
            @Nullable IModule module,
            boolean silent,
            boolean rebuildSessionFactory,
            boolean reloadProperties,
            @Nullable Collection<IRepository<?>> toImport
    ) {

        checkValid();
        
        if (!reloadProperties && !rebuildSessionFactory) {
            throw new RuntimeException("Вітаємо! Ви - ідіот!");
        }

        if (module != null) {

            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module attempted to request a database reload");

            if (!silent) {
                log.warn("Module {} requested a database reload. Reloading the database!", module);
            }

        }

        else if (!silent) {
            log.warn("Requested a database reload! Reloading the database!");
        }

        if (this.isRebuilding) {
            throw new IllegalStateException("Database already rebuiling");
        }

        try {

            this.isRebuilding = true;

            if (reloadProperties) {
                loadProperties();
            }

            if (rebuildSessionFactory) {
                rebuildSessionFactory(toImport);
            }

        }

        catch (Exception t) {
            failed = true;
            log.error("Failed to reload the database! SBDS will not function properly!");
            log.error("All calls to the database will cause an IllegalStateException. Everything that works with the database will break!", t);
            return;
        }

        finally {
            this.isRebuilding = false;
        }

        failed = false;

        if (!silent) {
            log.info("Database reloaded successfully!");
        }

    }

    private void loadProperties() {

        this.properties = null;

        ConfigurationNode config = sbds.getConfiguration();

        ConfigurationNode databaseSection = config.node("database");

        String jdbcUrl = databaseSection.node("jdbc").getString("null")
                .replace("{SBDS-DIR}", sbds.getWorkingDir().getAbsolutePath());

        String driver = databaseSection.node("driver").getString("null");
        String dialect = databaseSection.node("dialect").getString();
        String tableModifier = databaseSection.node("mode").getString("validate");

        String username = databaseSection.node("user").getString();
        String password = databaseSection.node("password").getString();


        Properties properties = CommonUtils.getPropertiesFromYaml(databaseSection.node("properties"));

        properties.setProperty("hibernate.connection.url", jdbcUrl);

        properties.setProperty("hibernate.connection.driver_class", driver);
        if (dialect != null) properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.hbm2ddl.auto", tableModifier);

        if (username != null) properties.setProperty("hibernate.connection.username", username);
        if (password != null) properties.setProperty("hibernate.connection.password", password);

        properties.setProperty("hibernate.hikari.poolName", "DatabaseHikariMain");
//        properties.setProperty("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
        properties.setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

        this.properties = properties;

    }

    private void rebuildSessionFactory(@Nullable Collection<IRepository<?>> toImport) {

        Objects.requireNonNull(properties, "properties == null");
        
        if (toImport == null) {
            log.info("Rebuilding SessionFactory...");
        }

        else {
            log.info("Rebuilding SessionFactory to include {} new repositories.", toImport.size());
        }

        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }

        // Нам потрібно щоб Hibernate вантажив класи репозиторіїв від імені Root Class Loader, оскільки виявляється Hibernate не має доступу до класів модулів :(
        // Це може викликати проблеми, оскільки тоді Hibernate повністю обходить ізоляцію, але... Коли будуть проблеми, от тоді зробимо розумніше!
        BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder()
                        .applyClassLoader(sbds.getLibrariesManager().getRootClassLoader())
                        .build();

        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration(serviceRegistry);
        configuration.setProperties(properties);

        List<IRepository<?>> repos = new ArrayList<>(currentAttachedRepositories);
        if (toImport != null) {
            repos.addAll(toImport);
        }

        for (var repo : repos) {
            Class<?> clazz = repo.getRecordClass();
            configuration.addAnnotatedClass(clazz);
        }

        this.currentAttachedRepositories.clear();
        this.currentAttachedRepositories.addAll(repos);

        sessionFactory = configuration.buildSessionFactory();

    }

    //
    // REPOSITORIES
    //

    // CREATE //

    @Override
    public <T extends DataRecord> @NotNull Repository<T> createRepository(@NotNull IModule module, @NotNull String name, @NotNull Class<T> clazz) {

        Objects.requireNonNull(module, "module == null");
        Objects.requireNonNull(name, "name == null");
        Objects.requireNonNull(clazz, "clazz == null");

        return createRepository0(module, name, clazz);

    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends DataRecord> @NotNull Repository<T> createRepository0(@Nullable IModule module, @NotNull String name, @NotNull Class<T> clazz) {

        checkValid();

        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("DataRecord class must have jakarta.persistence.Entity annotation");
        }

        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new IllegalArgumentException("RepositoryHandler's DataRecord class must have jakarta.persistence.Table annotation");
        }

        String tableName = table.name();
        boolean tableExists = repositoriesRegistry.getRegistrations().stream()
                .map(Registration::object)
                .map(IRepository::getRecordClass)
                .anyMatch(cls -> cls.getAnnotation(Table.class).name().equals(tableName));

        if (tableExists) {
            throw new IllegalArgumentException("DataRecord with table name `" + tableName + "` already exist");
        }

        if (module != null) {
            sbds.getModuleManager().checkModuleEnabled(module, "Disabled module attempted to create a repository");
        }

        Repository<T> repository = new Repository<>(clazz, this);
        repository.registration = (Registration<IRepository<T>>) (Registration<?>) repositoriesRegistry.register0(module, name, repository);

        this.isRebuilding = true;
        rebuildQueue.append(repository);

        return repository;

    }

    // REMOVE //


    @Override
    public synchronized boolean removeRepository(@NotNull IRepository<?> repository) {

        checkValid();

        var reg = repositoriesRegistry.unregister(repository);

        return reg != null;

    }

    // GETTERS //

    @Override
    public @Nullable IRepository<?> getRepository(@NotNull NamespacedKey key) {

        checkValid();

        var reg = repositoriesRegistry.getRegistration(key);
        if (reg == null) {
            return null;
        }

        return reg.object();

    }

    @Override
    public @NotNull List<IRepository<?>> getRepositories() {
        return repositoriesRegistry.getRegisteredObjects();
    }

    //
    // SESSIONS
    //

    public @NotNull Session requestSession(@NotNull Repository<?> repository) {

        checkValid();
        checkRepository(repository);
        checkDatabase();

        return createSession();

    }

    public @NotNull Session createSession() {
        CommonUtils.waitUntil(() -> !isRebuilding, 30000);
        Objects.requireNonNull(sessionFactory, "sessionFactory == null, something went wrong");
        return sessionFactory.openSession();
    }


    //
    // QUEUE
    //

    @Override
    public void queueSave(@NotNull DataRecord record) {
        checkRepository(record);
        queue.queueRecordSave(record);
    }

    public @NotNull <V> CompletableFuture<V> queueSessionRequest(@NotNull Repository<?> repository, @NotNull Function<Session, V> function) {

        checkValid();
        checkRepository(repository);
        checkDatabase();

        return queue.queueSessionRequest(function);

    }

    public @NotNull CompletableFuture<Void> queueSessionRequest(@NotNull Repository<?> repository, @NotNull Consumer<Session> consumer) {

        checkValid();
        checkRepository(repository);
        checkDatabase();

        return queue.queueSessionRequest(consumer);

    }

    public @NotNull DatabaseQueue getQueue() {
        return queue;
    }

    //
    // TYPE SERIALIZERS
    //

    // REG //

    @Override
    public @NotNull <T> IRegisteredTypeSerializer<T> registerSerializer(
            @NotNull IModule module,
            @NotNull Class<T> clazz,
            @NotNull TypeSerializer<T> serializer
    ) {
        Objects.requireNonNull(module, "module == null");
        return registerSerializer0(module, clazz, serializer);
    }

    @SuppressWarnings("unchecked") // <-- Я сказав тобі, сходи нахуй!
    public @NotNull <T> IRegisteredTypeSerializer<T> registerSerializer0(
            @Nullable IModule module,
            @NotNull Class<T> clazz,
            @NotNull TypeSerializer<T> serializer
    ) {

        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(serializer, "serializer == null");
        checkValid();

        IRegisteredTypeSerializer<?> reg = getRegisteredSerializers().stream()
                .filter(ser -> ser.getClazz().equals(clazz))
                .findAny()
                .orElse(null);
        if (reg != null) {
            throw new IllegalStateException("TypeSerializer for `" + clazz + "` already exists: `" + reg.getRegistration().key() + "`");
        }

        RegisteredTypeSerializer<T> registeredTypeSerializer = new RegisteredTypeSerializer<>(clazz, serializer);
        String name = clazz.getSimpleName().toLowerCase();

        registeredTypeSerializer.registration = (Registration<IRegisteredTypeSerializer<T>>) (Registration<?>) typeSerializersRegistry.register0(module, name, registeredTypeSerializer);

        return registeredTypeSerializer;

    }

    // UNREG //

    @Override
    public boolean unregisterSerializer(@NotNull IRegisteredTypeSerializer<?> registration) {
        checkValid();
        return typeSerializersRegistry.unregister(registration) != null;
    }

    @Override
    public @Nullable IRegisteredTypeSerializer<?> unregisterSerializer(@NotNull NamespacedKey key) {

        var reg = getSerializer(key);
        if (reg == null) {
            return null;
        }

        unregisterSerializer(reg);
        return reg;

    }

    // GET //

    @Override
    public @Nullable IRegisteredTypeSerializer<?> getSerializer(@NotNull NamespacedKey key) {
        checkValid();
        return typeSerializersRegistry.getRegistrationAsObject(key);
    }

    @Override
    public @NotNull List<IRegisteredTypeSerializer<?>> getRegisteredSerializers() {
        checkValid();
        return typeSerializersRegistry.getRegisteredObjects();
    }

    // RECORD //

    public static class RegisteredTypeSerializer<T> implements IRegisteredTypeSerializer<T> {

        private final TypeSerializer<T> serializer;

        private final Class<T> clazz;

        private Registration<IRegisteredTypeSerializer<T>> registration;


        public RegisteredTypeSerializer(@NotNull Class<T> clazz, @NotNull TypeSerializer<T> serializer) {
            this.serializer = serializer;
            this.clazz = clazz;
        }


        @Override
        public @NotNull Registration<IRegisteredTypeSerializer<T>> getRegistration() {
            return registration;
        }

        @Override
        public @NotNull TypeSerializer<T> getSerializer() {
            return serializer;
        }

        @Override
        public @NotNull Class<T> getClazz() {
            return clazz;
        }
    }


    //
    // UTILS
    //

    public void checkRepository(@NotNull DataRecord record) {

        Objects.requireNonNull(record, "record == null");

        if (repositoriesRegistry.getRegistrations().stream().noneMatch(reg -> reg.object().getRecordClass().equals(record.getClass()))) {
            throw new IllegalArgumentException("Repository object is not registered in the Database. Looks like this repository object is no longer valid.");
        }

    }

    private void checkRepository(@NotNull Repository<?> repository) {

        if (repositoriesRegistry.getObjectRegistration(repository) == null) {
            throw new IllegalArgumentException("Repository object is not registered in the Database. Looks like this repository object is no longer valid.");
        }

        if (!repository.isValid()) {
            throw new IllegalArgumentException("Repository object is registered, but method Repository#valid returned false. Did you break something?");
        }

    }

    private void checkDatabase() {
        CommonUtils.waitUntil(() -> !isRebuilding);
        if (sessionFactory == null) {
            throw new IllegalStateException("Datasource is not present. Looks like database was reloaded incorrectly.");
        }

    }

}
