package org.bsdclub.furuta.jalo.jq;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes a minimal jq-compatible filter subset for transpilation.
 *
 * <p>Supported tokens include field access, pipe, brackets, and identifiers required by PR-A.
 */
public final class JqLexer {
    /** Token kinds for PR-A jq subset. */
    public enum Kind { DOT, PIPE, LBRACKET, RBRACKET, LPAREN, RPAREN, IDENT, NUMBER, END }

    /**
     * Immutable token record.
     *
     * @param kind token kind
     * @param text source text
     */
    public record Token(Kind kind, String text) {}

    /**
     * Converts jq source text into tokens.
     *
     * @param source jq filter source
     * @return token sequence ending with {@link Kind#END}
     * @throws IllegalArgumentException when unsupported characters are present
     */
    public List<Token> tokenize(String source) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            switch (c) {
                case '.' -> { out.add(new Token(Kind.DOT, ".")); i++; }
                case '|' -> { out.add(new Token(Kind.PIPE, "|")); i++; }
                case '[' -> { out.add(new Token(Kind.LBRACKET, "[")); i++; }
                case ']' -> { out.add(new Token(Kind.RBRACKET, "]")); i++; }
                case '(' -> { out.add(new Token(Kind.LPAREN, "(")); i++; }
                case ')' -> { out.add(new Token(Kind.RPAREN, ")")); i++; }
                default -> {
                    if (Character.isDigit(c)) {
                        int j = i + 1;
                        while (j < source.length() && Character.isDigit(source.charAt(j))) {
                            j++;
                        }
                        out.add(new Token(Kind.NUMBER, source.substring(i, j)));
                        i = j;
                        continue;
                    }
                    if (Character.isAlphabetic(c) || c == '-' || c == '_' || c == '@') {
                        int j = i + 1;
                        while (j < source.length()) {
                            char d = source.charAt(j);
                            if (!Character.isAlphabetic(d) && !Character.isDigit(d) && d != '-' && d != '_' && d != '@') {
                                break;
                            }
                            j++;
                        }
                        out.add(new Token(Kind.IDENT, source.substring(i, j)));
                        i = j;
                        continue;
                    }
                    throw new IllegalArgumentException("unsupported jq token: " + c);
                }
            }
        }
        out.add(new Token(Kind.END, ""));
        return out;
    }
}
