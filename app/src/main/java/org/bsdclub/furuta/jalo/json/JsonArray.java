package org.bsdclub.furuta.jalo.json;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentVector;

public final class JsonArray implements JsonValue {
    private final PersistentVector<JsonValue> elements;

    public JsonArray(PersistentVector<JsonValue> elements) {
        this.elements = elements;
    }

    public static JsonArray empty() {
        return new JsonArray(PersistentVector.empty());
    }

    public static JsonArray of(JsonValue... values) {
        return new JsonArray(PersistentVector.ofIter(Arrays.asList(values)));
    }

    public PersistentVector<JsonValue> elements() {
        return elements;
    }

    public int size() {
        return elements.size();
    }

    public JsonValue get(int idx) {
        return elements.get(idx);
    }

    public JsonArray append(JsonValue v) {
        return new JsonArray(elements.append(v));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonArray a && elements.equals(a.elements);
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public String toString() {
        return "[" + elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }
}
