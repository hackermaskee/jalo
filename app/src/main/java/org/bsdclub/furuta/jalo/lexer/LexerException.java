package org.bsdclub.furuta.jalo.lexer;

public final class LexerException extends RuntimeException {
    private final int line;
    private final int col;

    public LexerException(String message, int line, int col) {
        super("Line " + line + ":" + col + " - " + message);
        this.line = line;
        this.col = col;
    }

    public int line() { return line; }
    public int col() { return col; }
}
