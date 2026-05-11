package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
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
            if (v instanceof JaloArray arr) return new JaloInt(arr.size());
            if (v instanceof org.bsdclub.furuta.jalo.value.JaloMap obj) return new JaloInt(obj.size());
            throw error("count: expected array or map");
        });
        registry.register("conj", (args, env) -> {
            requireMinArity("conj", args, 2);
            JaloArray arr = requireArray("conj", args.get(0));
            JaloArray out = arr;
            for (int i = 1; i < args.size(); i++) out = out.append(args.get(i));
            return out;
        });
        registry.register("get", (args, env) -> {
            requireArity("get", args, 2);
            JaloArray arr = requireArray("get", args.get(0));
            int i = requireInt("get", args.get(1));
            if (i < 0 || i >= arr.size()) return JaloNull.INSTANCE;
            return arr.get(i);
        });
        registry.register("nth", (args, env) -> {
            requireArity("nth", args, 2);
            JaloArray arr = requireArray("nth", args.get(0));
            int i = requireInt("nth", args.get(1));
            if (i < 0 || i >= arr.size()) throw error("nth: index out of range: " + i);
            return arr.get(i);
        });
        registry.register("first", (args, env) -> {
            requireArity("first", args, 1);
            JaloArray arr = requireArray("first", args.get(0));
            return arr.size() == 0 ? JaloNull.INSTANCE : arr.get(0);
        });
        registry.register("rest", (args, env) -> {
            requireArity("rest", args, 1);
            JaloArray arr = requireArray("rest", args.get(0));
            JaloArray out = JaloArray.empty();
            for (int i = 1; i < arr.size(); i++) out = out.append(arr.get(i));
            return out;
        });
        registry.register("last", (args, env) -> {
            requireArity("last", args, 1);
            JaloArray arr = requireArray("last", args.get(0));
            return arr.size() == 0 ? JaloNull.INSTANCE : arr.get(arr.size() - 1);
        });
        registry.register("cons", (args, env) -> {
            requireArity("cons", args, 2);
            JaloArray arr = requireArray("cons", args.get(1));
            JaloArray out = JaloArray.empty().append(args.get(0));
            for (int i = 0; i < arr.size(); i++) out = out.append(arr.get(i));
            return out;
        });
        registry.register("concat", (args, env) -> {
            JaloArray out = JaloArray.empty();
            for (JaloValue arg : args) {
                JaloArray arr = requireArray("concat", arg);
                for (int i = 0; i < arr.size(); i++) out = out.append(arr.get(i));
            }
            return out;
        });
        registry.register("reverse", (args, env) -> {
            requireArity("reverse", args, 1);
            JaloArray arr = requireArray("reverse", args.get(0));
            JaloArray out = JaloArray.empty();
            for (int i = arr.size() - 1; i >= 0; i--) out = out.append(arr.get(i));
            return out;
        });
        registry.register("sort", (args, env) -> {
            requireArity("sort", args, 1);
            JaloArray arr = requireArray("sort", args.get(0));
            List<JaloValue> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) list.add(arr.get(i));
            list.sort(ArrayBuiltins::compareValues);
            JaloArray out = JaloArray.empty();
            for (JaloValue v : list) out = out.append(v);
            return out;
        });
        registry.register("sort-by", (args, env) -> {
            requireArity("sort-by", args, 2);
            JaloArray arr = requireArray("sort-by", args.get(1));
            return arr;
        });
        registry.register("subvec", (args, env) -> {
            requireArity("subvec", args, 3);
            JaloArray arr = requireArray("subvec", args.get(0));
            int start = requireInt("subvec", args.get(1));
            int end = requireInt("subvec", args.get(2));
            if (start < 0 || end < start || end > arr.size()) throw error("subvec: index out of range");
            JaloArray out = JaloArray.empty();
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
            JaloArray out = JaloArray.empty();
            for (int i = start; i < end; i++) out = out.append(new JaloInt(i));
            return out;
        });
        registry.register("index-of", (args, env) -> {
            requireArity("index-of", args, 2);
            JaloArray arr = requireArray("index-of", args.get(0));
            JaloValue needle = toJsonValue(args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).equals(needle)) return new JaloInt(i);
            }
            return new JaloInt(-1);
        });
        registry.register("contains?", (args, env) -> {
            requireArity("contains?", args, 2);
            JaloArray arr = requireArray("contains?", args.get(0));
            JaloValue needle = toJsonValue(args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (arr.get(i).equals(needle)) return JaloBool.TRUE;
            }
            return JaloBool.FALSE;
        });
    }

    private static int compareValues(JaloValue a, JaloValue b) {
        if (a instanceof JaloNumber x && b instanceof JaloNumber y) {
            return Double.compare(x.value(), y.value());
        }
        return Comparator.comparing(Object::toString).compare(a, b);
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static int requireInt(String name, JaloValue value) {
        if (value instanceof JaloInt n) return n.value();
        if (value instanceof JaloLong n) return (int) n.value();
        if (value instanceof JaloNumber n) return (int) n.value();
        throw error(name + ": expected int index");
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
