package net.survivalboom.sbds.api.utils;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T t) throws Throwable;

    static <T> Consumer<T> transform(ThrowingConsumer<T> consumer) {

        if (consumer == null) {
            return null;
        }

        return t -> {

            try {
                consumer.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        };

    }

}
