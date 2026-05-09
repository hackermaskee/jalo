package org.bsdclub.furuta.jalo.value;

/**
 * Represents a jalo integer primitive backed by {@code long}.
 *
 * <p>Layer: Value (per DESIGN.md §1 architecture table).
 * Provides a widened numeric runtime value that is part of jalo but not directly representable in the JSON model.
 *
 * @param value the long primitive value
 * @see JaloValue
 */
public record JaloLong(long value) implements JaloValue {
}
