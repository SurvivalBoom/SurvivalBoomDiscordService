package net.survivalboom.sbds.api.utils;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

    @Override
    default void accept(T t) {

        try {
            acceptThrowing(t);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    void acceptThrowing(T t) throws Throwable;


    static @NotNull <V> ThrowingConsumer<V> create(@NotNull ThrowingConsumer<V> supplier) {
        return supplier;
    }

}
