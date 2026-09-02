package net.survivalboom.sbds.api.utils.map;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class WeakHashMap<K, V> implements Map<K, V> {

    private final ConcurrentHashMap<K, WeakReference<V>> map = new ConcurrentHashMap<>();

    private final ReentrantLock cleanupLock = new ReentrantLock();

    @Override
    public int size() {
        cleanUp();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        cleanUp();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {

        WeakReference<V> ref = map.get(key);
        V value = ref == null ? null : ref.get();

        if (value == null && ref != null) {
            map.remove(key);
            return false;
        }

        return value != null;

    }

    @Override
    public boolean containsValue(Object value) {

        if (value == null) {
            return false;
        }

        cleanUp();

        for (WeakReference<V> ref : map.values()) {
            V val = ref.get();
            if (val != null && val.equals(value)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public V get(Object key) {

        WeakReference<V> ref = map.get(key);

        if (ref == null) {
            return null;
        }

        V value = ref.get();
        if (value == null) {
            map.remove(key);
        }

        return value;

    }

    @Override
    public V put(K key, V value) {

        if (key == null || value == null) {
            return null;
        }

        WeakReference<V> prev = map.put(key, new WeakReference<>(value));

        cleanUp();

        return prev == null ? null : prev.get();

    }

    @Override
    public V remove(Object key) {
        WeakReference<V> ref = map.remove(key);
        return ref == null ? null : ref.get();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        cleanUp();
        return map.keySet();
    }

    @Override
    public Collection<V> values() {

        cleanUp();

        List<V> list = new ArrayList<>();
        for (WeakReference<V> ref : map.values()) {

            V value = ref.get();
            if (value != null) {
                list.add(value);
            }

        }

        return list;

    }

    @Override
    public Set<Entry<K, V>> entrySet() {

        cleanUp();

        Set<Entry<K, V>> set = new HashSet<>();
        for (var entry : map.entrySet()) {

            V value = entry.getValue().get();
            if (value != null) {
                set.add(new AbstractMap.SimpleEntry<>(entry.getKey(), value));
            }

        }

        return set;

    }

    private void cleanUp() {

        if (!cleanupLock.tryLock()) {
            return;
        }

        try {
            map.entrySet().removeIf(e -> e.getValue().get() == null);
        }

        finally {
            cleanupLock.unlock();
        }

    }

}
