package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import java.util.Map;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Map-related built-in functions.
 */
public final class MapBuiltins {
    private MapBuiltins() { }

    /**
     * Registers map built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("assoc", (args, env) -> {
            requireMinArity("assoc", args, 3);
            if (((args.size() - 1) % 2) != 0) throw error("assoc: requires key/value pairs");
            JsonObject map = requireMap("assoc", args.get(0));
            for (int i = 1; i < args.size(); i += 2) {
                String key = requireString("assoc", args.get(i));
                map = map.put(key, toJsonValue(args.get(i + 1)));
            }
            return map;
        });
        registry.register("dissoc", (args, env) -> {
            requireArity("dissoc", args, 2);
            JsonObject map = requireMap("dissoc", args.get(0));
            String key = requireString("dissoc", args.get(1));
            return new JsonObject(map.entries().without(key));
        });
        registry.register("keys", (args, env) -> {
            requireArity("keys", args, 1);
            JsonObject map = requireMap("keys", args.get(0));
            JsonArray out = JsonArray.empty();
            for (Map.Entry<String, JsonValue> e : map.entries().entrySet()) out = out.append(new JsonString(e.getKey()));
            return out;
        });
        registry.register("vals", (args, env) -> {
            requireArity("vals", args, 1);
            JsonObject map = requireMap("vals", args.get(0));
            JsonArray out = JsonArray.empty();
            for (Map.Entry<String, JsonValue> e : map.entries().entrySet()) out = out.append(e.getValue());
            return out;
        });
        registry.register("entries", (args, env) -> {
            requireArity("entries", args, 1);
            JsonObject map = requireMap("entries", args.get(0));
            JsonArray out = JsonArray.empty();
            for (Map.Entry<String, JsonValue> e : map.entries().entrySet()) {
                out = out.append(JsonArray.of(new JsonString(e.getKey()), e.getValue()));
            }
            return out;
        });
        registry.register("from-entries", (args, env) -> {
            requireArity("from-entries", args, 1);
            JsonArray arr = requireArray("from-entries", args.get(0));
            JsonObject out = JsonObject.empty();
            for (int i = 0; i < arr.size(); i++) {
                JsonArray ent = requireArray("from-entries", arr.get(i));
                if (ent.size() != 2 || !(ent.get(0) instanceof JsonString key)) {
                    throw error("from-entries: entry must be [string value]");
                }
                out = out.put(key.value(), ent.get(1));
            }
            return out;
        });
        registry.register("merge", (args, env) -> {
            JsonObject out = JsonObject.empty();
            for (JaloValue arg : args) {
                JsonObject map = requireMap("merge", arg);
                for (Map.Entry<String, JsonValue> e : map.entries().entrySet()) out = out.put(e.getKey(), e.getValue());
            }
            return out;
        });
        registry.register("update", (args, env) -> requireMap("update", args.get(0)));
        registry.register("select-keys", (args, env) -> {
            requireArity("select-keys", args, 2);
            JsonObject map = requireMap("select-keys", args.get(0));
            JsonArray keys = requireArray("select-keys", args.get(1));
            JsonObject out = JsonObject.empty();
            for (int i = 0; i < keys.size(); i++) {
                String key = requireString("select-keys", keys.get(i));
                JsonValue val = map.get(key);
                if (val != null) out = out.put(key, val);
            }
            return out;
        });
    }

    private static JsonObject requireMap(String name, JaloValue value) {
        if (value instanceof JsonObject obj) return obj;
        throw error(name + ": expected map");
    }

    private static JsonArray requireArray(String name, JaloValue value) {
        if (value instanceof JsonArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static String requireString(String name, JaloValue value) {
        if (value instanceof JsonString s) return s.value();
        throw error(name + ": expected string key");
    }

    private static JsonValue toJsonValue(JaloValue value) {
        if (value instanceof JsonValue jsonValue) return jsonValue;
        if (value instanceof JaloInt n) return new JsonNumber(n.value());
        if (value instanceof JaloLong n) return new JsonNumber(n.value());
        throw error("expected JSON-compatible value");
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) throw error("Wrong arity for " + name);
    }

    private static void requireMinArity(String name, List<JaloValue> args, int arity) {
        if (args.size() < arity) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String message) {
        return new JaloEffectSignal(new JsonString("error"), new JsonString(message));
    }
}
