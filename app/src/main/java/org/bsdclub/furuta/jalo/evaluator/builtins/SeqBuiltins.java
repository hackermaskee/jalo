package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;

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
            if (v instanceof JaloArray arr) return arr.size() == 0 ? JaloBool.TRUE : JaloBool.FALSE;
            if (v instanceof JaloMap obj) return obj.size() == 0 ? JaloBool.TRUE : JaloBool.FALSE;
            throw error("empty?: expected array or map");
        });
        registry.register("get-in", (args, env) -> {
            requireArity("get-in", args, 2);
            JaloValue cur = args.get(0);
            JaloArray path = requireArray("get-in", args.get(1));
            for (int i = 0; i < path.size(); i++) {
                JaloValue p = path.get(i);
                if (cur instanceof JaloMap obj && p instanceof JaloString key) {
                    JaloValue next = obj.get(key.value());
                    if (next == null) return JaloNull.INSTANCE;
                    cur = next;
                    continue;
                }
                if (cur instanceof JaloArray arr) {
                    int idx = requireInt("get-in", p);
                    if (idx < 0 || idx >= arr.size()) return JaloNull.INSTANCE;
                    cur = arr.get(idx);
                    continue;
                }
                return JaloNull.INSTANCE;
            }
            return cur;
        });
        registry.register("assoc-in", (args, env) -> args.get(0));
        registry.register("update-in", (args, env) -> args.get(0));
        registry.register("dissoc-in", (args, env) -> args.get(0));
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) return arr;
        throw error(name + ": expected array");
    }

    private static int requireInt(String name, JaloValue value) {
        if (value instanceof JaloInt n) return n.value();
        if (value instanceof JaloLong n) return (int) n.value();
        if (value instanceof JaloNumber n) return (int) n.value();
        throw error(name + ": expected int");
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String message) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(message));
    }
}
