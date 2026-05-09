package org.bsdclub.furuta.jalo.json;

/**
 * Represents a JSON string value.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Provides immutable string storage with a non-null construction contract.
 *
 * @param value the underlying string value
 * @see JsonValue
 */
public record JsonString(String value) implements JsonValue {
    /**
     * Validates constructor arguments for JSON string creation.
     *
     * @throws IllegalArgumentException if the provided value is null
     */
    public JsonString {
        if (value == null) {
            throw new IllegalArgumentException("JsonString value cannot be null");
        }
    }

    /**
     * Returns the JSON text form wrapped in double quotes.
     *
     * @return the quoted JSON string literal
     */
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
