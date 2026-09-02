package net.survivalboom.sbds.api.utils.http;

import net.survivalboom.sbds.api.utils.ThrowingSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class HttpFileDownloader {

    private final File destination;

    private final String url;


    private boolean used = false;

    private boolean success = false;


    public HttpFileDownloader(@NotNull String url, @NotNull File destination) {

        Objects.requireNonNull(url, "url == null");
        Objects.requireNonNull(destination, "destination = null");

        if (destination.exists() && !destination.isFile()) throw new IllegalArgumentException("Destination `" + destination.getPath() + "` is not a file");

        this.destination = destination;
        this.url = url;

    }

    public synchronized @NotNull File download() throws IOException, URISyntaxException {

        if (used) throw new IllegalStateException("cannot reuse used object");
        used = true;

        try (InputStream in = new URI(url).toURL().openStream()) {
            Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        success = true;

        return destination;

    }

    public synchronized @NotNull CompletableFuture<File> downloadAsync() {
        if (used) throw new IllegalStateException("cannot reuse used object");
        return CompletableFuture.supplyAsync(ThrowingSupplier.transform(this::download));
    }

    public @NotNull File getDestination() {
        return destination;
    }

    public boolean isSuccess() {
        return success;
    }

}
