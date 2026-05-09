package org.bsdclub.furuta.jalo.json;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentHashMap;

/**
 * Represents an immutable JSON object.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Provides map-like key-value storage through a persistent hash map without in-place mutation.
 *
 * @see JsonValue
 */
public final class JsonObject implements JsonValue {
    private final PersistentHashMap<String, JsonValue> entries;

    /**
     * Creates a JSON object from the provided persistent hash map.
     *
     * @param entries the persistent key-value entries
     */
    public JsonObject(PersistentHashMap<String, JsonValue> entries) {
        this.entries = entries;
    }

    /**
     * Returns an empty JSON object.
     *
     * @return a JSON object with zero key-value pairs
     */
    public static JsonObject empty() {
        return new JsonObject(PersistentHashMap.empty());
    }

    /**
     * Returns the underlying persistent hash map of entries.
     *
     * @return the persistent map storing this object's entries
     */
    public PersistentHashMap<String, JsonValue> entries() {
        return entries;
    }

    /**
     * Returns the number of key-value pairs in this object.
     *
     * @return the object size
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns the value associated with the given key, or null when absent.
     *
     * @param key the key to look up
     * @return the mapped JSON value, or null if no mapping exists
     */
    public JsonValue get(String key) {
        return entries.get(key);
    }

    /**
     * Returns true when this object contains the given key.
     *
     * @param key the key to check
     * @return true if a mapping exists for the given key
     */
    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    /**
     * Returns a new object with the given key-value pair added or updated.
     *
     * @param key the key to add or update
     * @param v the value to associate with the key
     * @return a new JSON object with the updated mapping
     */
    public JsonObject put(String key, JsonValue v) {
        return new JsonObject(entries.assoc(key, v));
    }

    /**
     * Returns true when the other object is a JSON object with equal entries.
     *
     * @param o the object to compare
     * @return true if the compared object has equal persistent map contents
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof JsonObject obj && entries.equals(obj.entries);
    }

    /**
     * Returns the hash code of this object based on its entries.
     *
     * @return the hash code delegated from the underlying persistent hash map
     */
    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    /**
     * Returns the JSON-like text form of this object with keys in sorted order.
     *
     * @implNote Keys are sorted alphabetically for stable output regardless of internal PersistentHashMap iteration order.
     * @return the brace-delimited key-value representation with deterministic key ordering
     */
    @Override
    public String toString() {
        return "{" + entries.entrySet().stream()
            .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", ")) + "}";
    }
}
