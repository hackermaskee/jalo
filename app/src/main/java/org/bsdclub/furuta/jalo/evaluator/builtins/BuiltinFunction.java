package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.Environment;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Built-in function contract evaluated by {@link org.bsdclub.furuta.jalo.evaluator.Evaluator}.
 */
@FunctionalInterface
public interface BuiltinFunction {
    /**
     * Applies this built-in function with already evaluated arguments.
     *
     * @param args argument values
     * @param env lexical environment of the call site
     * @return evaluated result value
     */
    JaloValue apply(List<JaloValue> args, Environment env);
}
