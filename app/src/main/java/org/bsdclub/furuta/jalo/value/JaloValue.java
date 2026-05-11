package org.bsdclub.furuta.jalo.value;

/**
 * Sealed top-level type for all jalo runtime values.
 *
 * <p>Layer: Value (per DESIGN.md §1 architecture table).
 * Permits exactly 9 concrete value types.
 *
 * @see JaloNull
 * @see JaloBool
 * @see JaloNumber
 * @see JaloString
 * @see JaloArray
 * @see JaloMap
 * @see JaloInt
 * @see JaloLong
 * @see JaloFunction
 */
public sealed interface JaloValue
    permits JaloNull, JaloBool, JaloNumber, JaloString, JaloArray, JaloMap, JaloInt, JaloLong, JaloFunction {
}
