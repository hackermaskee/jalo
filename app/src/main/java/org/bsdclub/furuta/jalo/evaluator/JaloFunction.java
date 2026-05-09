package org.bsdclub.furuta.jalo.evaluator;

import java.util.List;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloValue;

public final class JaloFunction implements JaloValue {
    private final List<String> params;
    private final String restParam;
    private final List<JsonValue> body;
    private final Environment closure;

    public JaloFunction(List<String> params, String restParam, List<JsonValue> body, Environment closure) {
        this.params = params;
        this.restParam = restParam;
        this.body = body;
        this.closure = closure;
    }

    public JaloValue apply(List<JaloValue> args, Evaluator evaluator) {
        if (restParam == null && args.size() != params.size()) {
            throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("Arity mismatch"));
        }
        if (restParam != null && args.size() < params.size()) {
            throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("Arity mismatch"));
        }

        Environment env = closure;
        for (int i = 0; i < params.size(); i++) {
            env = env.bind(params.get(i), args.get(i));
        }
        if (restParam != null) {
            JsonArray rest = JsonArray.empty();
            for (int i = params.size(); i < args.size(); i++) {
                JaloValue v = args.get(i);
                if (!(v instanceof JsonValue jsonValue)) {
                    throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("Non-JSON value in rest args"));
                }
                rest = rest.append(jsonValue);
            }
            env = env.bind(restParam, rest);
        }

        JaloValue result = null;
        for (JsonValue form : body) {
            result = evaluator.eval(form, env);
        }
        return result;
    }

    public static JaloFunction fromForm(JsonArray fnForm, Environment closure) {
        if (fnForm.size() < 3 || !(fnForm.get(1) instanceof JsonArray paramVec)) {
            throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("Malformed fn"));
        }

        java.util.ArrayList<String> params = new java.util.ArrayList<>();
        String rest = null;
        for (int i = 0; i < paramVec.size(); i++) {
            JsonValue pv = paramVec.get(i);
            if (!(pv instanceof JsonString p)) {
                throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("fn params must be identifiers"));
            }
            if ("&".equals(p.value())) {
                if (i != paramVec.size() - 2 || !(paramVec.get(i + 1) instanceof JsonString restName)) {
                    throw new JaloEffectSignal(new org.bsdclub.furuta.jalo.json.JsonString("error"), new org.bsdclub.furuta.jalo.json.JsonString("Malformed fn rest parameter"));
                }
                rest = restName.value();
                break;
            }
            params.add(p.value());
        }

        java.util.ArrayList<JsonValue> body = new java.util.ArrayList<>();
        for (int i = 2; i < fnForm.size(); i++) {
            body.add(fnForm.get(i));
        }
        return new JaloFunction(params, rest, body, closure);
    }
}
