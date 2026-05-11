package org.bsdclub.furuta.jalo.value;

/**
 * Represents a double value in jalo runtime.
 *
 * @param value the IEEE-754 double-precision floating-point payload
 */
public record JaloNumber(double value) implements JaloValue {
    @Override public String toString() {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
