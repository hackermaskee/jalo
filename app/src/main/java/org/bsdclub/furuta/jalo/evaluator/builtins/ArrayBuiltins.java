package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Array-related built-in functions.
 */
public final class ArrayBuiltins {
    private ArrayBuiltins() { }

    /**
     * Registers array built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("count", (args, env) -> {
            requireArity("count", args, 1);
            JaloValue v = args.get(0);
            if (v instanceof JsonArray arr) return new JaloInt(arr.size());
            if (v instanceof org.bsdclub.furuta.jalo.json.JsonObject obj) return new JaloInt(obj.size());
            throw error("count: expected array or map");
        });
        registry.register("conj", (args, env) -> {
            requireMinArity("conj", args, 2);
            JsonArray arr = requireArray("conj", args.get(0));
            JsonArray out = arr;
            for (int i = 1; i < args.size(); i++) out = out.append(toJsonValue(args.get(i)));
            return out;
        });
        registry.register("get", (args, env) -> {
            requireArity("get", args, 2);
            JsonArray arr = requireArray("get", args.get(0));
            int i = requireInt("get", args.get(1));
            if (i < 0 || i >= arr.size()) return JsonNull.INSTANCE;
            return arr.get(i);
        });
        registry.register("nth", (args, env) -> {
            requireArity("nth", args, 2);
            JsonArray arr = requireArray("nth", args.get(0));
            int i = requireInt("nth", args.get(1));
            if (i < 0 || i >= arr.size()) throw error("nth: index out of range: " + i);
            return arr.get(i);
        });
        registry.register("first", (args, env) -> {
            requireArity("first", args, 1);
            JsonArray arr = requireArray("first", args.get(0));
            return arr.size() == 0 ? JsonNull.INSTANCE : arr.get(0);
        });
        registry.register("rest", (args, env) -> {
            requireArity("rest", args, 1);
            JsonArray arr = requireArray("rest", args.get(0));
            JsonArray out = JsonArray.empty();
            for (int i = 1; i < arr.size(); i++) out = out.append(arr.get(i));
            return out;
        });
        registry.register("last", (args, env) -> {
            requireArity("last", args, 1);
            JsonArray arr = requireArray("last", args.get(0));
            return arr.size() == 0 ? JsonNull.INSTANCE : arr.get(arr.size() - 1);
        });
        registry.register("cons", (args, env) -> {
            requireArity("cons", args, 2);
            JsonArray arr = requireArray("cons", args.get(1));
            JsonArray out = JsonArray.empty().append(toJsonValue(args.get(0)));
            for (int i = 0; i < arr.size(); i++) out = out.append(arr.get(i));
            return out;
        });
        registry.register("concat", (args, env) -> {
            JsonArray out = JsonArray.empty();
            for (JaloValue arg : args) {
                JsonArray arr = requireArray("concat", arg);
                for (int i = 0; i < arr.size(); i++) out = out.append(arr.get(i));
            }
            return out;
        });
        registry.register("reverse", (args, env) -> {
            requireArity("reverse", args, 1);
            JsonArray arr = requireArray("reverse", args.get(0));
            JsonArray out = JsonArray.empty();
            for (int i = arr.size() - 1; i >= 0; i--) out = out.append(arr.get(i));
            return out;
        });
        registry.register("sort", (args, env) -> {
            requireArity("sort", args, 1);
            JsonArray arr = requireArray("sort", args.get(0));
            List<JsonValue> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) list.add(arr.get(i));
            list.sort(ArrayBuiltins::compareValues);
            JsonArray out = JsonArray.empty();
            for (JsonValue v : list) out = out.append(v);
            return out;
        });
        registry.register("sort-by", (args, env) -> {
            requireArity("sort-by", args, 2);
            JsonArray arr = requireArray("sort-by", args.get(1));
            return arr;
        });
        registry.register("subvec", (args, env) -> {
            requireArity("subvec", args, 3);
            JsonArray arr = requireArray("subvec", args.get(0));
            int start = requireInt("subvec", args.get(1));
            int end = requireInt("subvec", args.get(2));
            if (start < 0 || end < start || end > arr.size()) throw error("subvec: index out of range");
            JsonArray out = JsonArray.empty();
            for (int i = start; i < end; i++) out = out.append(arr.get(i));
            return out;
        });
        registry.register("range", (args, env) -> {
            int start;
            int end;
            if (args.size() == 1) {
                start = 0;
                end = requireInt("range", args.get(0));
            } else if (args.size() == 2) {
                start = requireInt("range", args.get(0));
                end = requireInt("range", args.get(1));
            } else {
                throw error("Wrong arity for range");
            }
            JsonArray out = JsonArray.empty();
            for (int i = start; i < end; i++) out = out.append(new JsonNumber(i));
            return out;
        });
        registry.register("index-of", (args, env) -> {
            requireArity("index-of", args, 2);
            JsonArray arr = requireArray("index-of", args.get(0));
            JsonValue needle = toJsonValue(args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).equals(needle)) return new JaloInt(i);
            }
            return new JaloInt(-1);
        });
        registry.register("contains?", (args, env) -> {
            requireArity("contains?", args, 2);
            JsonArray arr = requireArray("contains?", args.get(0));
            JsonValue needle = toJsonValue(args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).equals(needle)) return JsonBool.TRUE;
            }
            return JsonBool.FALSE;
        });
    }

    private static int compareValues(JsonValue a, JsonValue b) {
        if (a instanceof JsonNumber x && b instanceof JsonNumber y) {
            return Double.compare(x.value(), y.value());
        }
        return Comparator.comparing(Object::toString).compare(a, b);
    }

    private static JsonArray requireArray(String name, JaloValue value) {
        if (value instanceof JsonArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static int requireInt(String name, JaloValue value) {
        if (value instanceof JaloInt n) return n.value();
        if (value instanceof JaloLong n) return (int) n.value();
        if (value instanceof JsonNumber n) return (int) n.value();
        throw error(name + ": expected int index");
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
