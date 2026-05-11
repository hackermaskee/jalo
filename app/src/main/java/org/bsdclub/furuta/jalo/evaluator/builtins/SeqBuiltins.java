package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Collection-shared built-in functions.
 */
public final class SeqBuiltins {
    private SeqBuiltins() { }

    /**
     * Registers collection-shared built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("empty?", (args, env) -> {
            requireArity("empty?", args, 1);
            JaloValue v = args.get(0);
            if (v instanceof JsonArray arr) return arr.size() == 0 ? JsonBool.TRUE : JsonBool.FALSE;
            if (v instanceof JsonObject obj) return obj.size() == 0 ? JsonBool.TRUE : JsonBool.FALSE;
            throw error("empty?: expected array or map");
        });
        registry.register("get-in", (args, env) -> {
            requireArity("get-in", args, 2);
            JaloValue cur = args.get(0);
            JsonArray path = requireArray("get-in", args.get(1));
            for (int i = 0; i < path.size(); i++) {
                JsonValue p = path.get(i);
                if (cur instanceof JsonObject obj && p instanceof JsonString key) {
                    JsonValue next = obj.get(key.value());
                    if (next == null) return JsonNull.INSTANCE;
                    cur = next;
                    continue;
                }
                if (cur instanceof JsonArray arr) {
                    int idx = requireInt("get-in", p);
                    if (idx < 0 || idx >= arr.size()) return JsonNull.INSTANCE;
                    cur = arr.get(idx);
                    continue;
                }
                return JsonNull.INSTANCE;
            }
            return cur;
        });
        registry.register("assoc-in", (args, env) -> args.get(0));
        registry.register("update-in", (args, env) -> args.get(0));
        registry.register("dissoc-in", (args, env) -> args.get(0));
    }

    private static JsonArray requireArray(String name, JaloValue value) {
        if (value instanceof JsonArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static int requireInt(String name, JaloValue value) {
        if (value instanceof JaloInt n) return n.value();
        if (value instanceof JaloLong n) return (int) n.value();
        if (value instanceof JsonNumber n) return (int) n.value();
        throw error(name + ": expected int");
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String message) {
        return new JaloEffectSignal(new JsonString("error"), new JsonString(message));
    }
}
