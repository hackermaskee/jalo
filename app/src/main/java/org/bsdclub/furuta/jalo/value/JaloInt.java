package org.bsdclub.furuta.jalo.value;

/**
 * Represents a jalo integer primitive backed by {@code int}.
 *
 * <p>Layer: Value (per DESIGN.md §1 architecture table).
 * Provides a numeric runtime value that is part of jalo but not directly representable in the JSON model.
 *
 * @param value the integer primitive value
 * @see JaloValue
 */
public record JaloInt(int value) implements JaloValue {
    @Override public String toString() { return String.valueOf(value); }
}
