package com.iit.dashboard2022.util.mapping;

import com.google.gson.JsonElement;

import java.util.concurrent.CompletableFuture;

public interface JsonHandler {
    /**
     * Reads json mapping from input
     *
     * @return {@link JsonElement} if exists, null if not
     */
    public CompletableFuture<JsonElement> read();

    /**
     * Writes json mapping to output
     *
     * @param element {@link JsonElement}
     */
    public void write(JsonElement element);

    /**
     * Deletes json entry
     *
     * @return True if success, false if not
     */
    public CompletableFuture<Boolean> delete();
}
