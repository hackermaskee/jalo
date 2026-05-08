package org.bsdclub.furuta.jalo.json;

public record JsonString(String value) implements JsonValue {
    public JsonString {
        if (value == null) {
            throw new IllegalArgumentException("JsonString value cannot be null");
        }
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
