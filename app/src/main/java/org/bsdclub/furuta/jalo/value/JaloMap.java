package org.bsdclub.furuta.jalo.value;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentHashMap;

/** Immutable map from string keys to jalo values. */
public final class JaloMap implements JaloValue {
    private final PersistentHashMap<String, JaloValue> entries;

    /**
     * Creates a map from persistent hash map.
     *
     * @param entries persistent hash map of string-to-JaloValue entries
     */
    public JaloMap(PersistentHashMap<String, JaloValue> entries) { this.entries = entries; }

    /**
     * Returns empty map.
     *
     * @return empty JaloMap
     */
    public static JaloMap empty() { return new JaloMap(PersistentHashMap.empty()); }

    /**
     * Returns underlying entries.
     *
     * @return persistent hash map of entries
     */
    public PersistentHashMap<String, JaloValue> entries() { return entries; }
    public int size() { return entries.size(); }
    public JaloValue get(String key) { return entries.get(key); }
    public boolean containsKey(String key) { return entries.containsKey(key); }

    /**
     * Returns new map with key mapped to value.
     *
     * @param key string key
     * @param v value to associate with the key
     * @return new JaloMap with the key-value pair added or updated
     */
    public JaloMap put(String key, JaloValue v) {
        return new JaloMap(entries.assoc(key, v));
    }

    @Override public boolean equals(Object o) {
        return o instanceof JaloMap m && entries.equals(m.entries);
    }

    @Override public int hashCode() { return entries.hashCode(); }

    @Override public String toString() {
        return "{" + entries.entrySet().stream()
            .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", ")) + "}";
    }
}
