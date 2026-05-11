package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import java.util.Map;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;

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
            JaloMap map = requireMap("assoc", args.get(0));
            for (int i = 1; i < args.size(); i += 2) {
                String key = requireString("assoc", args.get(i));
                map = map.put(key, toJsonValue(args.get(i + 1)));
            }
            return map;
        });
        registry.register("dissoc", (args, env) -> {
            requireArity("dissoc", args, 2);
            JaloMap map = requireMap("dissoc", args.get(0));
            String key = requireString("dissoc", args.get(1));
            return new JaloMap(map.entries().without(key));
        });
        registry.register("keys", (args, env) -> {
            requireArity("keys", args, 1);
            JaloMap map = requireMap("keys", args.get(0));
            JaloArray out = JaloArray.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) out = out.append(new JaloString(e.getKey()));
            return out;
        });
        registry.register("vals", (args, env) -> {
            requireArity("vals", args, 1);
            JaloMap map = requireMap("vals", args.get(0));
            JaloArray out = JaloArray.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) out = out.append(e.getValue());
            return out;
        });
        registry.register("entries", (args, env) -> {
            requireArity("entries", args, 1);
            JaloMap map = requireMap("entries", args.get(0));
            JaloArray out = JaloArray.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) {
                out = out.append(JaloArray.of(new JaloString(e.getKey()), e.getValue()));
            }
            return out;
        });
        registry.register("from-entries", (args, env) -> {
            requireArity("from-entries", args, 1);
            JaloArray arr = requireArray("from-entries", args.get(0));
            JaloMap out = JaloMap.empty();
            for (int i = 0; i < arr.size(); i++) {
                JaloArray ent = requireArray("from-entries", arr.get(i));
                if (ent.size() != 2 || !(ent.get(0) instanceof JaloString key)) {
                    throw error("from-entries: entry must be [string value]");
                }
                out = out.put(key.value(), ent.get(1));
            }
            return out;
        });
        registry.register("merge", (args, env) -> {
            JaloMap out = JaloMap.empty();
            for (JaloValue arg : args) {
                JaloMap map = requireMap("merge", arg);
                for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) out = out.put(e.getKey(), e.getValue());
            }
            return out;
        });
        registry.register("update", (args, env) -> requireMap("update", args.get(0)));
        registry.register("select-keys", (args, env) -> {
            requireArity("select-keys", args, 2);
            JaloMap map = requireMap("select-keys", args.get(0));
            JaloArray keys = requireArray("select-keys", args.get(1));
            JaloMap out = JaloMap.empty();
            for (int i = 0; i < keys.size(); i++) {
                String key = requireString("select-keys", keys.get(i));
                JaloValue val = map.get(key);
                if (val != null) out = out.put(key, val);
            }
            return out;
        });
    }

    private static JaloMap requireMap(String name, JaloValue value) {
        if (value instanceof JaloMap obj) return obj;
        throw error(name + ": expected map");
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static String requireString(String name, JaloValue value) {
        if (value instanceof JaloString s) return s.value();
        throw error(name + ": expected string key");
    }

    private static JaloValue toJsonValue(JaloValue value) {
        if (value instanceof JaloInt n) return new JaloNumber(n.value());
        if (value instanceof JaloLong n) return new JaloNumber(n.value());
        return value;
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) throw error("Wrong arity for " + name);
    }

    private static void requireMinArity(String name, List<JaloValue> args, int arity) {
        if (args.size() < arity) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String message) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(message));
    }
}
