package org.bsdclub.furuta.jalo.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes jalo source text into a parser-consumable token sequence.
 *
 * <p>Layer: Lexer (per DESIGN.md §1 architecture table).
 * This class applies the lexical rules defined in SPEC §3.
 *
 * @see Token
 */
public final class Lexer {
    private String input;
    private int idx;
    private int line;
    private int col;

    /**
     * Tokenizes input text and appends an EOF token at the end.
     *
     * @param input source text to tokenize; {@code null} is treated as an empty string
     * @return the token sequence including the trailing EOF token
     * @throws LexerException if the input contains an invalid token form (SPEC §3.1)
     */
    public List<Token> tokenize(String input) {
        this.input = input == null ? "" : input;
        this.idx = 0;
        this.line = 1;
        this.col = 1;

        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            char ch = current();
            if (Character.isWhitespace(ch)) {
                advance();
                continue;
            }

            int startLine = line;
            int startCol = col;

            switch (ch) {
                case '[' -> {
                    advance();
                    tokens.add(new Token.LBracket(startLine, startCol));
                }
                case ']' -> {
                    advance();
                    tokens.add(new Token.RBracket(startLine, startCol));
                }
                case '(' -> {
                    advance();
                    tokens.add(new Token.LParen(startLine, startCol));
                }
                case ')' -> {
                    advance();
                    tokens.add(new Token.RParen(startLine, startCol));
                }
                case '{' -> {
                    advance();
                    tokens.add(new Token.LBrace(startLine, startCol));
                }
                case '}' -> {
                    advance();
                    tokens.add(new Token.RBrace(startLine, startCol));
                }
                case ':' -> {
                    advance();
                    tokens.add(new Token.Colon(startLine, startCol));
                }
                case ',' -> {
                    advance();
                    tokens.add(new Token.Comma(startLine, startCol));
                }
                case '\'' -> {
                    advance();
                    tokens.add(new Token.Quote(startLine, startCol));
                }
                case '`' -> {
                    advance();
                    tokens.add(new Token.Backquote(startLine, startCol));
                }
                case '$' -> {
                    advance();
                    tokens.add(new Token.Dollar(startLine, startCol));
                }
                case '@' -> {
                    advance();
                    tokens.add(new Token.At(startLine, startCol));
                }
                case '%' -> {
                    advance();
                    tokens.add(new Token.Percent(startLine, startCol));
                }
                case '#' -> tokens.add(parseHashLiteral(startLine, startCol));
                case '"' -> tokens.add(parseString(startLine, startCol));
                default -> {
                    if (isNumberStart(ch)) {
                        tokens.add(parseNumber(startLine, startCol));
                    } else if (isIdentifierStart(ch)) {
                        tokens.add(parseIdentifier(startLine, startCol));
                    } else {
                        throw new LexerException("unexpected character: " + ch, startLine, startCol);
                    }
                }
            }
        }

