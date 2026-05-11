package org.bsdclub.furuta.jalo.parser;

import java.util.List;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.lexer.Token;

/**
 * Parses token streams into JSON-model trees for jalo syntax modes.
 *
 * <p>Layer: Parser (per DESIGN.md §1 architecture table).
 * This parser supports JSON mode and standard mode according to SPEC §3.2 and §4.
 *
 * @see org.bsdclub.furuta.jalo.lexer.Lexer
 */
public final class Parser {
    /**
     * Represents parser entry modes for JSON and standard syntax.
     */
    public enum ParseMode { JSON, STANDARD }

    private List<Token> tokens;
    private int index;

    /**
     * Parses tokens with the specified syntax mode.
     *
     * @param tokens token sequence ending with EOF
     * @param mode parsing mode that determines grammar selection
     * @return the parsed JSON-model tree
     * @throws ParserException if the token stream violates the selected grammar (SPEC §3.2, §4)
     */
    public JaloValue parse(List<Token> tokens, ParseMode mode) {
        return switch (mode) {
            case JSON -> parseJson(tokens);
            case STANDARD -> parseStandard(tokens);
        };
    }

    /**
     * Parses tokens using strict JSON grammar.
     *
     * @param tokens token sequence ending with EOF
     * @return the parsed JSON value
     * @throws ParserException if the token stream is not valid JSON (SPEC §3.2)
     */
    public JaloValue parseJson(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
        JaloValue value = parseValue();
        if (!(peek() instanceof Token.Eof)) {
            Token token = peek();
            throw new ParserException("Unexpected token after JSON value", token.line(), token.col());
        }
        return value;
    }

    /**
     * Parses tokens using jalo standard syntax grammar.
     *
     * @param tokens token sequence ending with EOF
     * @return the parsed standard-syntax value normalized to JSON model
     * @throws ParserException if the token stream violates standard syntax rules (SPEC §4)
     *
     * @implNote Requires explicit mode selection by callers to avoid ambiguous auto-detection.
     */
    public JaloValue parseStandard(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
        JaloValue value = parseExpr(false);
        if (!(peek() instanceof Token.Eof)) {
            Token token = peek();
            throw new ParserException("Unexpected token after standard value", token.line(), token.col());
        }
        return value;
    }

    /**
     * Parses standard expressions, including backquote sugar forms (SPEC §5.5).
     *
     * @param inBackquote true when parsing inside a backquote context
     * @return parsed expression
     * @throws ParserException if parsing fails due to invalid syntax
     */
    private JaloValue parseExpr(boolean inBackquote) {
        Token token = peek();
        return switch (token) {
            case Token.Null t -> { advance(); yield JaloNull.INSTANCE; }
            case Token.True t -> { advance(); yield JaloBool.TRUE; }
            case Token.False t -> { advance(); yield JaloBool.FALSE; }
            case Token.NumberDouble t -> { advance(); yield new JaloNumber(t.value()); }
            case Token.NumberInt t -> { advance(); yield new JaloInt(t.value()); }
            case Token.NumberLong t -> { advance(); yield new JaloLong(t.value()); }
            case Token.Str t -> { advance(); yield new JaloString(t.value()); }
            case Token.Identifier t -> { advance(); yield new JaloString(t.name()); }
            case Token.Quote t -> {
                advance();
                Token next = peek();
                if (next instanceof Token.Eof || next instanceof Token.RParen || next instanceof Token.RBracket) {
                    throw new ParserException("unexpected end of expression after quote shorthand", next.line(), next.col());
                }
                yield JaloArray.of(new JaloString("quote"), parseExpr(inBackquote));
            }
            case Token.Backquote t -> {
                advance();
                yield JaloArray.of(new JaloString("backquote"), parseExpr(true));
            }
            case Token.Dollar t -> {
                if (!inBackquote) {
                    throw new ParserException("$ outside backquote context (SPEC §5.5)", t.line(), t.col());
                }
                advance();
                yield JaloArray.of(new JaloString("dollar"), parseExpr(false));
            }
            case Token.At t -> {
                if (!inBackquote) {
                    throw new ParserException("@ outside backquote context (SPEC §5.5)", t.line(), t.col());
                }
                advance();
                yield JaloArray.of(new JaloString("at"), parseExpr(false));
            }
            case Token.Percent t -> {
                if (!inBackquote) {
                    throw new ParserException("% outside backquote context (SPEC §5.5)", t.line(), t.col());
                }
                advance();
                yield JaloArray.of(new JaloString("percent"), parseExpr(false));
            }
            case Token.LBracket t -> parseStandardArray(Token.LBracket.class, Token.RBracket.class, "]", inBackquote);
            case Token.LParen t -> parseStandardArray(Token.LParen.class, Token.RParen.class, ")");
            case Token.LBrace t -> parseStandardObject(inBackquote);
            case Token.Eof t -> throw new ParserException("Unexpected EOF", t.line(), t.col());
            default -> throw new ParserException("Unexpected token", token.line(), token.col());
        };
    }

    private JaloArray parseStandardArray(Class<? extends Token> leftType, Class<? extends Token> rightType, String right) {
        return parseStandardArray(leftType, rightType, right, false);
    }

