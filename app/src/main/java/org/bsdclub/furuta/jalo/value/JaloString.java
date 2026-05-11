package org.bsdclub.furuta.jalo.value;

/**
 * Represents a string value in jalo runtime.
 *
 * @param value the string payload; must not be null
 */
public record JaloString(String value) implements JaloValue {
    /** Creates an immutable jalo string. */
    public JaloString {
        if (value == null) {
            throw new IllegalArgumentException("JaloString value cannot be null");
        }
    }

    @Override public String toString() { return "\"" + value + "\""; }
}
