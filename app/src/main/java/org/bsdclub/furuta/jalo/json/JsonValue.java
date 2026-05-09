package org.bsdclub.furuta.jalo.json;

import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Represents the shared supertype of all JSON value variants.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Defines the six JSON runtime forms used by the parser and evaluator layers.
 *
 * @see JsonNull
 * @see JsonBool
 * @see JsonNumber
 * @see JsonString
 * @see JsonArray
 * @see JsonObject
 */
public sealed interface JsonValue extends JaloValue permits JsonNull, JsonBool, JsonNumber, JsonString, JsonArray, JsonObject {
}
