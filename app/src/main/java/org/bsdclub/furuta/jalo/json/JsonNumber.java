package org.bsdclub.furuta.jalo.json;

public record JsonNumber(double value) implements JsonValue {
    @Override
    public String toString() {
        return Double.toString(value);
    }
}
