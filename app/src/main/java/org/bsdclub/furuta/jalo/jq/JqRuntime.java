package org.bsdclub.furuta.jalo.jq;

import org.bsdclub.furuta.jalo.evaluator.Environment;
import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * jq-mode execution entrypoint: jq text -> transpiled jalo AST -> evaluator.
 */
public final class JqRuntime {
    private final JqLexer lexer;
    private final JqParser parser;
    private final Evaluator evaluator;

    /** Creates a runtime with default lexer/parser/evaluator components. */
    public JqRuntime() {
        this.lexer = new JqLexer();
        this.parser = new JqParser();
        this.evaluator = new Evaluator();
    }

    /**
     * Evaluates a jq filter against one input value bound as {@code x}.
     *
     * @param filter jq filter expression
     * @param input input value
     * @return evaluated jalo runtime value
     */
    public JaloValue eval(String filter, JaloValue input) {
        JaloValue ast = parser.transpile(filter);
        Environment env = Environment.root().bind("x", input);
        return evaluator.eval(ast, env);
    }
}
