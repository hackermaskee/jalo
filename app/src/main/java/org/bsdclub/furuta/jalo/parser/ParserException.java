package org.bsdclub.furuta.jalo.parser;

public final class ParserException extends RuntimeException {
    private final int line;
    private final int col;

    public ParserException(String message, int line, int col) {
        super("Line " + line + ":" + col + " - " + message);
        this.line = line;
        this.col = col;
    }

    public int line() {
        return line;
    }

    public int col() {
        return col;
    }
}
