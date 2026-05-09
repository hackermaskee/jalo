package org.bsdclub.furuta.jalo.parser;

import java.util.List;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
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
    public JsonValue parse(List<Token> tokens, ParseMode mode) {
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
    public JsonValue parseJson(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
        JsonValue value = parseValue();
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
    public JsonValue parseStandard(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;
        JsonValue value = parseStandardValue();
        if (!(peek() instanceof Token.Eof)) {
            Token token = peek();
            throw new ParserException("Unexpected token after standard value", token.line(), token.col());
        }
        return value;
    }

    private JsonValue parseStandardValue() {
        Token token = peek();
        return switch (token) {
            case Token.Null t -> { advance(); yield JsonNull.INSTANCE; }
            case Token.True t -> { advance(); yield JsonBool.TRUE; }
            case Token.False t -> { advance(); yield JsonBool.FALSE; }
            case Token.NumberDouble t -> { advance(); yield new JsonNumber(t.value()); }
            case Token.NumberInt t -> {
                advance();
                yield JsonArray.of(new JsonString("int"), new JsonNumber(t.value()));
            }
            case Token.NumberLong t -> {
                advance();
                yield JsonArray.of(new JsonString("long"), new JsonNumber(t.value()));
            }
            case Token.Str t -> { advance(); yield new JsonString(t.value()); }
            case Token.Identifier t -> { advance(); yield new JsonString(t.name()); }
            case Token.LBracket t -> parseStandardArray(Token.LBracket.class, Token.RBracket.class, "]");
            case Token.LParen t -> parseStandardArray(Token.LParen.class, Token.RParen.class, ")");
            case Token.LBrace t -> parseStandardObject();
            case Token.Eof t -> throw new ParserException("Unexpected EOF", t.line(), t.col());
            default -> throw new ParserException("Unexpected token", token.line(), token.col());
        };
    }

    private JsonArray parseStandardArray(Class<? extends Token> leftType, Class<? extends Token> rightType, String right) {
        expect(leftType, "Expected array opener");
        JsonArray arr = JsonArray.empty();
        while (true) {
            Token token = peek();
            if (rightType.isInstance(token)) {
                advance();
                return arr;
            }
            if (token instanceof Token.Eof eof) {
                throw new ParserException("Unexpected EOF: expected '" + right + "'", eof.line(), eof.col());
            }
            arr = arr.append(parseStandardValue());
            if (peek() instanceof Token.Comma) {
                advance();
            }
        }
    }

    private JsonObject parseStandardObject() {
        expect(Token.LBrace.class, "Expected '{'");
        JsonObject obj = JsonObject.empty();
        while (true) {
            Token token = peek();
            if (token instanceof Token.RBrace) {
                advance();
                return obj;
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
            obj = obj.put(key, parseStandardValue());
            if (peek() instanceof Token.Comma) {
                advance();
            }
        }
    }

    private JsonValue parseValue() {
        Token token = peek();
        return switch (token) {
            case Token.Null t -> { advance(); yield JsonNull.INSTANCE; }
            case Token.True t -> { advance(); yield JsonBool.TRUE; }
            case Token.False t -> { advance(); yield JsonBool.FALSE; }
            case Token.NumberDouble t -> { advance(); yield new JsonNumber(t.value()); }
            case Token.NumberInt t -> { advance(); yield new JsonNumber(t.value()); }
            case Token.NumberLong t -> { advance(); yield new JsonNumber(t.value()); }
            case Token.Str t -> { advance(); yield new JsonString(t.value()); }
            case Token.Identifier t -> {
                advance();
                yield switch (t.name()) {
                    case "null" -> JsonNull.INSTANCE;
                    case "true" -> JsonBool.TRUE;
                    case "false" -> JsonBool.FALSE;
                    default -> throw new ParserException("Unexpected identifier: " + t.name(), t.line(), t.col());
                };
            }
            case Token.LBracket t -> parseArray();
            case Token.LBrace t -> parseObject();
            case Token.Eof t -> throw new ParserException("Unexpected EOF", t.line(), t.col());
            default -> throw new ParserException("Unexpected token", token.line(), token.col());
        };
    }

    private JsonArray parseArray() {
        expect(Token.LBracket.class, "Expected '['");
        JsonArray arr = JsonArray.empty();
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

    private JsonObject parseObject() {
        expect(Token.LBrace.class, "Expected '{'");
        JsonObject obj = JsonObject.empty();
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
