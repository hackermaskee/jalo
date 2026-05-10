package org.bsdclub.furuta.jalo.evaluator;

import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.builtins.Callable;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Closure value representing a jalo function ({@code (fn ...)}).
 *
 * <p>Layer: Evaluation (value type, per DESIGN.md §1).
 * Captures lexical environment at function creation time.
 *
 * @see Evaluator
 * @see Environment
 * @see <a href="../../../docs/SPEC.md#42">SPEC §4.2 fn special form</a>
 */
public final class JaloFunction implements JaloValue, Callable {
    private final List<String> params;
    private final String restParam;
    private final List<JsonValue> body;
    private final Environment closure;

    /**
     * Creates a closure.
     *
     * @param params fixed positional parameter names
     * @param restParam optional rest parameter name, or {@code null}
     * @param body function body forms
     * @param closure captured lexical environment
     */
    public JaloFunction(List<String> params, String restParam, List<JsonValue> body, Environment closure) {
        this.params = params;
        this.restParam = restParam;
        this.body = body;
        this.closure = closure;
    }

    /**
     * Applies this closure to evaluated arguments.
     *
     * @param args evaluated arguments
     * @param evaluator evaluator used to run body forms
     * @return result of the last body expression
     * @implSpec Binds parameters and rest argument to the captured closure
     *           environment, then evaluates the body in sequence. The result
     *           of the last body expression is returned.
     * @throws JaloEffectSignal if arity is invalid or rest arguments contain non-JSON values
     */
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

    /**
     * Builds a closure from an {@code fn} form AST.
     *
     * @param fnForm parsed {@code fn} form
     * @param closure lexical environment to capture
     * @return closure value
     * @throws JaloEffectSignal if the function form is malformed
     */
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
