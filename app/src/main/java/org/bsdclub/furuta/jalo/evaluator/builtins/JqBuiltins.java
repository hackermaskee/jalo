package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bsdclub.furuta.jalo.evaluator.Environment;
import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBuiltinFunction;
import org.bsdclub.furuta.jalo.value.JaloFunction;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** jq compatibility built-ins registered under kebab-case names. */
public final class JqBuiltins {
    private JqBuiltins() {
    }

    /**
     * Registers jq-specific built-ins (additive only).
     *
     * @param registry target built-in registry
     * @param evaluator evaluator used when built-ins invoke function values
     */
    public static void registerAll(BuiltinRegistry registry, Evaluator evaluator) {
        registry.register("recurse", (args, env) -> {
            requireArity("recurse", args, 1);
            JaloArray out = JaloArray.empty();
            for (JaloValue v : recurse(args.get(0))) {
                out = out.append(v);
            }
            return out;
        });
        registry.register("paths", (args, env) -> {
            requireArity("paths", args, 1);
            return collectPaths(args.get(0), false);
        });
        registry.register("leaf-paths", (args, env) -> {
            requireArity("leaf-paths", args, 1);
            return collectPaths(args.get(0), true);
        });
        registry.register("group-by", (args, env) -> {
            requireArity("group-by", args, 2);
            JaloArray arr = requireArray("group-by", args.get(1));
            Map<String, JaloArray> grouped = new LinkedHashMap<>();
            for (int i = 0; i < arr.size(); i++) {
                JaloValue item = arr.get(i);
                String key = callFn(args.get(0), List.of(item), env, evaluator).toString();
                grouped.putIfAbsent(key, JaloArray.empty());
                grouped.put(key, grouped.get(key).append(item));
            }
            JaloMap out = JaloMap.empty();
            for (Map.Entry<String, JaloArray> e : grouped.entrySet()) {
                out = out.put(e.getKey(), e.getValue());
            }
            return out;
        });
        registry.register("unique-by", (args, env) -> {
            requireArity("unique-by", args, 2);
            JaloArray arr = requireArray("unique-by", args.get(1));
            Set<String> seen = new LinkedHashSet<>();
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < arr.size(); i++) {
                JaloValue item = arr.get(i);
                String key = callFn(args.get(0), List.of(item), env, evaluator).toString();
                if (seen.add(key)) {
                    out = out.append(item);
                }
            }
            return out;
        });
        registry.register("min-by", (args, env) -> byExtreme("min-by", args, env, evaluator, true));
        registry.register("max-by", (args, env) -> byExtreme("max-by", args, env, evaluator, false));
        registry.register("at-csv", (args, env) -> toCsvLike("at-csv", args, ","));
        registry.register("at-tsv", (args, env) -> toCsvLike("at-tsv", args, "\t"));
        registry.register("at-html", (args, env) -> {
            requireArity("at-html", args, 1);
            return new JaloString(escapeHtml(toSimpleString(args.get(0))));
        });
        registry.register("at-uri", (args, env) -> {
            requireArity("at-uri", args, 1);
            return new JaloString(toSimpleString(args.get(0)).replace(" ", "%20"));
        });
        registry.register("at-base64", (args, env) -> {
            requireArity("at-base64", args, 1);
            String encoded = Base64.getEncoder().encodeToString(toSimpleString(args.get(0)).getBytes(StandardCharsets.UTF_8));
            return new JaloString(encoded);
        });
        registry.register("at-json", (args, env) -> {
            requireArity("at-json", args, 1);
            return new JaloString(args.get(0).toString());
        });
    }

    private static JaloValue byExtreme(String name, List<JaloValue> args, Environment env, Evaluator evaluator, boolean min) {
        requireArity(name, args, 2);
        JaloArray arr = requireArray(name, args.get(1));
        if (arr.size() == 0) {
            return JaloNull.INSTANCE;
        }
        JaloValue best = arr.get(0);
        String bestKey = callFn(args.get(0), List.of(best), env, evaluator).toString();
        for (int i = 1; i < arr.size(); i++) {
            JaloValue cand = arr.get(i);
            String key = callFn(args.get(0), List.of(cand), env, evaluator).toString();
            int cmp = key.compareTo(bestKey);
            if ((min && cmp < 0) || (!min && cmp > 0)) {
                best = cand;
                bestKey = key;
            }
        }
        return best;
    }

    private static JaloValue toCsvLike(String name, List<JaloValue> args, String sep) {
        requireArity(name, args, 1);
        JaloArray arr = requireArray(name, args.get(0));
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            parts.add(toSimpleString(arr.get(i)));
        }
        return new JaloString(String.join(sep, parts));
    }

    private static String toSimpleString(JaloValue value) {
        if (value instanceof JaloString s) {
            return s.value();
        }
        if (value instanceof JaloInt i) {
            return Integer.toString(i.value());
        }
        return value.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static List<JaloValue> recurse(JaloValue value) {
        List<JaloValue> out = new ArrayList<>();
        walk(value, out);
        return out;
    }

    private static void walk(JaloValue value, List<JaloValue> out) {
        out.add(value);
        if (value instanceof JaloMap m) {
            for (JaloValue v : m.entries().values()) {
                walk(v, out);
            }
        } else if (value instanceof JaloArray a) {
            for (int i = 0; i < a.size(); i++) {
                walk(a.get(i), out);
            }
        }
    }

    private static JaloArray collectPaths(JaloValue value, boolean leafOnly) {
        List<JaloArray> paths = new ArrayList<>();
        collect(value, JaloArray.empty(), paths, leafOnly);
        JaloArray out = JaloArray.empty();
        for (JaloArray p : paths) {
            out = out.append(p);
        }
        return out;
    }

    private static void collect(JaloValue value, JaloArray prefix, List<JaloArray> paths, boolean leafOnly) {
        if (value instanceof JaloMap m) {
            if (!leafOnly) {
                paths.add(prefix);
            }
            for (Map.Entry<String, JaloValue> e : m.entries().entrySet()) {
                collect(e.getValue(), prefix.append(new JaloString(e.getKey())), paths, leafOnly);
            }
            return;
        }
        if (value instanceof JaloArray a) {
            if (!leafOnly) {
                paths.add(prefix);
            }
            for (int i = 0; i < a.size(); i++) {
                collect(a.get(i), prefix.append(new JaloInt(i)), paths, leafOnly);
            }
            return;
        }
        paths.add(prefix);
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) {
            return arr;
        }
        throw error(name + ": expected array");
    }

    private static JaloValue callFn(JaloValue f, List<JaloValue> args, Environment env, Evaluator evaluator) {
        if (f instanceof JaloFunction fn) {
            return fn.apply(args, evaluator);
        }
        if (f instanceof JaloBuiltinFunction bfn) {
            return bfn.fn().apply(args, env);
        }
        throw error("not a function: " + (f == null ? "null" : f.getClass().getSimpleName()));
    }

    private static void requireArity(String name, List<JaloValue> args, int n) {
        if (args.size() != n) {
            throw error("Wrong arity for " + name);
        }
    }

    private static JaloEffectSignal error(String msg) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(msg));
    }
}
