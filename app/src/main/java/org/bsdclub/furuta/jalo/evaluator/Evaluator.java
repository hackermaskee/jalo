package org.bsdclub.furuta.jalo.evaluator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bsdclub.furuta.jalo.evaluator.builtins.BuiltinFunction;
import org.bsdclub.furuta.jalo.evaluator.builtins.BuiltinRegistry;
import org.bsdclub.furuta.jalo.evaluator.builtins.ArrayBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.MapBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.HofBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.IoBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.NumericBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.SeqBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.StringBuiltins;
import org.bsdclub.furuta.jalo.evaluator.builtins.TypeBuiltins;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloBuiltinFunction;
import org.bsdclub.furuta.jalo.value.JaloFunction;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.organicdesign.fp.collections.PersistentHashMap;

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
    private final BuiltinRegistry registry;

    /**
     * Creates a new evaluator with a fresh global namespace.
     */
    public Evaluator() {
        this.globalEnv = Environment.root();
        this.registry = new BuiltinRegistry();
        StringBuiltins.registerAll(registry);
        ArrayBuiltins.registerAll(registry);
        MapBuiltins.registerAll(registry);
        SeqBuiltins.registerAll(registry);
        TypeBuiltins.registerAll(registry);
        NumericBuiltins.registerAll(registry);
        HofBuiltins.registerAll(registry, this);
        IoBuiltins.registerAll(registry);
    }

    /**
     * Evaluates an AST in this evaluator's global environment.
     *
     * @param ast AST node to evaluate
     * @return evaluated value
     */
    public JaloValue eval(JaloValue ast) {
        return eval(ast, globalEnv);
    }

    /**
     * Evaluates an AST in the given environment.
     *
     * @param ast AST node to evaluate
     * @param env environment used for name resolution
     * @return evaluated value
     * @implSpec Dispatches via sealed switch on {@link JaloValue} subtypes.
     *           Adding a new {@link JaloValue} subtype requires updating this
     *           switch (compiler-enforced via sealed permits).
     */
    public JaloValue eval(JaloValue ast, Environment env) {
        if (ast instanceof JaloNull || ast instanceof JaloBool || ast instanceof JaloNumber) {
            return ast;
        }
        if (ast instanceof JaloString s) {
            return env.lookup(s.value());
        }
        if (!(ast instanceof JaloArray form)) {
            return ast;
        }
        if (form.size() == 0) {
            return form;
        }
        if (!(form.get(0) instanceof JaloString op)) {
            return applyForm(form, env);
        }

        return switch (op.value()) {
            case "quote" -> evalQuote(form);
            case "backquote" -> evalBackquote(form, env);
            case "if" -> evalIf(form, env);
            case "and" -> evalAnd(form, env);
            case "or" -> evalOr(form, env);
            case "declare" -> evalDeclare(form);
            case "def" -> evalDef(form, env);
            case "let" -> evalLet(form, env, false);
            case "let*" -> evalLet(form, env, true);
            case "letrec" -> evalLetRec(form, env);
            case "fn" -> JaloFunction.fromForm(form, env);
            case "raise" -> evalRaise(form, env);
            case "handle" -> evalHandle(form, env);
            case "error" -> evalError(form, env);
            case "match" -> evalMatch(form, env);
            default -> applyForm(form, env);
        };
    }

    /**
     * Evaluates a {@code match} form with sequential pattern trials.
     *
     * @param form the AST node {@code ["match", valueExpr, pattern1, expr1, ...]}
     * @param env current evaluation environment
     * @return the selected branch value or {@code #null} when all patterns fail
     * @throws JaloEffectSignal if the form arity is invalid
     */
    private JaloValue evalMatch(JaloArray form, Environment env) {
        if (form.size() < 4 || ((form.size() - 2) % 2 != 0)) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for match"));
        }
        JaloValue value = eval(form.get(1), env);
        PatternMatcher matcher = new PatternMatcher(this);
        for (int i = 2; i + 1 < form.size(); i += 2) {
            Optional<PersistentHashMap<String, JaloValue>> bindings = matcher.match(form.get(i), value, env);
            if (bindings.isPresent()) {
                Environment branchEnv = env.bindAll(bindings.get());
                return eval(form.get(i + 1), branchEnv);
            }
        }
        return JaloNull.INSTANCE;
    }

    private JaloValue evalError(JaloArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for error"));
        }
        throw new JaloEffectSignal(new JaloString("error"), eval(form.get(1), env));
    }

    private JaloValue evalQuote(JaloArray form) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for quote"));
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
    private JaloValue evalBackquote(JaloArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for backquote"));
        }
        return constructFromPattern(form.get(1), env);
    }

    private JaloValue constructFromPattern(JaloValue node, Environment env) {
        if (!(node instanceof JaloArray form)) {
            return node;
        }
        if (form.size() == 0) {
            return form;
        }
        if (!(form.get(0) instanceof JaloString opNode)) {
            return form;
        }

        return switch (opNode.value()) {
            case "dollar" -> {
                if (form.size() != 2) {
                    throw new JaloEffectSignal(new JaloString("error"), new JaloString("Malformed dollar form"));
                }
                yield toJsonValue(eval(form.get(1), env));
            }
            case "int", "long" -> toJsonValue(eval(form, env));
            case "array" -> spliceArray(form, env);
            case "map" -> spliceMap(form, env);
            default -> form;
        };
    }

    private JaloArray spliceArray(JaloArray form, Environment env) {
        JaloArray result = JaloArray.empty();
        for (int i = 1; i < form.size(); i++) {
            JaloValue elt = form.get(i);
            if (elt instanceof JaloArray inner
                    && inner.size() == 2
                    && inner.get(0) instanceof JaloString op
                    && "at".equals(op.value())) {
                JaloValue arr = eval(inner.get(1), env);
                if (!(arr instanceof JaloArray a)) {
                    throw new JaloEffectSignal(new JaloString("error"), new JaloString("splice target must be array"));
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

    private JaloMap spliceMap(JaloArray form, Environment env) {
        JaloMap result = JaloMap.empty();
        for (int i = 1; i < form.size(); i++) {
            JaloValue ent = form.get(i);
            if (!(ent instanceof JaloArray entry) || entry.size() < 2) {
                continue;
            }
            JaloValue head = entry.get(0);
            if (head instanceof JaloString op && "percent".equals(op.value())) {
                JaloValue extra = eval(entry.get(1), env);
                if (!(extra instanceof JaloMap obj)) {
                    throw new JaloEffectSignal(new JaloString("error"), new JaloString("percent splice target must be map"));
                }
                for (Map.Entry<String, JaloValue> e : obj.entries().entrySet()) {
                    result = result.put(e.getKey(), e.getValue());
                }
            } else if (head instanceof JaloString key) {
                result = result.put(key.value(), constructFromPattern(entry.get(1), env));
            }
        }
        return result;
    }

    private JaloValue evalIf(JaloArray form, Environment env) {
        if (form.size() != 4) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for if"));
        }
        JaloValue cond = eval(form.get(1), env);
        boolean truthy = !(cond == JaloNull.INSTANCE || JaloBool.FALSE.equals(cond));
        return eval((JaloValue) (truthy ? form.get(2) : form.get(3)), env);
    }

    private JaloValue evalAnd(JaloArray form, Environment env) {
        JaloValue last = JaloBool.TRUE;
        for (int i = 1; i < form.size(); i++) {
            last = eval(form.get(i), env);
            if (last == JaloNull.INSTANCE || JaloBool.FALSE.equals(last)) return last;
        }
        return last;
    }

    private JaloValue evalOr(JaloArray form, Environment env) {
        if (form.size() == 1) return JaloNull.INSTANCE;
        JaloValue last = JaloNull.INSTANCE;
        for (int i = 1; i < form.size(); i++) {
            last = eval(form.get(i), env);
            if (!(last == JaloNull.INSTANCE || JaloBool.FALSE.equals(last))) return last;
        }
        return last;
    }

    private JaloValue evalDef(JaloArray form, Environment env) {
        if (form.size() != 3 || !(form.get(1) instanceof JaloString name)) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for def"));
        }
        JaloValue value = eval(form.get(2), env);
        globalEnv.defineGlobal(name.value(), value);
        return JaloNull.INSTANCE;
    }

    private JaloValue evalDeclare(JaloArray form) {
        if (form.size() < 2) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for declare"));
        }
        for (int i = 1; i < form.size(); i++) {
            if (!(form.get(i) instanceof JaloString)) {
                throw new JaloEffectSignal(new JaloString("error"), new JaloString("declare arguments must be identifiers"));
            }
        }
        return JaloNull.INSTANCE;
    }

    private JaloValue evalLet(JaloArray form, Environment env, boolean sequential) {
        if (form.size() < 3 || !(form.get(1) instanceof JaloArray bindings) || bindings.size() % 2 != 0) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Malformed let"));
        }

        Environment bodyEnv = env;
        Environment evalEnv = env;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JaloString name)) {
                throw new JaloEffectSignal(new JaloString("error"), new JaloString("binding name must be identifier"));
            }
            JaloValue value = eval(bindings.get(i + 1), sequential ? evalEnv : env);
            bodyEnv = bodyEnv.bind(name.value(), value);
            if (sequential) {
                evalEnv = bodyEnv;
            }
        }

        JaloValue result = JaloNull.INSTANCE;
        for (int i = 2; i < form.size(); i++) {
            result = eval(form.get(i), bodyEnv);
        }
        return result;
    }

    private JaloValue evalLetRec(JaloArray form, Environment env) {
        if (form.size() < 3 || !(form.get(1) instanceof JaloArray bindings) || bindings.size() % 2 != 0) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Malformed letrec"));
        }

        List<String> names = new ArrayList<>();
        Map<String, Boolean> hadGlobal = new LinkedHashMap<>();
        Map<String, JaloValue> oldGlobal = new LinkedHashMap<>();
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JaloString name)) {
                throw new JaloEffectSignal(new JaloString("error"), new JaloString("binding name must be identifier"));
            }
            names.add(name.value());
            hadGlobal.put(name.value(), globalEnv.hasGlobal(name.value()));
            if (globalEnv.hasGlobal(name.value())) {
                oldGlobal.put(name.value(), globalEnv.getGlobal(name.value()));
            }
            globalEnv.defineGlobal(name.value(), JaloNull.INSTANCE);
        }

        try {
            for (int i = 0; i < names.size(); i++) {
                JaloValue actual = eval(bindings.get((i * 2) + 1), env);
                globalEnv.defineGlobal(names.get(i), actual);
            }

            JaloValue result = JaloNull.INSTANCE;
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

    private JaloValue evalRaise(JaloArray form, Environment env) {
        if (form.size() != 3) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for raise"));
        }
        JaloValue tag = eval(form.get(1), env);
        JaloValue value = eval(form.get(2), env);
        throw new JaloEffectSignal(tag, value);
    }

    private JaloValue evalHandle(JaloArray form, Environment env) {
        if (form.size() < 3) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for handle"));
        }
        try {
            return eval(form.get(1), env);
        } catch (JaloEffectSignal sig) {
            for (int i = 2; i < form.size(); i++) {
                if (!(form.get(i) instanceof JaloArray handler) || handler.size() != 3 || !(handler.get(1) instanceof JaloString varName)) {
                    throw new JaloEffectSignal(new JaloString("error"), new JaloString("Malformed handle clause"));
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

    private JaloValue applyForm(JaloArray form, Environment env) {
        if (form.size() == 0) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Cannot apply empty list"));
        }

        if (form.get(0) instanceof JaloString op) {
            switch (op.value()) {
                case "int":
                    if (form.size() != 2) {
                        throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for numeric wrapper"));
                    }
                    return new JaloInt((int) NumericPromotion.of(eval(form.get(1), env)).asLong());
                case "long":
                    if (form.size() != 2) {
                        throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for numeric wrapper"));
                    }
                    return new JaloLong(NumericPromotion.of(eval(form.get(1), env)).asLong());
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
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) < 0 ? JaloBool.TRUE : JaloBool.FALSE;
                case ">":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) > 0 ? JaloBool.TRUE : JaloBool.FALSE;
                case "<=":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) <= 0 ? JaloBool.TRUE : JaloBool.FALSE;
                case ">=":
                    return cmp(eval(form.get(1), env), eval(form.get(2), env)) >= 0 ? JaloBool.TRUE : JaloBool.FALSE;
                case "number?":
                    return bool(form.size() == 2 && isNumber(eval(form.get(1), env)));
                case "string?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JaloString);
                case "array?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JaloArray);
                case "object?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof org.bsdclub.furuta.jalo.value.JaloMap);
                case "boolean?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JaloBool);
                case "null?":
                    return bool(form.size() == 2 && eval(form.get(1), env) instanceof JaloNull);
                case "fn?":
                    if (form.size() != 2) return JaloBool.FALSE;
                    JaloValue fnCandidate = eval(form.get(1), env);
                    return bool(fnCandidate instanceof JaloFunction || fnCandidate instanceof JaloBuiltinFunction);
                case "type":
                    return typeOf(form, env);
                default:
                    Optional<BuiltinFunction> builtin = registry.lookup(op.value());
                    if (builtin.isPresent()) {
                        List<JaloValue> args = new ArrayList<>();
                        for (int i = 1; i < form.size(); i++) {
                            args.add(eval(form.get(i), env));
                        }
                        return builtin.get().apply(args, env);
                    }
                    break;
            }
        }

        JaloValue fnVal = eval(form.get(0), env);
        List<JaloValue> args = new ArrayList<>();
        for (int i = 1; i < form.size(); i++) {
            args.add(eval(form.get(i), env));
        }
        if (fnVal instanceof JaloFunction fn) return fn.apply(args, this);
        if (fnVal instanceof JaloBuiltinFunction bfn) return bfn.fn().apply(args, env);
        throw new JaloEffectSignal(new JaloString("error"), new JaloString("First position is not function"));
    }

    private JaloValue typeOf(JaloArray form, Environment env) {
        if (form.size() != 2) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Wrong arity for type"));
        }
        JaloValue v = eval(form.get(1), env);
        if (v instanceof JaloNull) return new JaloString("null");
        if (v instanceof JaloBool) return new JaloString("boolean");
        if (v instanceof JaloNumber) return new JaloString("double");
        if (v instanceof JaloString) return new JaloString("string");
        if (v instanceof JaloArray) return new JaloString("array");
        if (v instanceof org.bsdclub.furuta.jalo.value.JaloMap) return new JaloString("object");
        if (v instanceof JaloInt) return new JaloString("int");
        if (v instanceof JaloLong) return new JaloString("long");
        if (v instanceof JaloFunction || v instanceof JaloBuiltinFunction) return new JaloString("function");
        throw new JaloEffectSignal(new JaloString("error"), new JaloString("Unknown type"));
    }

    private JaloValue add(JaloValue l, JaloValue r) { return numericBinary(l, r, '+'); }
    private JaloValue sub(JaloValue l, JaloValue r) { return numericBinary(l, r, '-'); }
    private JaloValue mul(JaloValue l, JaloValue r) { return numericBinary(l, r, '*'); }

    private JaloValue div(JaloValue l, JaloValue r) {
        return NumericPromotion.divide(l, r);
    }

    private JaloValue eq(JaloValue l, JaloValue r) {
        NumericPromotion.Numeric left = NumericPromotion.of(l);
        NumericPromotion.Numeric right = NumericPromotion.of(r);
        return left.asDouble() == right.asDouble() ? JaloBool.TRUE : JaloBool.FALSE;
    }

    private int cmp(JaloValue l, JaloValue r) {
        NumericPromotion.Numeric left = NumericPromotion.of(l);
        NumericPromotion.Numeric right = NumericPromotion.of(r);
        return Double.compare(left.asDouble(), right.asDouble());
    }

    private JaloValue numericBinary(JaloValue l, JaloValue r, char op) {
        NumericPromotion.Numeric left = NumericPromotion.of(l);
        NumericPromotion.Numeric right = NumericPromotion.of(r);
        double result = switch (op) {
            case '+' -> left.asDouble() + right.asDouble();
            case '-' -> left.asDouble() - right.asDouble();
            case '*' -> left.asDouble() * right.asDouble();
            default -> throw new IllegalArgumentException("unknown op");
        };
        return NumericPromotion.result(left, right, result);
    }

    private JaloBool bool(boolean b) {
        return b ? JaloBool.TRUE : JaloBool.FALSE;
    }

    private boolean isNumber(JaloValue value) {
        return value instanceof JaloNumber || value instanceof JaloInt || value instanceof JaloLong;
    }

    private JaloValue toJsonValue(JaloValue value) {
        if (value instanceof JaloInt n) return new JaloNumber(n.value());
        if (value instanceof JaloLong n) return new JaloNumber(n.value());
        return value;
    }

}
