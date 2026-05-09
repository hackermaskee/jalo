package org.bsdclub.furuta.jalo.value;

import org.bsdclub.furuta.jalo.json.JsonValue;

/**
 * Represents the top-level abstraction of all jalo runtime values.
 *
 * <p>Layer: Value (per DESIGN.md §1 architecture table).
 * Defines a shared marker type for both JSON values and jalo-only numeric primitives.
 *
 * @see JsonValue
 * @see JaloInt
 * @see JaloLong
 */
public interface JaloValue {
}
