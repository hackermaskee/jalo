package org.bsdclub.furuta.jalo.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Tree-walking interpreter for jalo JSON model AST.
 *
 * <p>Layer: Evaluation (per DESIGN.md §1 architecture table).
 * Consumes verified AST and produces runtime values or effect signals.
 *
 * <p>State model: this evaluator keeps a mutable global namespace in
 * {@link #globalEnv} for {@code def} accumulation.
 *
 * @see Environment
 * @see JaloEffectSignal
 * @see <a href="../../../docs/SPEC.md#4">SPEC §4 semantics</a>
 */
public final class Evaluator {
    private final Environment globalEnv;

    /**
     * Creates a new evaluator with a fresh global namespace.
     */
    public Evaluator() {
        this.globalEnv = Environment.root();
    }

    /**
     * Evaluates an AST in this evaluator's global environment.
     *
     * @param ast AST node to evaluate
     * @return evaluated value
     */
    public JaloValue eval(JsonValue ast) {
        return eval(ast, globalEnv);
    }

    /**
     * Evaluates an AST in the given environment.
     *
     * @param ast AST node to evaluate
     * @param env environment used for name resolution
     * @return evaluated value
     * @implSpec Dispatches via sealed switch on {@link JsonValue} subtypes.
     *           Adding a new {@link JsonValue} subtype requires updating this
     *           switch (compiler-enforced via sealed permits).
     */
    public JaloValue eval(JsonValue ast, Environment env) {
        if (ast instanceof JsonNull || ast instanceof JsonBool || ast instanceof JsonNumber) {
            return ast;
        }
        if (ast instanceof JsonString s) {
            return env.lookup(s.value());
        }
        if (!(ast instanceof JsonArray form)) {
            return ast;
        }
        if (form.size() == 0) {
            return form;
        }
        if (!(form.get(0) instanceof JsonString op)) {
            return applyForm(form, env);
        }

        return switch (op.value()) {
            case "quote" -> evalQuote(form);
            case "backquote" -> evalBackquote(form, env);
            case "if" -> evalIf(form, env);
            case "declare" -> evalDeclare(form);
            case "def" -> evalDef(form, env);
            case "let" -> evalLet(form, env, false);
            case "let*" -> evalLet(form, env, true);
            case "letrec" -> evalLetRec(form, env);
            case "fn" -> JaloFunction.fromForm(form, env);
            case "raise" -> evalRaise(form, env);
            case "handle" -> evalHandle(form, env);
            case "error" -> evalError(form, env);
            default -> applyForm(form, env);
        };
    }

    private JaloValue evalError(JsonArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for error"));
        }
        throw new JaloEffectSignal(new JsonString("error"), eval(form.get(1), env));
    }

    private JaloValue evalQuote(JsonArray form) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for quote"));
        }
        return (JaloValue) form.get(1);
    }

    /**
     * Evaluates a {@code backquote} form, constructing a JSON value from a template pattern.
     *
     * <p>Supports {@code (dollar e)} for expression embedding,
     * {@code (at e)} for array splicing, and {@code (percent e)} for map merging (SPEC §5.2).
     *
     * @param form  the AST node {@code ["backquote", pattern]}; must have exactly 2 elements
     * @param env   the current evaluation environment
     * @return the constructed JSON value
     * @throws JaloEffectSignal with effect {@code "error"} if the form is malformed
     *                          or a splice target has an unexpected type
     */
    private JaloValue evalBackquote(JsonArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for backquote"));
        }
        return constructFromPattern(form.get(1), env);
    }

    private JsonValue constructFromPattern(JsonValue node, Environment env) {
        if (!(node instanceof JsonArray form)) {
            return node;
        }
        if (form.size() == 0) {
            return form;
        }
        if (!(form.get(0) instanceof JsonString opNode)) {
            return form;
        }

        return switch (opNode.value()) {
            case "dollar" -> {
                if (form.size() != 2) {
                    throw new JaloEffectSignal(new JsonString("error"), new JsonString("Malformed dollar form"));
                }
                yield toJsonValue(eval(form.get(1), env));
            }
            case "int", "long" -> toJsonValue(eval(form, env));
            case "array" -> spliceArray(form, env);
            case "map" -> spliceMap(form, env);
            default -> form;
        };
    }

    private JsonArray spliceArray(JsonArray form, Environment env) {
        JsonArray result = JsonArray.empty();
        for (int i = 1; i < form.size(); i++) {
            JsonValue elt = form.get(i);
            if (elt instanceof JsonArray inner
                    && inner.size() == 2
                    && inner.get(0) instanceof JsonString op
                    && "at".equals(op.value())) {
                JaloValue arr = eval(inner.get(1), env);
                if (!(arr instanceof JsonArray a)) {
                    throw new JaloEffectSignal(new JsonString("error"), new JsonString("splice target must be array"));
                }
                for (int j = 0; j < a.size(); j++) {
                    result = result.append(a.get(j));
                }
            } else {
                result = result.append(constructFromPattern(elt, env));
            }
        }
        return result;
    }

    private JsonObject spliceMap(JsonArray form, Environment env) {
        JsonObject result = JsonObject.empty();
        for (int i = 1; i < form.size(); i++) {
            JsonValue ent = form.get(i);
            if (!(ent instanceof JsonArray entry) || entry.size() < 2) {
                continue;
            }
            JsonValue head = entry.get(0);
            if (head instanceof JsonString op && "percent".equals(op.value())) {
                JaloValue extra = eval(entry.get(1), env);
                if (!(extra instanceof JsonObject obj)) {
                    throw new JaloEffectSignal(new JsonString("error"), new JsonString("percent splice target must be map"));
                }
                for (Map.Entry<String, JsonValue> e : obj.entries().entrySet()) {
                    result = result.put(e.getKey(), e.getValue());
                }
            } else if (head instanceof JsonString key) {
                result = result.put(key.value(), constructFromPattern(entry.get(1), env));
            }
        }
        return result;
    }

    private JaloValue evalIf(JsonArray form, Environment env) {
        if (form.size() != 4) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for if"));
        }
        JaloValue cond = eval(form.get(1), env);
        boolean truthy = !(cond == JsonNull.INSTANCE || JsonBool.FALSE.equals(cond));
        return eval((JsonValue) (truthy ? form.get(2) : form.get(3)), env);
    }

    private JaloValue evalDef(JsonArray form, Environment env) {
        if (form.size() != 3 || !(form.get(1) instanceof JsonString name)) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for def"));
        }
        JaloValue value = eval(form.get(2), env);
        globalEnv.defineGlobal(name.value(), value);
        return JsonNull.INSTANCE;
    }

    private JaloValue evalDeclare(JsonArray form) {
        if (form.size() < 2) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for declare"));
        }
        for (int i = 1; i < form.size(); i++) {
            if (!(form.get(i) instanceof JsonString)) {
                throw new JaloEffectSignal(new JsonString("error"), new JsonString("declare arguments must be identifiers"));
            }
        }
        return JsonNull.INSTANCE;
    }

    private JaloValue evalLet(JsonArray form, Environment env, boolean sequential) {
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray bindings) || bindings.size() % 2 != 0) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Malformed let"));
        }

        Environment bodyEnv = env;
        Environment evalEnv = env;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new JaloEffectSignal(new JsonString("error"), new JsonString("binding name must be identifier"));
            }
            JaloValue value = eval(bindings.get(i + 1), sequential ? evalEnv : env);
            bodyEnv = bodyEnv.bind(name.value(), value);
            if (sequential) {
                evalEnv = bodyEnv;
            }
        }

        JaloValue result = JsonNull.INSTANCE;
        for (int i = 2; i < form.size(); i++) {
            result = eval(form.get(i), bodyEnv);
        }
        return result;
    }

    private JaloValue evalLetRec(JsonArray form, Environment env) {
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray bindings) || bindings.size() % 2 != 0) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Malformed letrec"));
        }

        List<String> names = new ArrayList<>();
        Map<String, Boolean> hadGlobal = new LinkedHashMap<>();
        Map<String, JaloValue> oldGlobal = new LinkedHashMap<>();
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new JaloEffectSignal(new JsonString("error"), new JsonString("binding name must be identifier"));
            }
            names.add(name.value());
            hadGlobal.put(name.value(), globalEnv.hasGlobal(name.value()));
            if (globalEnv.hasGlobal(name.value())) {
                oldGlobal.put(name.value(), globalEnv.getGlobal(name.value()));
            }
            globalEnv.defineGlobal(name.value(), JsonNull.INSTANCE);
        }

        try {
            for (int i = 0; i < names.size(); i++) {
                JaloValue actual = eval(bindings.get((i * 2) + 1), env);
                globalEnv.defineGlobal(names.get(i), actual);
            }

            JaloValue result = JsonNull.INSTANCE;
            for (int i = 2; i < form.size(); i++) {
                result = eval(form.get(i), env);
            }
            return result;
        } finally {
            for (String name : names) {
                if (hadGlobal.get(name)) {
                    globalEnv.defineGlobal(name, oldGlobal.get(name));
                } else {
                    globalEnv.removeGlobal(name);
                }
            }
        }
    }

    private JaloValue evalRaise(JsonArray form, Environment env) {
        if (form.size() != 3) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for raise"));
        }
        JaloValue tag = eval(form.get(1), env);
        JaloValue value = eval(form.get(2), env);
        throw new JaloEffectSignal(tag, value);
    }

    private JaloValue evalHandle(JsonArray form, Environment env) {
        if (form.size() < 3) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for handle"));
        }
        try {
            return eval(form.get(1), env);
        } catch (JaloEffectSignal sig) {
            for (int i = 2; i < form.size(); i++) {
                if (!(form.get(i) instanceof JsonArray handler) || handler.size() != 3 || !(handler.get(1) instanceof JsonString varName)) {
                    throw new JaloEffectSignal(new JsonString("error"), new JsonString("Malformed handle clause"));
                }
                JaloValue handlerTag = eval(handler.get(0), env);
                if (handlerTag.equals(sig.tag())) {
                    Environment handleEnv = env.bind(varName.value(), sig.value());
                    return eval(handler.get(2), handleEnv);
                }
            }
            throw sig;
        }
    }

    private JaloValue applyForm(JsonArray form, Environment env) {
        if (form.size() == 0) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Cannot apply empty list"));
        }

        if (form.get(0) instanceof JsonString op) {
            switch (op.value()) {
                case "int":
                    if (form.size() != 2) {
                        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for numeric wrapper"));
                    }
                    return new JaloInt((int) asNumeric(eval(form.get(1), env)).asLong());
                case "long":
                    if (form.size() != 2) {
                        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for numeric wrapper"));
                    }
                    return new JaloLong(asNumeric(eval(form.get(1), env)).asLong());
                case "+":
                    return add(eval(form.get(1), env), eval(form.get(2), env));
                case "-":
                    return sub(eval(form.get(1), env), eval(form.get(2), env));
                case "*":
                    return mul(eval(form.get(1), env), eval(form.get(2), env));
                case "/":
                    return div(eval(form.get(1), env), eval(form.get(2), env));
                case "=":
                    return eq(eval(form.get(1), env), eval(form.get(2), env));
                case "<":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) < 0 ? JsonBool.TRUE : JsonBool.FALSE;
                case ">":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) > 0 ? JsonBool.TRUE : JsonBool.FALSE;
                case "<=":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) <= 0 ? JsonBool.TRUE : JsonBool.FALSE;
                case ">=":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) >= 0 ? JsonBool.TRUE : JsonBool.FALSE;
                case "number?":
                    return bool(form.size() == 2 && isNumber(eval(form.get(1), env)));
                case "string?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JsonString);
                case "array?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JsonArray);
                case "object?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof org.bsdclub.furuta.jalo.json.JsonObject);
                case "boolean?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JsonBool);
                case "null?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JsonNull);
                case "fn?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JaloFunction);
                case "type":
                    return typeOf(form, env);
                default:
                    break;
            }
        }

        JaloValue fnVal = eval(form.get(0), env);
        if (!(fnVal instanceof JaloFunction fn)) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("First position is not function"));
        }
        List<JaloValue> args = new ArrayList<>();
        for (int i = 1; i < form.size(); i++) {
            args.add(eval(form.get(i), env));
        }
        return fn.apply(args, this);
    }

    private JaloValue typeOf(JsonArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Wrong arity for type"));
        }
        JaloValue v = eval(form.get(1), env);
        if (v instanceof JsonNull) return new JsonString("null");
        if (v instanceof JsonBool) return new JsonString("boolean");
        if (v instanceof JsonNumber) return new JsonString("double");
        if (v instanceof JsonString) return new JsonString("string");
        if (v instanceof JsonArray) return new JsonString("array");
        if (v instanceof org.bsdclub.furuta.jalo.json.JsonObject) return new JsonString("object");
        if (v instanceof JaloInt) return new JsonString("int");
        if (v instanceof JaloLong) return new JsonString("long");
        if (v instanceof JaloFunction) return new JsonString("function");
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Unknown type"));
    }

    private JaloValue add(JaloValue l, JaloValue r) { return numericBinary(l, r, '+'); }
    private JaloValue sub(JaloValue l, JaloValue r) { return numericBinary(l, r, '-'); }
    private JaloValue mul(JaloValue l, JaloValue r) { return numericBinary(l, r, '*'); }

    private JaloValue div(JaloValue l, JaloValue r) {
        Numeric left = asNumeric(l);
        Numeric right = asNumeric(r);
        if (right.asDouble() == 0.0 && left.integral() && right.integral()) {
            throw new JaloEffectSignal(new JsonString("error"), new JsonString("Division by zero"));
        }
        return numericResult(left, right, left.asDouble() / right.asDouble());
    }

    private JaloValue eq(JaloValue l, JaloValue r) {
        Numeric left = asNumeric(l);
        Numeric right = asNumeric(r);
        return left.asDouble() == right.asDouble() ? JsonBool.TRUE : JsonBool.FALSE;
    }

    private int cmp(JaloValue l, JaloValue r) {
        Numeric left = asNumeric(l);
        Numeric right = asNumeric(r);
        return Double.compare(left.asDouble(), right.asDouble());
    }

    private JaloValue numericBinary(JaloValue l, JaloValue r, char op) {
        Numeric left = asNumeric(l);
        Numeric right = asNumeric(r);
        double result = switch (op) {
            case '+' -> left.asDouble() + right.asDouble();
            case '-' -> left.asDouble() - right.asDouble();
            case '*' -> left.asDouble() * right.asDouble();
            default -> throw new IllegalArgumentException("unknown op");
        };
        return numericResult(left, right, result);
    }

    private JaloValue numericResult(Numeric left, Numeric right, double result) {
        if (!left.isDouble() && !right.isDouble()) {
            if (left.isLong() || right.isLong()) {
                return new JaloLong((long) result);
            }
            return new JaloInt((int) result);
        }
        return new JsonNumber(result);
    }

    private JsonBool bool(boolean b) {
        return b ? JsonBool.TRUE : JsonBool.FALSE;
    }

    private boolean isNumber(JaloValue value) {
        return value instanceof JsonNumber || value instanceof JaloInt || value instanceof JaloLong;
    }

    private Numeric asNumeric(JaloValue value) {
        if (value instanceof JsonNumber n) {
            return new Numeric(n.value(), true, false);
        }
        if (value instanceof JaloInt n) {
            return new Numeric(n.value(), false, false);
        }
        if (value instanceof JaloLong n) {
            return new Numeric(n.value(), false, true);
        }
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Expected number"));
    }

    private JsonValue toJsonValue(JaloValue value) {
        if (value instanceof JsonValue jsonValue) {
            return jsonValue;
        }
        if (value instanceof JaloInt n) {
            return new JsonNumber(n.value());
        }
        if (value instanceof JaloLong n) {
            return new JsonNumber(n.value());
        }
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Backquote can only embed JSON-compatible values"));
    }

    private record Numeric(double asDouble, boolean isDouble, boolean isLong) {
        boolean integral() { return !isDouble; }
        long asLong() { return (long) asDouble; }
    }
}
