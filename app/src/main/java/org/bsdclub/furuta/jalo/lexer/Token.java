package org.bsdclub.furuta.jalo.lexer;

/**
 * Represents a token produced by lexical analysis for jalo source text.
 *
 * <p>Layer: Lexer (per DESIGN.md §1 architecture table).
 * This sealed interface models all lexical token variants consumed by the parser.
 *
 * @see Lexer
 */
public sealed interface Token permits
    Token.NumberDouble, Token.NumberInt, Token.NumberLong,
    Token.Null, Token.True, Token.False,
    Token.Str, Token.Identifier,
    Token.LBracket, Token.RBracket,
    Token.HashBracketOpen, Token.HashCurlyOpen,
    Token.HashJqText,
    Token.LParen, Token.RParen,
    Token.LBrace, Token.RBrace,
    Token.Colon, Token.Comma,
    Token.Quote, Token.Backquote, Token.Dollar, Token.At, Token.Percent,
    Token.Eof {

    /**
     * Returns the 1-based line number where this token starts.
     *
     * @return the source line number
     */
    int line();

    /**
     * Returns the 1-based column number where this token starts.
     *
     * @return the source column number
     */
    int col();

    record NumberDouble(double value, int line, int col) implements Token {}
    record NumberInt(int value, int line, int col) implements Token {}
    record NumberLong(long value, int line, int col) implements Token {}
    record Null(int line, int col) implements Token {}
    record True(int line, int col) implements Token {}
    record False(int line, int col) implements Token {}
    record Str(String value, int line, int col) implements Token {}
    record Identifier(String name, int line, int col) implements Token {}
    record LBracket(int line, int col) implements Token {}
    record RBracket(int line, int col) implements Token {}
    record HashBracketOpen(int line, int col) implements Token {}
    record HashCurlyOpen(int line, int col) implements Token {}
    record HashJqText(String payload, int line, int col) implements Token {}
    record LParen(int line, int col) implements Token {}
    record RParen(int line, int col) implements Token {}
    record LBrace(int line, int col) implements Token {}
    record RBrace(int line, int col) implements Token {}
    record Colon(int line, int col) implements Token {}
    record Comma(int line, int col) implements Token {}
    record Quote(int line, int col) implements Token {}
    record Backquote(int line, int col) implements Token {}
    record Dollar(int line, int col) implements Token {}
    record At(int line, int col) implements Token {}
    record Percent(int line, int col) implements Token {}
    record Eof(int line, int col) implements Token {}
}
