package org.bsdclub.furuta.jalo.json;

/**
 * Represents a JSON number backed by {@code double}.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Provides immutable numeric storage for JSON numeric literals.
 *
 * @param value the double numeric value
 * @see JsonValue
 */
public record JsonNumber(double value) implements JsonValue {
    /**
     * Returns the JSON text form of this numeric value.
     *
     * @return the string form produced by {@link Double#toString(double)}
     */
    @Override
    public String toString() {
        return Double.toString(value);
    }

    /**
     * Returns true when this number and the other number are equal by record semantics.
     *
     * @implNote Equality follows Java record semantics based on {@code Double.doubleToLongBits}. NaN compares equal here,
     * while jalo {@code =} behavior is enforced separately by the evaluator layer.
     * @param obj the object to compare
     * @return true if both records have the same double-bit representation
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof JsonNumber other && Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }
}
