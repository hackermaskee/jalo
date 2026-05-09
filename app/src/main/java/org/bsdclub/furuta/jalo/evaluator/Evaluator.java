package org.bsdclub.furuta.jalo.evaluator;

import java.util.ArrayList;
import java.util.List;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;

public final class Evaluator {
    private final Environment globalEnv;

    public Evaluator() {
        this.globalEnv = Environment.root();
    }

    public JaloValue eval(JsonValue ast) {
        return eval(ast, globalEnv);
    }

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
            case "if" -> evalIf(form, env);
            case "def" -> evalDef(form, env);
            case "let" -> evalLet(form, env, false);
            case "let*" -> evalLet(form, env, true);
            case "letrec" -> evalLetRec(form, env);
            case "fn" -> JaloFunction.fromForm(form, env);
            default -> applyForm(form, env);
        };
    }

    private JaloValue evalQuote(JsonArray form) {
        if (form.size() != 2) {
            throw new JaloEffectSignal("error", "Wrong arity for quote");
        }
        return (JaloValue) form.get(1);
    }

    private JaloValue evalIf(JsonArray form, Environment env) {
        if (form.size() != 4) {
            throw new JaloEffectSignal("error", "Wrong arity for if");
        }
        JaloValue cond = eval(form.get(1), env);
        boolean truthy = !(cond == JsonNull.INSTANCE || JsonBool.FALSE.equals(cond));
        return eval((JsonValue) (truthy ? form.get(2) : form.get(3)), env);
    }

    private JaloValue evalDef(JsonArray form, Environment env) {
        if (form.size() != 3 || !(form.get(1) instanceof JsonString name)) {
            throw new JaloEffectSignal("error", "Wrong arity for def");
        }
        JaloValue value = eval(form.get(2), env);
        globalEnv.defineGlobal(name.value(), value);
        return value;
    }

    private JaloValue evalLet(JsonArray form, Environment env, boolean sequential) {
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray bindings) || bindings.size() % 2 != 0) {
            throw new JaloEffectSignal("error", "Malformed let");
        }

        Environment bodyEnv = env;
        Environment evalEnv = env;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new JaloEffectSignal("error", "binding name must be identifier");
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
            throw new JaloEffectSignal("error", "Malformed letrec");
        }

        Environment recEnv = env;
        List<String> names = new ArrayList<>();
        List<JsonValue> exprs = new ArrayList<>();
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new JaloEffectSignal("error", "binding name must be identifier");
            }
            names.add(name.value());
            exprs.add(bindings.get(i + 1));
            globalEnv.defineGlobal(name.value(), JsonNull.INSTANCE);
        }

        for (int i = 0; i < names.size(); i++) {
            JaloValue actual = eval(exprs.get(i), recEnv);
            globalEnv.defineGlobal(names.get(i), actual);
        }

        JaloValue result = JsonNull.INSTANCE;
        for (int i = 2; i < form.size(); i++) {
            result = eval(form.get(i), recEnv);
        }
        return result;
    }

    private JaloValue applyForm(JsonArray form, Environment env) {
        if (form.size() == 0) {
            throw new JaloEffectSignal("error", "Cannot apply empty list");
        }

        if (form.get(0) instanceof JsonString op) {
            switch (op.value()) {
                case "+":
                    return new JsonNumber(asNumber(eval(form.get(1), env)) + asNumber(eval(form.get(2), env)));
                case "-":
                    return new JsonNumber(asNumber(eval(form.get(1), env)) - asNumber(eval(form.get(2), env)));
                case "*":
                    return new JsonNumber(asNumber(eval(form.get(1), env)) * asNumber(eval(form.get(2), env)));
                case "=":
                    return asNumber(eval(form.get(1), env)) == asNumber(eval(form.get(2), env)) ? JsonBool.TRUE : JsonBool.FALSE;
                default:
                    break;
            }
        }

        JaloValue fnVal = eval(form.get(0), env);
        if (!(fnVal instanceof JaloFunction fn)) {
            throw new JaloEffectSignal("error", "First position is not function");
        }
        List<JaloValue> args = new ArrayList<>();
        for (int i = 1; i < form.size(); i++) {
            args.add(eval(form.get(i), env));
        }
        return fn.apply(args, this);
    }

    private double asNumber(JaloValue value) {
        if (value instanceof JsonNumber n) {
            return n.value();
        }
        throw new JaloEffectSignal("error", "Expected number");
    }
}
