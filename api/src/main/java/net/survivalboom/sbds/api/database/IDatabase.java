package net.survivalboom.sbds.api.database;

import net.survivalboom.sbds.api.SbdsProvider;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.modules.ModuleMain;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.valid.IManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import javax.naming.Name;
import java.util.List;
import java.util.function.Consumer;

public interface IDatabase extends IManager {

    //
    // DATABASE
    //

    void reload(@NotNull IModule module);

    void queueSave(@NotNull DataRecord record);

    //
    // REPOSITORIES
    //

    // CREATION //

    @NotNull <T extends DataRecord> IRepository<T> createRepository(@NotNull IModule module, @NotNull Class<T> clazz);

    default <T extends DataRecord> @NotNull IRepository<T> createRepository(@NotNull ModuleMain module, @NotNull Class<T> clazz) {
        return createRepository(module.getModule(), clazz);
    }

    // REMOVE //

    boolean removeRepository(@NotNull IRepository<?> repository);

    default @Nullable IRepository<?> removeRepository(@NotNull NamespacedKey key) {

        IRepository<?> repository = getRepository(key, DataRecord.class);
        if (repository == null) {
            return null;
        }

        removeRepository(repository);

        return repository;

    }

    default @Nullable IRepository<?> removeRepository(@NotNull String key) {
        return removeRepository(NamespacedKey.fromString(key));
    }

    // GETTERS //

    @Nullable IRepository<?> getRepository(@NotNull NamespacedKey key);

    default IRepository<?> getRepository(@NotNull String key) {
        return getRepository(NamespacedKey.fromString(key));
    }

    @SuppressWarnings("unchecked") // <- ІДІ НАААХУУУЙЙ!!!!!
    default <T extends DataRecord> @Nullable IRepository<T> getRepository(@NotNull NamespacedKey key, @NotNull Class<T> clazz) {

        var repository = getRepository(key);
        if (repository == null) {
            return null;
        }

        return (IRepository<T>) repository;

    }

    default <T extends DataRecord> @Nullable IRepository<T> getRepository(@NotNull String name, @NotNull Class<T> clazz) {
        return getRepository(NamespacedKey.fromString(name), clazz);
    }

    @NotNull List<IRepository<?>> getRepositories();

    //
    // CONTAINER CONVERTERS
    //

    // REG //

    <T> @NotNull IRegisteredTypeSerializer<T> registerSerializer(
            @NotNull IModule module,
            @NotNull Class<T> clazz,
            @NotNull TypeSerializer<T> serializer
    );

    default <T> @NotNull IRegisteredTypeSerializer<T> registerSerializer(
            @NotNull ModuleMain module,
            @NotNull Class<T> clazz,
            @NotNull TypeSerializer<T> serializer
    ) {
        return registerSerializer(module.getModule(), clazz, serializer);
    }

    // UNREG //

    boolean unregisterSerializer(@NotNull IRegisteredTypeSerializer<?> registration);

    @Nullable IRegisteredTypeSerializer<?> unregisterSerializer(@NotNull NamespacedKey key);

    default @Nullable IRegisteredTypeSerializer<?> unregisterSerializer(@NotNull String key) {
        return unregisterSerializer(NamespacedKey.fromString(key));
    }

    // GET //

    @Nullable IRegisteredTypeSerializer<?> getSerializer(@NotNull NamespacedKey key);

    default @Nullable IRegisteredTypeSerializer<?> getSerializer(@NotNull String key) {
        return getSerializer(NamespacedKey.fromString(key));
    }

    @NotNull List<IRegisteredTypeSerializer<?>> getRegisteredSerializers();

    // CONFIGURATION LOADER //

    static @NotNull JacksonConfigurationLoader.Builder createConfigurateLoader() {

        List<IDatabase.IRegisteredTypeSerializer<?>> regs = SbdsProvider.getInstance().getDatabase().getRegisteredSerializers();
        Consumer<TypeSerializerCollection.Builder> consumer = builder -> regs.forEach(reg -> пердуляция(builder, reg));

        return JacksonConfigurationLoader.builder()
                .defaultOptions(opt -> opt.serializers(consumer));

    }

    // Цей метод необхідний, щоб перетворити ? в фіксовану типізацію, оскільки цього потребує метод build.register(Class<T> clazz, TypeSerializer<T>)
    private static <T> void пердуляция(@NotNull TypeSerializerCollection.Builder builder, @NotNull IDatabase.IRegisteredTypeSerializer<T> serializer) {
        builder.register(serializer.getClazz(), serializer.getSerializer());
    }

    // RECORD //

    interface IRegisteredTypeSerializer<T> {

        @NotNull Registration<IRegisteredTypeSerializer<T>> getRegistration();

        @NotNull TypeSerializer<T> getSerializer();

        @NotNull Class<T> getClazz();

    }

}
