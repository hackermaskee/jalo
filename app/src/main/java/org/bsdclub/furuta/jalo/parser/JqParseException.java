package org.bsdclub.furuta.jalo.parser;

/**
 * Represents jq transpilation failures raised while parsing #jq(...) reader macro.
 */
public final class JqParseException extends RuntimeException {
    private final int line;
    private final int col;

    /**
     * Creates a jq parse exception with source location metadata.
     *
     * @param message error detail message
     * @param line 1-based line number where the error occurred
     * @param col 1-based column number where the error occurred
     */
    public JqParseException(String message, int line, int col) {
        super(message);
        this.line = line;
        this.col = col;
    }

    /**
     * Returns the 1-based line number where jq parse failed.
     *
     * @return source line number
     */
    public int line() {
        return line;
    }

    /**
     * Returns the 1-based column number where jq parse failed.
     *
     * @return source column number
     */
    public int col() {
        return col;
    }
}
