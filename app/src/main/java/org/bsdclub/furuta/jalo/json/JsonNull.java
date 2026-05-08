package org.bsdclub.furuta.jalo.json;

public final class JsonNull implements JsonValue {
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonNull;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "null";
    }
}
