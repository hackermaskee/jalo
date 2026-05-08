package org.bsdclub.furuta.jalo.json;

import java.util.Comparator;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentHashMap;

public final class JsonObject implements JsonValue {
    private final PersistentHashMap<String, JsonValue> entries;

    public JsonObject(PersistentHashMap<String, JsonValue> entries) {
        this.entries = entries;
    }

    public static JsonObject empty() {
        return new JsonObject(PersistentHashMap.empty());
    }

    public PersistentHashMap<String, JsonValue> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    public JsonValue get(String key) {
        return entries.get(key);
    }

    public boolean containsKey(String key) {
        return entries.containsKey(key);
    }

    public JsonObject put(String key, JsonValue v) {
        return new JsonObject(entries.assoc(key, v));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonObject obj && entries.equals(obj.entries);
    }

    @Override
    public int hashCode() {
        return entries.hashCode();
    }

    @Override
    public String toString() {
        return "{" + entries.entrySet().stream()
            .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", ")) + "}";
    }
}