        tokens.add(new Token.Eof(line, col));
        return tokens;
    }

    private Token parseHashLiteral(int startLine, int startCol) {
        advance(); // '#'
        if (!isAtEnd() && current() == '[') {
            advance();
            return new Token.HashBracketOpen(startLine, startCol);
        }
        if (!isAtEnd() && current() == '{') {
            advance();
            return new Token.HashCurlyOpen(startLine, startCol);
        }
        int literalStart = idx;
        while (!isAtEnd() && Character.isLetter(current())) {
            advance();
        }
        String name = input.substring(literalStart, idx);
        return switch (name) {
            case "null" -> new Token.Null(startLine, startCol);
            case "true" -> new Token.True(startLine, startCol);
            case "false" -> new Token.False(startLine, startCol);
            default -> throw new LexerException("unknown hash literal: #" + name, startLine, startCol);
        };
    }

    private Token parseString(int startLine, int startCol) {
        advance(); // opening quote
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char ch = current();
            if (ch == '"') {
                advance();
                return new Token.Str(sb.toString(), startLine, startCol);
            }
            if (ch == '\\') {
                advance();
                if (isAtEnd()) {
                    throw new LexerException("unterminated string", startLine, startCol);
                }
                sb.append(parseEscape(startLine, startCol));
                continue;
            }
            if (ch == '\n' || ch == '\r') {
                throw new LexerException("unterminated string", startLine, startCol);
            }
            sb.append(ch);
            advance();
        }
        throw new LexerException("unterminated string", startLine, startCol);
    }

    private char parseEscape(int startLine, int startCol) {
        char esc = current();
        advance();
        return switch (esc) {
            case '"' -> '"';
            case '\\' -> '\\';
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case 'u' -> parseUnicodeEscape(startLine, startCol);
            default -> throw new LexerException("invalid escape: \\" + esc, startLine, startCol);
        };
    }

    private char parseUnicodeEscape(int startLine, int startCol) {
        if (idx + 4 > input.length()) {
            throw new LexerException("invalid unicode escape", startLine, startCol);
        }
        String hex = input.substring(idx, idx + 4);
        for (int i = 0; i < 4; i++) {
            if (Character.digit(hex.charAt(i), 16) < 0) {
                throw new LexerException("invalid unicode escape", startLine, startCol);
            }
        }
        idx += 4;
        col += 4;
        return (char) Integer.parseInt(hex, 16);
    }

    private Token parseNumber(int startLine, int startCol) {
        int start = idx;
        if ((current() == '+' || current() == '-') && hasNext() && Character.isDigit(peekNext())) {
            advance();
        }

        if (isAtEnd() || !Character.isDigit(current())) {
            throw new LexerException("invalid number", startLine, startCol);
        }

        int intStart = idx;
        while (!isAtEnd() && Character.isDigit(current())) {
            advance();
        }

        int intLen = idx - intStart;
        char firstDigit = input.charAt(intStart);
        if (firstDigit == '0' && intLen > 1) {
            throw new LexerException("leading zero", startLine, startCol);
        }

        boolean hasFraction = false;
        if (!isAtEnd() && current() == '.') {
            hasFraction = true;
            advance();
            if (isAtEnd() || !Character.isDigit(current())) {
                throw new LexerException("invalid number", startLine, startCol);
            }
            while (!isAtEnd() && Character.isDigit(current())) {
                advance();
            }
        }

        boolean hasExponent = false;
        if (!isAtEnd() && (current() == 'e' || current() == 'E')) {
            hasExponent = true;
            advance();
            if (!isAtEnd() && (current() == '+' || current() == '-')) {
                advance();
            }
            if (isAtEnd() || !Character.isDigit(current())) {
                throw new LexerException("invalid exponent", startLine, startCol);
            }
            while (!isAtEnd() && Character.isDigit(current())) {
                advance();
            }
        }

        if (!isAtEnd() && (current() == 'i' || current() == 'l' || current() == 'd')) {
            char suffix = current();
            if (hasFraction || hasExponent) {
                throw new LexerException("suffix not allowed for floating number", startLine, startCol);
            }
            advance();
            String numeric = input.substring(start, idx - 1);
            try {
                if (suffix == 'i') {
                    return new Token.NumberInt(Integer.parseInt(numeric), startLine, startCol);
                }
                if (suffix == 'l') {
                    return new Token.NumberLong(Long.parseLong(numeric), startLine, startCol);
                }
                return new Token.NumberDouble(Double.parseDouble(numeric), startLine, startCol);
            } catch (NumberFormatException ex) {
                throw new LexerException("invalid integer literal", startLine, startCol);
            }
        }

        String numeric = input.substring(start, idx);
        if (!hasFraction && !hasExponent) {
            try {
                long value = Long.parseLong(numeric);
                if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                    return new Token.NumberInt((int) value, startLine, startCol);
                }
                return new Token.NumberLong(value, startLine, startCol);
            } catch (NumberFormatException ex) {
                throw new LexerException("Number literal out of long range: " + numeric, startLine, startCol);
            }
        }
        try {
            return new Token.NumberDouble(Double.parseDouble(numeric), startLine, startCol);
        } catch (NumberFormatException ex) {
            throw new LexerException("invalid number", startLine, startCol);
        }
    }

    private Token parseIdentifier(int startLine, int startCol) {
        int start = idx;
        advance();
        while (!isAtEnd() && isIdentifierRest(current())) {
            advance();
        }
        return new Token.Identifier(input.substring(start, idx), startLine, startCol);
    }

    private boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch)
            || ch == '_'
            || ch == '+' || ch == '-' || ch == '*' || ch == '/'
            || ch == '=' || ch == '<' || ch == '>' || ch == '!' || ch == '?';
    }

    private boolean isIdentifierRest(char ch) {
        return isIdentifierStart(ch) || Character.isDigit(ch) || ch == '.' || ch == '\'';
    }

    private boolean isNumberStart(char ch) {
        if (Character.isDigit(ch)) {
            return true;
        }
        return (ch == '+' || ch == '-') && hasNext() && Character.isDigit(peekNext());
    }

    private boolean isAtEnd() {
        return idx >= input.length();
    }

    private char current() {
        return input.charAt(idx);
    }

    private boolean hasNext() {
        return idx + 1 < input.length();
    }

    private char peekNext() {
        return input.charAt(idx + 1);
    }

    private void advance() {
        char ch = input.charAt(idx++);
        if (ch == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
    }
}
