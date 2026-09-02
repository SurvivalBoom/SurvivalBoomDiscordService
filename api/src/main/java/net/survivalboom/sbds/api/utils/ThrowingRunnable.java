package net.survivalboom.sbds.api.utils;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Throwable;

    static Runnable transform(ThrowingRunnable runnable) {

        if (runnable == null) {
            return null;
        }

        return () -> {

            try {
                runnable.run();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        };

    }

}
