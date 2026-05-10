package org.bsdclub.furuta.jalo.evaluator;

import java.util.Map;
import java.util.Optional;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;
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
            JsonValue pattern, JaloValue value, Environment env) {
        return matchNode(pattern, value, env);
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchNode(
            JsonValue pattern, JaloValue value, Environment env) {
        if (isBackquote(pattern)) {
            JsonValue inner = ((JsonArray) pattern).get(1);
            return matchBackquote(inner, value, env);
        }

        JsonValue normalized = toJsonValue(value);
        if (pattern.equals(normalized)) {
            return Optional.of(PersistentHashMap.empty());
        }
        return Optional.empty();
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchBackquote(
            JsonValue inner, JaloValue value, Environment env) {
        if (isForm(inner, "dollar")) {
            JsonValue varNode = ((JsonArray) inner).get(1);
            if (!(varNode instanceof JsonString var)) {
                return Optional.empty();
            }
            if ("_".equals(var.value())) {
                return Optional.of(PersistentHashMap.empty());
            }
            return Optional.of(PersistentHashMap.<String, JaloValue>empty().assoc(var.value(), value));
        }

        if (isForm(inner, "array")) {
            return matchArrayPattern((JsonArray) inner, value, env);
        }

        if (isForm(inner, "map")) {
            return matchMapPattern((JsonArray) inner, value, env);
        }

        JaloValue normalizedPattern = evaluator.eval(JsonArray.of(new JsonString("backquote"), inner), env);
        if (normalizedPattern.equals(toJsonValue(value))) {
            return Optional.of(PersistentHashMap.empty());
        }
        return Optional.empty();
    }

    private Optional<PersistentHashMap<String, JaloValue>> matchArrayPattern(
            JsonArray pattern, JaloValue value, Environment env) {
        if (!(toJsonValue(value) instanceof JsonArray arr)) {
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

        JsonArray atForm = (JsonArray) pattern.get(atIndex);
        if (!(atForm.get(1) instanceof JsonString restVar)) {
            return Optional.empty();
        }
        JsonArray rest = JsonArray.empty();
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
            JsonArray pattern, JaloValue value, Environment env) {
        if (!(toJsonValue(value) instanceof JsonObject obj)) {
            return Optional.empty();
        }

        PersistentHashMap<String, JaloValue> bindings = PersistentHashMap.empty();
        JsonString restVar = null;
        for (int i = 1; i < pattern.size(); i++) {
            JsonValue node = pattern.get(i);
            if (isForm(node, "percent")) {
                JsonValue varNode = ((JsonArray) node).get(1);
                if (!(varNode instanceof JsonString var)) {
                    return Optional.empty();
                }
                restVar = var;
                continue;
            }

            if (!(node instanceof JsonArray entry) || entry.size() != 2 || !(entry.get(0) instanceof JsonString key)) {
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
            JsonObject rest = JsonObject.empty();
            for (Map.Entry<String, JsonValue> entry : obj.entries().entrySet()) {
                if (!containsMapKeyPattern(pattern, entry.getKey())) {
                    rest = rest.put(entry.getKey(), entry.getValue());
                }
            }
            bindings = bindings.assoc(restVar.value(), rest);
        }
        return Optional.of(bindings);
    }

    private boolean containsMapKeyPattern(JsonArray pattern, String key) {
        for (int i = 1; i < pattern.size(); i++) {
            JsonValue node = pattern.get(i);
            if (node instanceof JsonArray entry && entry.size() == 2 && entry.get(0) instanceof JsonString k) {
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

    private boolean isBackquote(JsonValue node) {
        return isForm(node, "backquote") && ((JsonArray) node).size() == 2;
    }

    private boolean isForm(JsonValue node, String op) {
        return node instanceof JsonArray arr
            && arr.size() > 0
            && arr.get(0) instanceof JsonString s
            && op.equals(s.value());
    }

    private JsonValue toJsonValue(JaloValue value) {
        if (value instanceof JsonValue jsonValue) {
            return jsonValue;
        }
        if (value instanceof JaloInt n) {
            return new org.bsdclub.furuta.jalo.json.JsonNumber(n.value());
        }
        if (value instanceof JaloLong n) {
            return new org.bsdclub.furuta.jalo.json.JsonNumber(n.value());
        }
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("match target must be JSON-compatible"));
    }
}
