package net.survivalboom.sbds.core.utils.placeholders;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.survivalboom.sbds.api.database.guildconfig.GuildConfigField;
import net.survivalboom.sbds.api.modules.IModule;
import net.survivalboom.sbds.api.translations.ITranslation;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholderRegistry;
import net.survivalboom.sbds.api.registrations.Registration;
import net.survivalboom.sbds.api.utils.NamespacedKey;
import net.survivalboom.sbds.api.utils.placeholders.IPlaceholders;
import net.survivalboom.sbds.api.utils.valid.Manager;
import net.survivalboom.sbds.core.SBDS;
import net.survivalboom.sbds.core.registration.InternalRegistrationManager;
import net.survivalboom.sbds.core.utils.placeholders.wrappers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;

public class PlaceholderRegistry extends Manager implements IPlaceholderRegistry {

    private final InternalRegistrationManager<IRegisteredPlaceholderProvider<?>> registry;


    public PlaceholderRegistry(@NotNull SBDS sbds) {
        this.registry = new InternalRegistrationManager<>(this, null, sbds.getRegistrationRegistry());
    }


    @Override
    protected void init0() {

        registry.init();

        registerProvider0(null, IModule.class, ModulePlaceholder::new);
        registerProvider0(null, User.class, UserPlaceholder::new);
        registerProvider0(null, Member.class, MemberPlaceholder::new);
        registerProvider0(null, Guild.class, GuildPlaceholder::new);
        registerProvider0(null, Channel.class, ChannelPlaceholder::new);
        registerProvider0(null, ITranslation.class, TranslationPlaceholder::new);
        registerProvider0(null, GuildConfigField.class, GuildConfigFieldPlaceholder::new);
        registerProvider0(null, TimeZone.class, TimeZonePlaceholder::new);

    }

    @Override
    protected void shutdown0() {
        registry.shutdown();
    }

    //
    // REG
    //

    @Override
    public @NotNull <V> IRegisteredPlaceholderProvider<V> registerProvider(
            @NotNull IModule module,
            @NotNull Class<V> clazz,
            @NotNull Function<V, IPlaceholders> function
    ) {
        Objects.requireNonNull(module, "module == null");
        return registerProvider0(module, clazz, function);
    }

    @SuppressWarnings("unchecked")
    public @NotNull <V> IRegisteredPlaceholderProvider<V> registerProvider0(
            @Nullable IModule module,
            @NotNull Class<V> clazz,
            @NotNull Function<V, IPlaceholders> function
    ) {

        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(function, "function == null");
        checkValid();

        boolean registered = getProviders().stream().anyMatch(reg -> reg.getClazz().equals(clazz));
        if (registered) {
            throw new IllegalStateException("Provider for `" + clazz + "` already exists");
        }

        RegisteredPlaceholderProvider<V> reg = new RegisteredPlaceholderProvider<>(clazz, function);
        reg.registration = (Registration<IRegisteredPlaceholderProvider<V>>) (Registration<?>) registry.register0(module, clazz.getSimpleName(), reg);

        return reg;

    }


    @Override
    public boolean unregisterProvider(@NotNull IRegisteredPlaceholderProvider<?> reg) {
        return registry.unregister(reg) != null;
    }

    //
    // GET
    //

    @Override
    public @NotNull List<IRegisteredPlaceholderProvider<?>> getProviders() {
        return registry.getRegisteredObjects();
    }

    @Override
    public @Nullable IRegisteredPlaceholderProvider<?> getProvider(@NotNull NamespacedKey key) {
        return registry.getRegistrationAsObject(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <V> IRegisteredPlaceholderProvider<V> getProviderFor(@NotNull V object) {

        Objects.requireNonNull(object, "object == null");
        Class<?> targetClass = object.getClass();

        return (IRegisteredPlaceholderProvider<V>) getProviders().stream()
                .filter(reg -> reg.getClazz().isAssignableFrom(targetClass))
                .min((p1, p2) -> {
                    if (p1.getClazz().equals(p2.getClazz())) return 0;
                    // Тот класс, который ближе к целевому (наследник), пойдет первым
                    return p1.getClazz().isAssignableFrom(p2.getClazz()) ? 1 : -1;
                })
                .orElse(null);

    }


    public static class RegisteredPlaceholderProvider<V> implements IRegisteredPlaceholderProvider<V> {

        private final Class<V> clazz;

        private final Function<V, IPlaceholders> function;

        private Registration<IRegisteredPlaceholderProvider<V>> registration;


        public RegisteredPlaceholderProvider(
                @NotNull Class<V> clazz,
                @NotNull Function<V, IPlaceholders> function
        ) {
            this.clazz = clazz;
            this.function = function;
        }


        @Override
        public @NotNull Class<V> getClazz() {
            return clazz;
        }

        @Override
        public @NotNull Function<V, IPlaceholders> getFunction() {
            return function;
        }

        @Override
        public @NotNull Registration<IRegisteredPlaceholderProvider<V>> getRegistration() {
            return registration;
        }

    }


}
