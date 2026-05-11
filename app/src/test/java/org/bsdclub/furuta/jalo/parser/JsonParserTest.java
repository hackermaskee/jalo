package org.bsdclub.furuta.jalo.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.lexer.LexerException;
import org.junit.jupiter.api.Test;

class JsonParserTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private Object parse(String input) {
        return parser.parseJson(lexer.tokenize(input));
    }

    @Test void b1_null() { assertThat(parse("null")).isEqualTo(JaloNull.INSTANCE); }
    @Test void b2_true() { assertThat(parse("true")).isEqualTo(JaloBool.TRUE); }
    @Test void b3_false() { assertThat(parse("false")).isEqualTo(JaloBool.FALSE); }
    @Test void b4_zero() { assertThat(parse("0")).isEqualTo(new JaloNumber(0.0)); }
    @Test void b5_integer() { assertThat(parse("42")).isEqualTo(new JaloNumber(42.0)); }
    @Test void b6_negativeDecimal() { assertThat(parse("-3.14")).isEqualTo(new JaloNumber(-3.14)); }
    @Test void b7_exponent() { assertThat(parse("1e3")).isEqualTo(new JaloNumber(1000.0)); }
    @Test void b8_string() { assertThat(parse("\"hello\"")).isEqualTo(new JaloString("hello")); }
    @Test void b9_stringEscape() { assertThat(parse("\"a\\nb\"")).isEqualTo(new JaloString("a\nb")); }
    @Test void b10_emptyArray() { assertThat(parse("[]")).isEqualTo(JaloArray.empty()); }
    @Test void b11_array() { assertThat(parse("[1,2,3]")).isEqualTo(JaloArray.of(new JaloNumber(1.0), new JaloNumber(2.0), new JaloNumber(3.0))); }
    @Test void b12_nestedArray() { assertThat(parse("[1,[2,3]]")).isEqualTo(JaloArray.of(new JaloNumber(1.0), JaloArray.of(new JaloNumber(2.0), new JaloNumber(3.0)))); }
    @Test void b13_emptyObject() { assertThat(parse("{}")).isEqualTo(JaloMap.empty()); }
    @Test void b14_object() { assertThat(parse("{\"a\":1}")).isEqualTo(JaloMap.empty().put("a", new JaloNumber(1.0))); }
    @Test void b15_objectMulti() { assertThat(parse("{\"a\":1,\"b\":2}")).isEqualTo(JaloMap.empty().put("a", new JaloNumber(1.0)).put("b", new JaloNumber(2.0))); }
    @Test void b16_leadingZero_lexerError() { assertThatThrownBy(() -> lexer.tokenize("01")).isInstanceOf(LexerException.class); }
    @Test void b17_trailingComma() { assertThatThrownBy(() -> parse("[1,2,]")).isInstanceOf(ParserException.class).hasMessageContaining("Trailing comma not allowed"); }
    @Test void b18_unclosedArray() { assertThatThrownBy(() -> parse("[1,2")).isInstanceOf(ParserException.class).hasMessageContaining("Unexpected EOF: expected ']'"); }
    @Test void b19_duplicateKey() { assertThatThrownBy(() -> parse("{\"a\":1,\"a\":2}")).isInstanceOf(ParserException.class).hasMessageContaining("Duplicate key: a"); }
    @Test void b20_missingColon() { assertThatThrownBy(() -> parse("{\"a\" 1}")).isInstanceOf(ParserException.class).hasMessageContaining("Expected ':' after key"); }
}
