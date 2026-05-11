package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** I/O and serialization built-ins. */
public final class IoBuiltins {
    private IoBuiltins() { }

    /**
     * Registers I/O built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("println", (args, env) -> {
            requireArity("println", args.size(), 1);
            System.out.println(display(args.get(0)));
            return JaloNull.INSTANCE;
        });
        registry.register("print", (args, env) -> {
            requireArity("print", args.size(), 1);
            System.out.print(display(args.get(0)));
            return JaloNull.INSTANCE;
        });
        registry.register("read-line", (args, env) -> {
            requireArity("read-line", args.size(), 0);
            try {
                String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
                return line == null ? JaloNull.INSTANCE : new JaloString(line);
            } catch (IOException ex) {
                throw error("read-line failed");
            }
        });
        registry.register("to-json", (args, env) -> {
            requireArity("to-json", args.size(), 1);
            JaloValue v = args.get(0);
            if (!TypeBuiltins.isPureJson(v)) throw error("to-json: pure-json? required");
            return new JaloString(toJson(v));
        });
    }

    private static String display(JaloValue v) {
        if (v instanceof JaloString s) return s.value();
        return v.toString();
    }

    private static String toJson(JaloValue v) {
        if (v == JaloNull.INSTANCE) return "null";
        if (v instanceof JaloBool b) return b.value() ? "true" : "false";
        if (v instanceof JaloNumber n) return Double.toString(n.value());
        if (v instanceof JaloString s) return quote(s.value());
        if (v instanceof JaloArray a) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < a.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(a.get(i)));
            }
            return sb.append(']').toString();
        }
        if (v instanceof JaloMap m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JaloValue> e : m.entries().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(e.getKey())).append(':').append(toJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        throw error("to-json: pure-json? required");
    }

    private static String quote(String s) {
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static void requireArity(String name, int got, int expected) {
        if (got != expected) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String msg) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(msg));
    }
}
