package org.bsdclub.furuta.jalo.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.junit.jupiter.api.Test;

class StandardParserTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private JaloValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test void c1_hashNull() { assertThat(parse("#null")).isEqualTo(JaloNull.INSTANCE); }
    @Test void c2_hashTrue() { assertThat(parse("#true")).isEqualTo(JaloBool.TRUE); }
    @Test void c3_hashFalse() { assertThat(parse("#false")).isEqualTo(JaloBool.FALSE); }
    @Test void c4_intSuffix() { assertThat(parse("42i")).isEqualTo(new JaloInt(42)); }
    @Test void c5_longSuffix() { assertThat(parse("42l")).isEqualTo(new JaloLong(42L)); }
    @Test void c6_identifierLiteral() { assertThat(parse("foo")).isEqualTo(new JaloString("foo")); }
    @Test void c7_bracketArrayWhitespace() { assertThat(parse("[1 2 3]")).isEqualTo(JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3))); }
    @Test void c8_parenArrayWhitespace() { assertThat(parse("(1 2 3)")).isEqualTo(JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3))); }
    @Test void c9_bracketArrayComma() { assertThat(parse("[1, 2, 3]")).isEqualTo(JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3))); }
    @Test void c10_objectWhitespace() { assertThat(parse("{a: 1 b: 2}")).isEqualTo(JaloMap.empty().put("a", new JaloInt(1)).put("b", new JaloInt(2))); }
    @Test void c11_objectComma() { assertThat(parse("{a: 1, b: 2}")).isEqualTo(JaloMap.empty().put("a", new JaloInt(1)).put("b", new JaloInt(2))); }
    @Test void c12_prefixCallLikeForm() { assertThat(parse("(+ 1 2)")).isEqualTo(JaloArray.of(new JaloString("+"), new JaloInt(1), new JaloInt(2))); }
    @Test
    void c13_nestedForm() {
        assertThat(parse("(if (= x 0) 1 2)"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("if"),
                    JaloArray.of(new JaloString("="), new JaloString("x"), new JaloInt(0)),
                    new JaloInt(1),
                    new JaloInt(2)));
    }
    @Test void c14_jsonBoundary() { assertThat(parse("{\"a\": 1}")).isEqualTo(JaloMap.empty().put("a", new JaloInt(1))); }
    @Test
    void c15_backquoteLiteral() {
        assertThat(parse("`42"))
            .isEqualTo(JaloArray.of(new JaloString("backquote"), new JaloInt(42)));
    }

    @Test
    void c16_backquoteArrayLiteral() {
        assertThat(parse("`[1 2 3]"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("backquote"),
                    JaloArray.of(
                        new JaloString("array"),
                        new JaloInt(1),
                        new JaloInt(2),
                        new JaloInt(3))));
    }

    @Test
    void c17_backquoteArrayWithDollarAndAt() {
        assertThat(parse("`[$x @arr]"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("backquote"),
                    JaloArray.of(
                        new JaloString("array"),
                        JaloArray.of(new JaloString("dollar"), new JaloString("x")),
                        JaloArray.of(new JaloString("at"), new JaloString("arr")))));
    }

    @Test
    void c18_backquoteMapWithDollarValue() {
        assertThat(parse("`{name: $n}"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("backquote"),
                    JaloArray.of(
                        new JaloString("map"),
                        JaloArray.of(
                            new JaloString("name"),
                            JaloArray.of(new JaloString("dollar"), new JaloString("n"))))));
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
            .isEqualTo(JaloArray.of(new JaloString("quote"), new JaloString("a")));
    }

    @Test
    void c21_quoteShorthandNumber() {
        assertThat(parse("'42"))
            .isEqualTo(JaloArray.of(new JaloString("quote"), new JaloInt(42)));
    }

    @Test
    void c22_quoteShorthandList() {
        assertThat(parse("'(a b c)"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("quote"),
                    JaloArray.of(new JaloString("a"), new JaloString("b"), new JaloString("c"))));
    }

    @Test
    void c23_quoteShorthandArray() {
        assertThat(parse("'[1 2 3]"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("quote"),
                    JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3))));
    }

    @Test
    void c24_quoteShorthandMap() {
        assertThat(parse("'{a: 1}"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("quote"),
                    JaloMap.empty().put("a", new JaloInt(1))));
    }

    @Test
    void c25_quoteOfQuote() {
        assertThat(parse("''a"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("quote"),
                    JaloArray.of(new JaloString("quote"), new JaloString("a"))));
    }

    @Test
    void c26_quoteShorthandHashNull() {
        assertThat(parse("'#null"))
            .isEqualTo(
                JaloArray.of(
                    new JaloString("quote"),
                    JaloNull.INSTANCE));
    }
}
