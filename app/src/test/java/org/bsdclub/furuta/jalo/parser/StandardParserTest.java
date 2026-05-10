package org.bsdclub.furuta.jalo.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.junit.jupiter.api.Test;

class StandardParserTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private JsonValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test void c1_hashNull() { assertThat(parse("#null")).isEqualTo(JsonNull.INSTANCE); }
    @Test void c2_hashTrue() { assertThat(parse("#true")).isEqualTo(JsonBool.TRUE); }
    @Test void c3_hashFalse() { assertThat(parse("#false")).isEqualTo(JsonBool.FALSE); }
    @Test void c4_intSuffix() { assertThat(parse("42i")).isEqualTo(JsonArray.of(new JsonString("int"), new JsonNumber(42.0))); }
    @Test void c5_longSuffix() { assertThat(parse("42l")).isEqualTo(JsonArray.of(new JsonString("long"), new JsonNumber(42.0))); }
    @Test void c6_identifierLiteral() { assertThat(parse("foo")).isEqualTo(new JsonString("foo")); }
    @Test void c7_bracketArrayWhitespace() { assertThat(parse("[1 2 3]")).isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0))); }
    @Test void c8_parenArrayWhitespace() { assertThat(parse("(1 2 3)")).isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0))); }
    @Test void c9_bracketArrayComma() { assertThat(parse("[1, 2, 3]")).isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0))); }
    @Test void c10_objectWhitespace() { assertThat(parse("{a: 1 b: 2}")).isEqualTo(JsonObject.empty().put("a", new JsonNumber(1.0)).put("b", new JsonNumber(2.0))); }
    @Test void c11_objectComma() { assertThat(parse("{a: 1, b: 2}")).isEqualTo(JsonObject.empty().put("a", new JsonNumber(1.0)).put("b", new JsonNumber(2.0))); }
    @Test void c12_prefixCallLikeForm() { assertThat(parse("(+ 1 2)")).isEqualTo(JsonArray.of(new JsonString("+"), new JsonNumber(1.0), new JsonNumber(2.0))); }
    @Test
    void c13_nestedForm() {
        assertThat(parse("(if (= x 0) 1 2)"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("if"),
                    JsonArray.of(new JsonString("="), new JsonString("x"), new JsonNumber(0.0)),
                    new JsonNumber(1.0),
                    new JsonNumber(2.0)));
    }
    @Test void c14_jsonBoundary() { assertThat(parse("{\"a\": 1}")).isEqualTo(JsonObject.empty().put("a", new JsonNumber(1.0))); }
    @Test
    void c15_backquoteLiteral() {
        assertThat(parse("`42"))
            .isEqualTo(JsonArray.of(new JsonString("backquote"), new JsonNumber(42.0)));
    }

    @Test
    void c16_backquoteArrayLiteral() {
        assertThat(parse("`[1 2 3]"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("backquote"),
                    JsonArray.of(
                        new JsonString("array"),
                        new JsonNumber(1.0),
                        new JsonNumber(2.0),
                        new JsonNumber(3.0))));
    }

    @Test
    void c17_backquoteArrayWithDollarAndAt() {
        assertThat(parse("`[$x @arr]"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("backquote"),
                    JsonArray.of(
                        new JsonString("array"),
                        JsonArray.of(new JsonString("dollar"), new JsonString("x")),
                        JsonArray.of(new JsonString("at"), new JsonString("arr")))));
    }

    @Test
    void c18_backquoteMapWithDollarValue() {
        assertThat(parse("`{name: $n}"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("backquote"),
                    JsonArray.of(
                        new JsonString("map"),
                        JsonArray.of(
                            new JsonString("name"),
                            JsonArray.of(new JsonString("dollar"), new JsonString("n"))))));
    }

    @Test
    void c19_dollarOutsideBackquoteFails() {
        assertThatThrownBy(() -> parse("$x"))
            .isInstanceOf(ParserException.class)
            .hasMessageContaining("$ outside backquote context");
    }

    @Test
    void c20_quoteShorthandIdentifier() {
        assertThat(parse("'a"))
            .isEqualTo(JsonArray.of(new JsonString("quote"), new JsonString("a")));
    }

    @Test
    void c21_quoteShorthandNumber() {
        assertThat(parse("'42"))
            .isEqualTo(JsonArray.of(new JsonString("quote"), new JsonNumber(42.0)));
    }

    @Test
    void c22_quoteShorthandList() {
        assertThat(parse("'(a b c)"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("quote"),
                    JsonArray.of(new JsonString("a"), new JsonString("b"), new JsonString("c"))));
    }

    @Test
    void c23_quoteShorthandArray() {
        assertThat(parse("'[1 2 3]"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("quote"),
                    JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0))));
    }

    @Test
    void c24_quoteShorthandMap() {
        assertThat(parse("'{a: 1}"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("quote"),
                    JsonObject.empty().put("a", new JsonNumber(1.0))));
    }

    @Test
    void c25_quoteOfQuote() {
        assertThat(parse("''a"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("quote"),
                    JsonArray.of(new JsonString("quote"), new JsonString("a"))));
    }

    @Test
    void c26_quoteShorthandHashNull() {
        assertThat(parse("'#null"))
            .isEqualTo(
                JsonArray.of(
                    new JsonString("quote"),
                    JsonNull.INSTANCE));
    }
}
