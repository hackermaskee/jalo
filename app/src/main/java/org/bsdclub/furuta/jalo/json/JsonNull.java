package org.bsdclub.furuta.jalo.json;

/**
 * Represents the singleton JSON null value.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Provides a canonical immutable instance for null semantics in the JSON domain.
 *
 * @see JsonValue
 */
public final class JsonNull implements JsonValue {
    /**
     * Defines the singleton JSON null instance.
     */
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
    }

    /**
     * Returns true when the other object is also a JSON null value.
     *
     * @param o the object to compare
     * @return true if the object is a JsonNull instance
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof JsonNull;
    }

    /**
     * Returns the hash code shared by all JSON null instances.
     *
     * @return the constant hash code for JSON null
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Returns the JSON text form of null.
     *
     * @return the JSON literal null
     */
    @Override
    public String toString() {
        return "null";
    }
}
