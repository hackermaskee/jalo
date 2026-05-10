package org.bsdclub.furuta.jalo.repl;

import java.util.OptionalInt;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Represents the outcome of evaluating jalo source through the REPL pipeline.
 *
 * <p>Layer: REPL (per DESIGN.md §1 architecture table).
 */
public sealed interface EvalResult permits EvalResult.Success, EvalResult.Failure {
    /**
     * Successful pipeline outcome.
     *
     * @param value the evaluated result
     */
    record Success(JaloValue value) implements EvalResult { }

    /**
     * Failed pipeline outcome with diagnostic metadata.
     *
     * @param kind    the category of error
     * @param message human-readable error description
     * @param line    source line number if available
     * @param col     source column number if available
     */
    record Failure(ErrorKind kind, String message, OptionalInt line, OptionalInt col) implements EvalResult { }

    /** Error stage classification for pipeline failures. */
    enum ErrorKind {
        LEX,
        PARSE,
        SYNTAX,
        EFFECT,
        INTERNAL
    }
}
