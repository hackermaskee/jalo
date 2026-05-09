package org.bsdclub.furuta.jalo.parser;

/**
 * Represents a parsing failure detected by {@link Parser}.
 *
 * <p>Layer: Parser (per DESIGN.md §1 architecture table).
 * This exception reports fail-fast parser errors with source position metadata.
 */
public final class ParserException extends RuntimeException {
    private final int line;
    private final int col;

    /**
     * Creates a parser exception with source location metadata.
     *
     * @param message error detail message
     * @param line 1-based line number where the error occurred
     * @param col 1-based column number where the error occurred
     */
    public ParserException(String message, int line, int col) {
        super("Line " + line + ":" + col + " - " + message);
        this.line = line;
        this.col = col;
    }

    /**
     * Returns the 1-based line number where the parse error occurred.
     *
     * @return the error line number
     */
    public int line() {
        return line;
    }

    /**
     * Returns the 1-based column number where the parse error occurred.
     *
     * @return the error column number
     */
    public int col() {
        return col;
    }
}
