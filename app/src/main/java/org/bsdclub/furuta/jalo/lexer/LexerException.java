package org.bsdclub.furuta.jalo.lexer;

/**
 * Represents a lexical analysis failure detected by {@link Lexer}.
 *
 * <p>Layer: Lexer (per DESIGN.md §1 architecture table).
 * This exception reports fail-fast lexer errors with source position metadata.
 */
public final class LexerException extends RuntimeException {
    private final int line;
    private final int col;

    /**
     * Creates a lexer exception with source location metadata.
     *
     * @param message error detail message
     * @param line 1-based line number where the error occurred
     * @param col 1-based column number where the error occurred
     */
    public LexerException(String message, int line, int col) {
        super("Line " + line + ":" + col + " - " + message);
        this.line = line;
        this.col = col;
    }

    /**
     * Returns the 1-based line number where the lexical error occurred.
     *
     * @return the error line number
     */
    public int line() { return line; }

    /**
     * Returns the 1-based column number where the lexical error occurred.
     *
     * @return the error column number
     */
    public int col() { return col; }
}
