package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * String-related built-in functions for the jalo evaluator.
 */
public final class StringBuiltins {
    private StringBuiltins() { }

    /**
     * Registers all Phase 1 string built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("str-count", (args, env) -> new JaloInt(requireString("str-count", args, 1, 0).length()));
        registry.register("str-get", (args, env) -> {
            String s = requireString("str-get", args, 2, 0);
            int idx = requireIndex("str-get", args.get(1));
            if (idx < 0 || idx >= s.length()) {
                throw error("index out of range");
            }
            return new JaloString(String.valueOf(s.charAt(idx)));
        });
        registry.register("subs", (args, env) -> {
            String s = requireString("subs", args, 3, 0);
            int start = requireIndex("subs", args.get(1));
            int end = requireIndex("subs", args.get(2));
            if (start < 0 || end < start || end > s.length()) {
                throw error("index out of range");
            }
            return new JaloString(s.substring(start, end));
        });
        registry.register("str-upper", (args, env) -> new JaloString(requireString("str-upper", args, 1, 0).toUpperCase()));
        registry.register("str-lower", (args, env) -> new JaloString(requireString("str-lower", args, 1, 0).toLowerCase()));
        registry.register("str-trim", (args, env) -> new JaloString(requireString("str-trim", args, 1, 0).trim()));
        registry.register("str-starts-with?", (args, env) -> bool(requireString("str-starts-with?", args, 2, 0).startsWith(requireString("str-starts-with?", args, 2, 1))));
        registry.register("str-ends-with?", (args, env) -> bool(requireString("str-ends-with?", args, 2, 0).endsWith(requireString("str-ends-with?", args, 2, 1))));
        registry.register("str-contains?", (args, env) -> bool(requireString("str-contains?", args, 2, 0).contains(requireString("str-contains?", args, 2, 1))));
        registry.register("str-split", (args, env) -> {
            String s = requireString("str-split", args, 2, 0);
            String sep = requireString("str-split", args, 2, 1);
            String[] parts = s.split(java.util.regex.Pattern.quote(sep), -1);
            JaloArray arr = JaloArray.empty();
            for (String p : parts) {
                arr = arr.append(new JaloString(p));
            }
            return arr;
        });
        registry.register("str-join", (args, env) -> {
            requireArity("str-join", args, 2);
            JaloArray arr = requireArray("str-join", args.get(0));
            String sep = requireString("str-join", args, 2, 1);
            List<String> values = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                values.add(coerceToString(arr.get(i)));
            }
            return new JaloString(values.stream().collect(Collectors.joining(sep)));
        });
        registry.register("str-replace", (args, env) -> new JaloString(requireString("str-replace", args, 3, 0)
            .replace(requireString("str-replace", args, 3, 1), requireString("str-replace", args, 3, 2))));
        registry.register("str-replace-first", (args, env) -> {
            String src = requireString("str-replace-first", args, 3, 0);
            String old = requireString("str-replace-first", args, 3, 1);
            String rep = requireString("str-replace-first", args, 3, 2);
            int idx = src.indexOf(old);
            if (idx < 0) {
                return new JaloString(src);
            }
            return new JaloString(src.substring(0, idx) + rep + src.substring(idx + old.length()));
        });
        registry.register("str-index-of", (args, env) -> new JaloInt(requireString("str-index-of", args, 2, 0)
            .indexOf(requireString("str-index-of", args, 2, 1))));
        registry.register("str->number", (args, env) -> {
            String s = requireString("str->number", args, 1, 0);
            try {
                return new JaloNumber(Double.parseDouble(s));
            } catch (NumberFormatException ex) {
                throw error("invalid number");
            }
        });
        registry.register("number->str", (args, env) -> {
            requireArity("number->str", args, 1);
            JaloValue v = args.get(0);
            if (v instanceof JaloNumber n) return new JaloString(Double.toString(n.value()));
            if (v instanceof JaloInt n) return new JaloString(Integer.toString(n.value()));
            if (v instanceof JaloLong n) return new JaloString(Long.toString(n.value()));
            throw error("number->str expects number");
        });
        registry.register("str->keyword", (args, env) -> new JaloString(":" + requireString("str->keyword", args, 1, 0)));
        registry.register("keyword->str", (args, env) -> {
            String keyword = requireString("keyword->str", args, 1, 0);
            return new JaloString(keyword.startsWith(":") ? keyword.substring(1) : keyword);
        });
        registry.register("char-at", (args, env) -> {
            String s = requireString("char-at", args, 2, 0);
            int idx = requireIndex("char-at", args.get(1));
            if (idx < 0 || idx >= s.length()) throw error("index out of range");
            return new JaloString(String.valueOf(s.charAt(idx)));
        });
        registry.register("str-empty?", (args, env) -> bool(requireString("str-empty?", args, 1, 0).isEmpty()));
        registry.register("str-blank?", (args, env) -> bool(requireString("str-blank?", args, 1, 0).trim().isEmpty()));
        registry.register("str", (args, env) -> {
            requireArity("str", args, 1);
            return new JaloString(coerceToString(args.get(0)));
        });
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) {
            throw error("Wrong arity for " + name);
        }
    }

    private static String requireString(String name, List<JaloValue> args, int arity, int index) {
        requireArity(name, args, arity);
        if (!(args.get(index) instanceof JaloString s)) {
            throw error(name + " expects string");
        }
        return s.value();
    }

    private static JaloArray requireArray(String name, JaloValue value) {
        if (!(value instanceof JaloArray arr)) {
            throw error(name + " expects array");
        }
        return arr;
    }

    private static int requireIndex(String name, JaloValue value) {
        if (value instanceof JaloInt n) return n.value();
        if (value instanceof JaloLong n) return (int) n.value();
        if (value instanceof JaloNumber n) return (int) n.value();
        throw error(name + " expects numeric index");
    }

    private static String coerceToString(JaloValue value) {
        if (value instanceof JaloString s) return s.value();
        return value.toString();
    }

    private static JaloBool bool(boolean value) {
        return value ? JaloBool.TRUE : JaloBool.FALSE;
    }

    private static JaloEffectSignal error(String message) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(message));
    }
}
