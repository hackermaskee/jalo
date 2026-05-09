package org.bsdclub.furuta.jalo.json;

/**
 * Represents JSON boolean values.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Defines canonical enum variants for true and false in the JSON domain.
 *
 * @see JsonValue
 */
public enum JsonBool implements JsonValue {
    TRUE,
    FALSE;

    /**
     * Returns the primitive boolean represented by this JSON boolean.
     *
     * @return the boolean primitive value
     */
    public boolean value() {
        return this == TRUE;
    }

    /**
     * Returns the JSON text form of this boolean value.
     *
     * @return the JSON literal true or false
     */
    @Override
    public String toString() {
        return this == TRUE ? "true" : "false";
    }
}
