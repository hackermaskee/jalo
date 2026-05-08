package org.bsdclub.furuta.jalo.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.lexer.LexerException;
import org.junit.jupiter.api.Test;

class JsonParserTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private Object parse(String input) {
        return parser.parseJson(lexer.tokenize(input));
    }

    @Test void b1_null() { assertThat(parse("null")).isEqualTo(JsonNull.INSTANCE); }
    @Test void b2_true() { assertThat(parse("true")).isEqualTo(JsonBool.TRUE); }
    @Test void b3_false() { assertThat(parse("false")).isEqualTo(JsonBool.FALSE); }
    @Test void b4_zero() { assertThat(parse("0")).isEqualTo(new JsonNumber(0.0)); }
    @Test void b5_integer() { assertThat(parse("42")).isEqualTo(new JsonNumber(42.0)); }
    @Test void b6_negativeDecimal() { assertThat(parse("-3.14")).isEqualTo(new JsonNumber(-3.14)); }
    @Test void b7_exponent() { assertThat(parse("1e3")).isEqualTo(new JsonNumber(1000.0)); }
    @Test void b8_string() { assertThat(parse("\"hello\"")).isEqualTo(new JsonString("hello")); }
    @Test void b9_stringEscape() { assertThat(parse("\"a\\nb\"")).isEqualTo(new JsonString("a\nb")); }
    @Test void b10_emptyArray() { assertThat(parse("[]")).isEqualTo(JsonArray.empty()); }
    @Test void b11_array() { assertThat(parse("[1,2,3]")).isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0))); }
    @Test void b12_nestedArray() { assertThat(parse("[1,[2,3]]")).isEqualTo(JsonArray.of(new JsonNumber(1.0), JsonArray.of(new JsonNumber(2.0), new JsonNumber(3.0)))); }
    @Test void b13_emptyObject() { assertThat(parse("{}")).isEqualTo(JsonObject.empty()); }
    @Test void b14_object() { assertThat(parse("{\"a\":1}")).isEqualTo(JsonObject.empty().put("a", new JsonNumber(1.0))); }
    @Test void b15_objectMulti() { assertThat(parse("{\"a\":1,\"b\":2}")).isEqualTo(JsonObject.empty().put("a", new JsonNumber(1.0)).put("b", new JsonNumber(2.0))); }
    @Test void b16_leadingZero_lexerError() { assertThatThrownBy(() -> lexer.tokenize("01")).isInstanceOf(LexerException.class); }
    @Test void b17_trailingComma() { assertThatThrownBy(() -> parse("[1,2,]")).isInstanceOf(ParserException.class).hasMessageContaining("Trailing comma not allowed"); }
    @Test void b18_unclosedArray() { assertThatThrownBy(() -> parse("[1,2")).isInstanceOf(ParserException.class).hasMessageContaining("Unexpected EOF: expected ']'"); }
    @Test void b19_duplicateKey() { assertThatThrownBy(() -> parse("{\"a\":1,\"a\":2}")).isInstanceOf(ParserException.class).hasMessageContaining("Duplicate key: a"); }
    @Test void b20_missingColon() { assertThatThrownBy(() -> parse("{\"a\" 1}")).isInstanceOf(ParserException.class).hasMessageContaining("Expected ':' after key"); }
}
