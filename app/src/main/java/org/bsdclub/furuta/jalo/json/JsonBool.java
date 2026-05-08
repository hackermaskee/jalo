package org.bsdclub.furuta.jalo.json;

public enum JsonBool implements JsonValue {
    TRUE,
    FALSE;

    public boolean value() {
        return this == TRUE;
    }

    @Override
    public String toString() {
        return this == TRUE ? "true" : "false";
    }
}
