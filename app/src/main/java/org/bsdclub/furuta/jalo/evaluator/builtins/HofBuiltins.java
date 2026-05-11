package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bsdclub.furuta.jalo.evaluator.Environment;
import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloBuiltinFunction;
import org.bsdclub.furuta.jalo.value.JaloFunction;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** Higher-order function built-ins. */
public final class HofBuiltins {
    private HofBuiltins() { }

    /**
     * Registers higher-order built-ins.
     *
     * @param registry built-in registry
     * @param evaluator evaluator for invoking closures
     */
    public static void registerAll(BuiltinRegistry registry, Evaluator evaluator) {
        registry.register("map", (args, env) -> {
            requireArity("map", args, 2);
            JaloArray coll = requireArray("map", args.get(1));
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < coll.size(); i++) out = out.append(callFn(args.get(0), List.of(coll.get(i)), env, evaluator));
            return out;
        });
        registry.register("filter", (args, env) -> filterLike("filter", args, env, evaluator, true));
        registry.register("remove", (args, env) -> filterLike("remove", args, env, evaluator, false));
        registry.register("reduce", (args, env) -> {
            requireArity("reduce", args, 3);
            JaloArray coll = requireArray("reduce", args.get(2));
            JaloValue acc = args.get(1);
            for (int i = 0; i < coll.size(); i++) acc = callFn(args.get(0), List.of(acc, coll.get(i)), env, evaluator);
            return acc;
        });
        registry.register("reduce-right", (args, env) -> {
            requireArity("reduce-right", args, 3);
            JaloArray coll = requireArray("reduce-right", args.get(2));
            JaloValue acc = args.get(1);
            for (int i = coll.size() - 1; i >= 0; i--) acc = callFn(args.get(0), List.of(coll.get(i), acc), env, evaluator);
            return acc;
        });
        registry.register("apply", (args, env) -> {
            requireArity("apply", args, 2);
            JaloArray arr = requireArray("apply", args.get(1));
            List<JaloValue> vals = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) vals.add(arr.get(i));
            return callFn(args.get(0), vals, env, evaluator);
        });
        registry.register("identity", (args, env) -> {
            requireArity("identity", args, 1);
            return args.get(0);
        });
        registry.register("not", (args, env) -> {
            requireArity("not", args, 1);
            return truthy(args.get(0)) ? JaloBool.FALSE : JaloBool.TRUE;
        });
        registry.register("every?", (args, env) -> {
            requireArity("every?", args, 2);
            JaloArray coll = requireArray("every?", args.get(1));
            for (int i = 0; i < coll.size(); i++) if (!truthy(callFn(args.get(0), List.of(coll.get(i)), env, evaluator))) return JaloBool.FALSE;
            return JaloBool.TRUE;
        });
        registry.register("some", (args, env) -> {
            requireArity("some", args, 2);
            JaloArray coll = requireArray("some", args.get(1));
            for (int i = 0; i < coll.size(); i++) {
                JaloValue v = callFn(args.get(0), List.of(coll.get(i)), env, evaluator);
                if (truthy(v)) return v;
            }
            return JaloNull.INSTANCE;
        });
        registry.register("not-any?", (args, env) -> {
            requireArity("not-any?", args, 2);
            JaloArray coll = requireArray("not-any?", args.get(1));
            for (int i = 0; i < coll.size(); i++) if (truthy(callFn(args.get(0), List.of(coll.get(i)), env, evaluator))) return JaloBool.FALSE;
            return JaloBool.TRUE;
        });
        registry.register("take", (args, env) -> takeDrop(args, true));
        registry.register("drop", (args, env) -> takeDrop(args, false));
        registry.register("mapcat", (args, env) -> {
            requireArity("mapcat", args, 2);
            JaloArray coll = requireArray("mapcat", args.get(1));
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < coll.size(); i++) {
                JaloArray mapped = requireArray("mapcat", callFn(args.get(0), List.of(coll.get(i)), env, evaluator));
                for (int j = 0; j < mapped.size(); j++) out = out.append(mapped.get(j));
            }
            return out;
        });
        registry.register("keep", (args, env) -> {
            requireArity("keep", args, 2);
            JaloArray coll = requireArray("keep", args.get(1));
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < coll.size(); i++) {
                JaloValue mapped = callFn(args.get(0), List.of(coll.get(i)), env, evaluator);
                if (mapped != JaloNull.INSTANCE) out = out.append(mapped);
            }
            return out;
        });
        registry.register("take-while", (args, env) -> {
            requireArity("take-while", args, 2);
            JaloArray coll = requireArray("take-while", args.get(1));
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < coll.size(); i++) {
                if (!truthy(callFn(args.get(0), List.of(coll.get(i)), env, evaluator))) break;
                out = out.append(coll.get(i));
            }
            return out;
        });
        registry.register("drop-while", (args, env) -> {
            requireArity("drop-while", args, 2);
            JaloArray coll = requireArray("drop-while", args.get(1));
            int i = 0;
            while (i < coll.size() && truthy(callFn(args.get(0), List.of(coll.get(i)), env, evaluator))) i++;
            JaloArray out = JaloArray.empty();
            for (; i < coll.size(); i++) out = out.append(coll.get(i));
            return out;
        });
        registry.register("comp", (args, env) -> {
            if (args.isEmpty()) throw error("Wrong arity for comp");
            return new JaloBuiltinFunction("comp", (args2, env2) -> {
                JaloValue result = callFn(args.get(args.size() - 1), args2, env2, evaluator);
                for (int i = args.size() - 2; i >= 0; i--) result = callFn(args.get(i), List.of(result), env2, evaluator);
                return result;
            });
        });
        registry.register("partial", (args, env) -> {
            if (args.isEmpty()) throw error("Wrong arity for partial");
            JaloValue f = args.get(0);
            List<JaloValue> fixed = new ArrayList<>(args.subList(1, args.size()));
            return new JaloBuiltinFunction("partial", (args2, env2) -> {
                List<JaloValue> full = new ArrayList<>(fixed);
                full.addAll(args2);
                return callFn(f, full, env2, evaluator);
            });
        });
        registry.register("constantly", (args, env) -> {
            requireArity("constantly", args, 1);
            JaloValue x = args.get(0);
            return new JaloBuiltinFunction("constantly", (args2, env2) -> x);
        });
        registry.register("complement", (args, env) -> {
            requireArity("complement", args, 1);
            JaloValue pred = args.get(0);
            return new JaloBuiltinFunction("complement", (args2, env2) ->
                truthy(callFn(pred, args2, env2, evaluator)) ? JaloBool.FALSE : JaloBool.TRUE);
        });
        registry.register("map-keys", (args, env) -> {
            requireArity("map-keys", args, 2);
            JaloMap map = requireMap("map-keys", args.get(1));
            JaloMap out = JaloMap.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) {
                JaloValue mapped = callFn(args.get(0), List.of(new JaloString(e.getKey())), env, evaluator);
                if (!(mapped instanceof JaloString s)) throw error("map-keys: key must be string");
                out = out.put(s.value(), e.getValue());
            }
            return out;
        });
        registry.register("map-vals", (args, env) -> {
            requireArity("map-vals", args, 2);
            JaloMap map = requireMap("map-vals", args.get(1));
            JaloMap out = JaloMap.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) out = out.put(e.getKey(), callFn(args.get(0), List.of(e.getValue()), env, evaluator));
            return out;
        });
        registry.register("not-empty", (args, env) -> {
            requireArity("not-empty", args, 1);
            JaloValue v = args.get(0);
            if (v instanceof JaloArray a) return a.size() == 0 ? JaloNull.INSTANCE : a;
            if (v instanceof JaloMap m) return m.size() == 0 ? JaloNull.INSTANCE : m;
            throw error("not-empty: expected array or map");
        });
    }

    private static JaloValue takeDrop(List<JaloValue> args, boolean take) {
        requireArity(take ? "take" : "drop", args, 2);
        int n = requireInt(args.get(0));
        JaloArray coll = requireArray(take ? "take" : "drop", args.get(1));
        int from = take ? 0 : Math.max(0, n);
        int to = take ? Math.min(Math.max(0, n), coll.size()) : coll.size();
        JaloArray out = JaloArray.empty();
        for (int i = from; i < to; i++) out = out.append(coll.get(i));
        return out;
    }

    private static JaloValue filterLike(String name, List<JaloValue> args, Environment env, Evaluator evaluator, boolean keepTruthy) {
        requireArity(name, args, 2);
        JaloArray coll = requireArray(name, args.get(1));
        JaloArray out = JaloArray.empty();
        for (int i = 0; i < coll.size(); i++) {
            JaloValue v = coll.get(i);
            boolean ok = truthy(callFn(args.get(0), List.of(v), env, evaluator));
            if ((keepTruthy && ok) || (!keepTruthy && !ok)) out = out.append(v);
        }
        return out;
    }

    private static JaloValue callFn(JaloValue f, List<JaloValue> args, Environment env, Evaluator evaluator) {
        if (f instanceof JaloFunction fn) return fn.apply(args, evaluator);
        if (f instanceof JaloBuiltinFunction bfn) return bfn.fn().apply(args, env);
        throw error("not a function: " + (f == null ? "null" : f.getClass().getSimpleName()));
    }

    private static boolean truthy(JaloValue v) {
        return !(v == JaloNull.INSTANCE || JaloBool.FALSE.equals(v));
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static JaloMap requireMap(String name, JaloValue value) {
        if (value instanceof JaloMap map) return map;
        throw error(name + ": expected map");
    }

    private static int requireInt(JaloValue v) {
        if (v instanceof JaloInt i) return i.value();
        throw error("expected int");
    }

    private static void requireArity(String name, List<JaloValue> args, int n) {
        if (args.size() != n) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String msg) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(msg));
    }
}