    private JaloArray parseStandardArray(
            Class<? extends Token> leftType, Class<? extends Token> rightType, String right, boolean inBackquote) {
        expect(leftType, "Expected array opener");
        JaloArray arr = inBackquote && leftType.equals(Token.LBracket.class)
                ? JaloArray.of(new JaloString("array"))
                : JaloArray.empty();
        while (true) {
            Token token = peek();
            if (rightType.isInstance(token)) {
                advance();
                return arr;
            }
            if (token instanceof Token.Eof eof) {
                throw new ParserException("Unexpected EOF: expected '" + right + "'", eof.line(), eof.col());
            }
            arr = arr.append(parseExpr(inBackquote));
            if (peek() instanceof Token.Comma) {
                advance();
            }
        }
    }

    private JaloValue parseStandardObject(boolean inBackquote) {
        expect(Token.LBrace.class, "Expected '{'");
        JaloMap obj = JaloMap.empty();
        JaloArray mapForm = JaloArray.of(new JaloString("map"));
        while (true) {
            Token token = peek();
            if (token instanceof Token.RBrace) {
                advance();
                return inBackquote ? mapForm : obj;
            }
            if (token instanceof Token.Eof eof) {
                throw new ParserException("Unexpected EOF: expected '}'", eof.line(), eof.col());
            }

            String key = switch (token) {
                case Token.Identifier id -> {
                    advance();
                    yield id.name();
                }
                case Token.Str str -> {
                    advance();
                    yield str.value();
                }
                default -> throw new ParserException("Expected object key", token.line(), token.col());
            };

            Token colon = peek();
            if (!(colon instanceof Token.Colon)) {
                throw new ParserException("Expected ':' after key", colon.line(), colon.col());
            }
            advance();
            JaloValue value = parseExpr(inBackquote);
            if (inBackquote) {
                mapForm = mapForm.append(JaloArray.of(new JaloString(key), value));
            } else {
                obj = obj.put(key, value);
            }
            if (peek() instanceof Token.Comma) {
                advance();
            }
        }
    }

    private JaloValue parseValue() {
        Token token = peek();
        return switch (token) {
            case Token.Null t -> { advance(); yield JaloNull.INSTANCE; }
            case Token.True t -> { advance(); yield JaloBool.TRUE; }
            case Token.False t -> { advance(); yield JaloBool.FALSE; }
            case Token.NumberDouble t -> { advance(); yield new JaloNumber(t.value()); }
            case Token.NumberInt t -> { advance(); yield new JaloNumber(t.value()); }
            case Token.NumberLong t -> { advance(); yield new JaloNumber(t.value()); }
            case Token.Str t -> { advance(); yield new JaloString(t.value()); }
            case Token.Identifier t -> {
                advance();
                yield switch (t.name()) {
                    case "null" -> JaloNull.INSTANCE;
                    case "true" -> JaloBool.TRUE;
                    case "false" -> JaloBool.FALSE;
                    default -> throw new ParserException("Unexpected identifier: " + t.name(), t.line(), t.col());
                };
            }
            case Token.LBracket t -> parseArray();
            case Token.LBrace t -> parseObject();
            case Token.Eof t -> throw new ParserException("Unexpected EOF", t.line(), t.col());
            default -> throw new ParserException("Unexpected token", token.line(), token.col());
        };
    }

    private JaloArray parseArray() {
        expect(Token.LBracket.class, "Expected '['");
        JaloArray arr = JaloArray.empty();
        if (peek() instanceof Token.RBracket) {
            advance();
            return arr;
        }

        while (true) {
            arr = arr.append(parseValue());
            if (peek() instanceof Token.Comma) {
                Token comma = advance();
                if (peek() instanceof Token.RBracket) {
                    throw new ParserException("Trailing comma not allowed", comma.line(), comma.col());
                }
                continue;
            }
            if (peek() instanceof Token.RBracket) {
                advance();
                return arr;
            }
            if (peek() instanceof Token.Eof eof) {
                throw new ParserException("Unexpected EOF: expected ']'", eof.line(), eof.col());
            }
            Token token = peek();
            throw new ParserException("Expected ',' or ']'", token.line(), token.col());
        }
    }

    private JaloMap parseObject() {
        expect(Token.LBrace.class, "Expected '{'");
        JaloMap obj = JaloMap.empty();
        if (peek() instanceof Token.RBrace) {
            advance();
            return obj;
        }

        while (true) {
            Token keyToken = peek();
            if (!(keyToken instanceof Token.Str key)) {
                throw new ParserException("Expected string key", keyToken.line(), keyToken.col());
            }
            advance();
            if (!(peek() instanceof Token.Colon)) {
                Token t = peek();
                throw new ParserException("Expected ':' after key", t.line(), t.col());
            }
            advance();
            if (obj.containsKey(key.value())) {
                throw new ParserException("Duplicate key: " + key.value(), key.line(), key.col());
            }
            obj = obj.put(key.value(), parseValue());

            if (peek() instanceof Token.Comma) {
                Token comma = advance();
                if (peek() instanceof Token.RBrace) {
                    throw new ParserException("Trailing comma not allowed", comma.line(), comma.col());
                }
                continue;
            }
            if (peek() instanceof Token.RBrace) {
                advance();
                return obj;
            }
            if (peek() instanceof Token.Eof eof) {
                throw new ParserException("Unexpected EOF: expected '}'", eof.line(), eof.col());
            }
            Token token = peek();
            throw new ParserException("Expected ',' or '}'", token.line(), token.col());
        }
    }

    private <T extends Token> T expect(Class<T> type, String message) {
        Token token = peek();
        if (!type.isInstance(token)) {
            throw new ParserException(message, token.line(), token.col());
        }
        return type.cast(advance());
    }

    private Token peek() {
        return tokens.get(index);
    }

    private Token advance() {
        return tokens.get(index++);
    }
}
