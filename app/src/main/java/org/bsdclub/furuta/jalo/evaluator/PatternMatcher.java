package org.bsdclub.furuta.jalo.evaluator;

import java.util.Map;
import java.util.Optional;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.organicdesign.fp.collections.PersistentHashMap;

/**
 * Matches runtime values against jalo pattern forms.
 *
 * <p>Layer: Evaluation (support structure, per DESIGN.md §1 architecture table).
 * Performs recursive pattern matching for {@code match} special form (SPEC §5.3).
 */
public final class PatternMatcher {
    private final Evaluator evaluator;

    /**
     * Creates a matcher bound to an evaluator.
     *
     * @param evaluator evaluator used for pattern normalization
     */
    public PatternMatcher(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * Attempts to match a value against a pattern.
     *
     * @param pattern AST pattern node
     * @param value runtime value to match
     * @param env current environment
     * @return the variable bindings on success, empty if no match
     */
    public Optional<PersistentHashMap<String, JaloValue>> match(
            JaloValue pattern, JaloValue value, Environment env) {
        return matchNode(pattern, value, env);
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchNode(
            JaloValue pattern, JaloValue value, Environment env) {
        if (isBackquote(pattern)) {
            JaloValue inner = ((JaloArray) pattern).get(1);
            return matchBackquote(inner, value, env);
        }

        JaloValue normalized = toJsonValue(value);
        if (pattern.equals(normalized)) {
            return Optional.of(PersistentHashMap.empty());
        }
        return Optional.empty();
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchBackquote(
            JaloValue inner, JaloValue value, Environment env) {
        if (isForm(inner, "dollar")) {
            JaloValue varNode = ((JaloArray) inner).get(1);
            if (!(varNode instanceof JaloString var)) {
                return Optional.empty();
            }
            if ("_".equals(var.value())) {
                return Optional.of(PersistentHashMap.empty());
            }
            return Optional.of(PersistentHashMap.<String, JaloValue>empty().assoc(var.value(), value));
        }

        if (isForm(inner, "array")) {
            return matchArrayPattern((JaloArray) inner, value, env);
        }

        if (isForm(inner, "map")) {
            return matchMapPattern((JaloArray) inner, value, env);
        }

        JaloValue normalizedPattern = evaluator.eval(JaloArray.of(new JaloString("backquote"), inner), env);
        if (normalizedPattern.equals(toJsonValue(value))) {
            return Optional.of(PersistentHashMap.empty());
        }
        return Optional.empty();
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchArrayPattern(
            JaloArray pattern, JaloValue value, Environment env) {
        if (!(toJsonValue(value) instanceof JaloArray arr)) {
            return Optional.empty();
        }

        int atIndex = -1;
        for (int i = 1; i < pattern.size(); i++) {
            if (isForm(pattern.get(i), "at")) {
                atIndex = i;
                break;
            }
        }

        PersistentHashMap<String, JaloValue> bindings = PersistentHashMap.empty();
        if (atIndex < 0) {
            if (arr.size() != pattern.size() - 1) {
                return Optional.empty();
            }
            for (int i = 1; i < pattern.size(); i++) {
                Optional<PersistentHashMap<String, JaloValue>> m =
                    matchBackquote(pattern.get(i), arr.get(i - 1), env);
                if (m.isEmpty()) {
                    return Optional.empty();
                }
                bindings = merge(bindings, m.get());
            }
            return Optional.of(bindings);
        }

        int prefix = atIndex - 1;
        int suffix = pattern.size() - 1 - atIndex;
        if (arr.size() < prefix + suffix) {
            return Optional.empty();
        }

        for (int i = 1; i < atIndex; i++) {
            Optional<PersistentHashMap<String, JaloValue>> m =
                matchBackquote(pattern.get(i), arr.get(i - 1), env);
            if (m.isEmpty()) {
                return Optional.empty();
            }
            bindings = merge(bindings, m.get());
        }

        JaloArray atForm = (JaloArray) pattern.get(atIndex);
        if (!(atForm.get(1) instanceof JaloString restVar)) {
            return Optional.empty();
        }
        JaloArray rest = JaloArray.empty();
        int restEnd = arr.size() - suffix;
        for (int i = prefix; i < restEnd; i++) {
            rest = rest.append(arr.get(i));
        }
        if (!"_".equals(restVar.value())) {
            bindings = bindings.assoc(restVar.value(), rest);
        }

        for (int i = atIndex + 1; i < pattern.size(); i++) {
            int arrIdx = arr.size() - (pattern.size() - i);
            Optional<PersistentHashMap<String, JaloValue>> m =
                matchBackquote(pattern.get(i), arr.get(arrIdx), env);
            if (m.isEmpty()) {
                return Optional.empty();
            }
            bindings = merge(bindings, m.get());
        }
        return Optional.of(bindings);
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchMapPattern(
            JaloArray pattern, JaloValue value, Environment env) {
        if (!(toJsonValue(value) instanceof JaloMap obj)) {
            return Optional.empty();
        }

        PersistentHashMap<String, JaloValue> bindings = PersistentHashMap.empty();
        JaloString restVar = null;
        for (int i = 1; i < pattern.size(); i++) {
            JaloValue node = pattern.get(i);
            if (isForm(node, "percent")) {
                JaloValue varNode = ((JaloArray) node).get(1);
                if (!(varNode instanceof JaloString var)) {
                    return Optional.empty();
                }
                restVar = var;
                continue;
            }

            if (!(node instanceof JaloArray entry) || entry.size() != 2 || !(entry.get(0) instanceof JaloString key)) {
                return Optional.empty();
            }
            if (!obj.containsKey(key.value())) {
                return Optional.empty();
            }
            Optional<PersistentHashMap<String, JaloValue>> m =
                matchBackquote(entry.get(1), obj.get(key.value()), env);
            if (m.isEmpty()) {
                return Optional.empty();
            }
            bindings = merge(bindings, m.get());
        }

        if (restVar != null && !"_".equals(restVar.value())) {
            JaloMap rest = JaloMap.empty();
            for (Map.Entry<String, JaloValue> entry : obj.entries().entrySet()) {
                if (!containsMapKeyPattern(pattern, entry.getKey())) {
                    rest = rest.put(entry.getKey(), entry.getValue());
                }
            }
            bindings = bindings.assoc(restVar.value(), rest);
        }
        return Optional.of(bindings);
    }

    private boolean containsMapKeyPattern(JaloArray pattern, String key) {
        for (int i = 1; i < pattern.size(); i++) {
            JaloValue node = pattern.get(i);
            if (node instanceof JaloArray entry && entry.size() == 2 && entry.get(0) instanceof JaloString k) {
                if (k.value().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private PersistentHashMap<String, JaloValue> merge(
            PersistentHashMap<String, JaloValue> a,
            PersistentHashMap<String, JaloValue> b) {
        PersistentHashMap<String, JaloValue> merged = a;
        for (Map.Entry<String, JaloValue> entry : b.entrySet()) {
            merged = merged.assoc(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    private boolean isBackquote(JaloValue node) {
        return isForm(node, "backquote") && ((JaloArray) node).size() == 2;
    }

    private boolean isForm(JaloValue node, String op) {
        return node instanceof JaloArray arr
            && arr.size() > 0
            && arr.get(0) instanceof JaloString s
            && op.equals(s.value());
    }

    private JaloValue toJsonValue(JaloValue value) {
        if (value instanceof JaloInt n) return new org.bsdclub.furuta.jalo.value.JaloNumber(n.value());
        if (value instanceof JaloLong n) return new org.bsdclub.furuta.jalo.value.JaloNumber(n.value());
        return value;
    }
}
