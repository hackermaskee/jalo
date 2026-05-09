package org.bsdclub.furuta.jalo.syntaxcheck;

/**
 * Exception thrown when a JSON model AST violates jalo syntax constraints.
 *
 * <p>Layer: SyntaxChecker (per DESIGN.md §1 architecture table).
 */
public final class SyntaxCheckException extends RuntimeException {
    /**
     * Creates a new syntax-check exception.
     *
     * @param message human-readable violation detail
     */
    public SyntaxCheckException(String message) {
        super(message);
    }
}
