package org.bsdclub.furuta.jalo.json;

import org.bsdclub.furuta.jalo.value.JaloValue;

public sealed interface JsonValue extends JaloValue permits JsonNull, JsonBool, JsonNumber, JsonString, JsonArray, JsonObject {
}
