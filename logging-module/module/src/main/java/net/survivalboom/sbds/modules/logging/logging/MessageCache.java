package net.survivalboom.sbds.modules.logging.logging;

import net.survivalboom.sbds.modules.logging.api.ILoggedMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCache {

    private final Map<Long, ILoggedMessage> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public MessageCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    public void put(ILoggedMessage message) {
        cache.put(message.getMessageId(), message);
    }

    public ILoggedMessage get(long messageId) {
        return cache.get(messageId);
    }

    public void cleanupOldMessages() {
        long currentTime = System.currentTimeMillis();
        cache.values().removeIf(message -> (currentTime - message.getTimestamp()) > ttlMillis);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}