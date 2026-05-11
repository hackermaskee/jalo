package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.bsdclub.furuta.jalo.value.JaloNumber;
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
        registry.register("add", (args, env) -> {
            requireArity("add", args, 1);
            JaloArray arr = requireArray("add", args.get(0));
            boolean numeric = true;
            for (int i = 0; i < arr.size(); i++) {
                if (!(arr.get(i) instanceof JaloNumber || arr.get(i) instanceof JaloInt)) {
                    numeric = false;
                    break;
                }
            }
            if (numeric) {
                double sum = 0;
                for (int i = 0; i < arr.size(); i++) {
                    JaloValue v = arr.get(i);
                    sum += (v instanceof JaloInt n) ? n.value() : ((JaloNumber) v).value();
                }
                return Math.rint(sum) == sum ? new JaloInt((int) sum) : new JaloNumber(sum);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.size(); i++) sb.append(toSimpleString(arr.get(i)));
            return new JaloString(sb.toString());
        });
        registry.register("sort-by", (args, env) -> {
            requireArity("sort-by", args, 2);
            JaloArray arr = requireArray("sort-by", args.get(1));
            List<JaloValue> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) list.add(arr.get(i));
            list.sort(Comparator.comparing(v -> callFn(args.get(0), List.of(v), env, evaluator).toString()));
            JaloArray out = JaloArray.empty();
            for (JaloValue v : list) out = out.append(v);
            return out;
        });
        registry.register("unique", (args, env) -> {
            requireArity("unique", args, 1);
            JaloArray arr = requireArray("unique", args.get(0));
            Set<JaloValue> seen = new LinkedHashSet<>();
            JaloArray out = JaloArray.empty();
            for (int i = 0; i < arr.size(); i++) {
                JaloValue item = arr.get(i);
                if (seen.add(item)) out = out.append(item);
            }
            return out;
        });
        registry.register("to-entries", (args, env) -> {
            requireArity("to-entries", args, 1);
            JaloMap map = requireMap("to-entries", args.get(0));
            JaloArray out = JaloArray.empty();
            for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) {
                out = out.append(JaloMap.empty().put("key", new JaloString(e.getKey())).put("value", e.getValue()));
            }
            return out;
        });
        registry.register("from-entries", (args, env) -> {
            requireArity("from-entries", args, 1);
            JaloArray arr = requireArray("from-entries", args.get(0));
            JaloMap out = JaloMap.empty();
            for (int i = 0; i < arr.size(); i++) {
                JaloMap ent = requireMap("from-entries", arr.get(i));
                JaloValue keyValue = ent.get("key");
                if (!(keyValue instanceof JaloString key)) {
                    throw error("from-entries: key must be string");
                }
                out = out.put(key.value(), ent.get("value"));
            }
            return out;
        });
        registry.register("with-entries", (args, env) -> {
            requireArity("with-entries", args, 2);
            JaloArray entries = toEntries(args.get(1));
            // Inline to-entries / map / from-entries composition.
            JaloArray mapped = JaloArray.empty();
            for (int i = 0; i < entries.size(); i++) {
                mapped = mapped.append(callFn(args.get(0), List.of(entries.get(i)), env, evaluator));
            }
            return fromEntries(mapped);
        });
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
        registry.register("not", (args, env) -> {
            requireArity("not", args, 1);
            return truthy(args.get(0)) ? JaloBool.FALSE : JaloBool.TRUE;
        });
        registry.register("any", (args, env) -> {
            requireArity("any", args, 2);
            JaloArray arr = requireArray("any", args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (truthy(callFn(args.get(0), List.of(arr.get(i)), env, evaluator))) {
                    return JaloBool.TRUE;
                }
            }
            return JaloBool.FALSE;
        });
        registry.register("all", (args, env) -> {
            requireArity("all", args, 2);
            JaloArray arr = requireArray("all", args.get(1));
            for (int i = 0; i < arr.size(); i++) {
                if (!truthy(callFn(args.get(0), List.of(arr.get(i)), env, evaluator))) {
                    return JaloBool.FALSE;
                }
            }
            return JaloBool.TRUE;
        });
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

    private static JaloValue fromEntries(JaloArray arr) {
        JaloMap out = JaloMap.empty();
        for (int i = 0; i < arr.size(); i++) {
            JaloMap ent = requireMap("with-entries", arr.get(i));
            JaloValue keyValue = ent.get("key");
            if (!(keyValue instanceof JaloString key)) {
                throw error("with-entries: key must be string");
            }
            out = out.put(key.value(), ent.get("value"));
        }
        return out;
    }

    private static JaloArray toEntries(JaloValue value) {
        JaloMap map = requireMap("to-entries", value);
        JaloArray out = JaloArray.empty();
        for (Map.Entry<String, JaloValue> e : map.entries().entrySet()) {
            out = out.append(JaloMap.empty().put("key", new JaloString(e.getKey())).put("value", e.getValue()));
        }
        return out;
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

    private static boolean truthy(JaloValue v) {
        return !(v == JaloNull.INSTANCE || JaloBool.FALSE.equals(v));
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (value instanceof JaloArray arr) {
            return arr;
        }
        throw error(name + ": expected array");
    }

    private static JaloMap requireMap(String name, JaloValue value) {
        if (value instanceof JaloMap map) {
            return map;
        }
        throw error(name + ": expected map");
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
