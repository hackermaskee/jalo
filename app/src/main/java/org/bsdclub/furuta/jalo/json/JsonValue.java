package org.bsdclub.furuta.jalo.json;

public sealed interface JsonValue permits JsonNull, JsonBool, JsonNumber, JsonString, JsonArray, JsonObject {
}
