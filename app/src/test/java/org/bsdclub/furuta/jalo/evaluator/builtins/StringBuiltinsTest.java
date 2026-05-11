package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.junit.jupiter.api.Test;

class StringBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.json.JsonValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void bA1_strCount() {
        assertThat(evaluator.eval(parse("(str-count (quote \"hello\"))"))).isEqualTo(new JaloInt(5));
    }

    @Test
    void bA2_strCountEmpty() {
        assertThat(evaluator.eval(parse("(str-count (quote \"\"))"))).isEqualTo(new JaloInt(0));
    }

    @Test
    void bA3_strGet() {
        assertThat(evaluator.eval(parse("(str-get (quote \"abc\") 1i)"))).isEqualTo(new JsonString("b"));
    }

    @Test
    void bA4_strGetOutOfRangeError() {
        assertThat(evaluator.eval(parse("(handle (str-get (quote \"abc\") 5i) [(quote error) e e])")))
            .isEqualTo(new JsonString("index out of range"));
    }

    @Test
    void bA5_subs() {
        assertThat(evaluator.eval(parse("(subs (quote \"hello\") 1i 3i)"))).isEqualTo(new JsonString("el"));
    }

    @Test
    void bA6_strUpper() {
        assertThat(evaluator.eval(parse("(str-upper (quote \"hello\"))"))).isEqualTo(new JsonString("HELLO"));
    }

    @Test
    void bA7_strLower() {
        assertThat(evaluator.eval(parse("(str-lower (quote \"HELLO\"))"))).isEqualTo(new JsonString("hello"));
    }

    @Test
    void bA8_strTrim() {
        assertThat(evaluator.eval(parse("(str-trim (quote \"  hi  \"))"))).isEqualTo(new JsonString("hi"));
    }

    @Test
    void bA9_startsWith() {
        assertThat(evaluator.eval(parse("(str-starts-with? (quote \"hello\") (quote \"he\"))"))).isEqualTo(JsonBool.TRUE);
    }

    @Test
    void bA10_endsWith() {
        assertThat(evaluator.eval(parse("(str-ends-with? (quote \"hello\") (quote \"lo\"))"))).isEqualTo(JsonBool.TRUE);
    }

    @Test
    void bA11_contains() {
        assertThat(evaluator.eval(parse("(str-contains? (quote \"hello\") (quote \"ell\"))"))).isEqualTo(JsonBool.TRUE);
    }

    @Test
    void bA12_split() {
        assertThat(evaluator.eval(parse("(str-split (quote \"a,b,c\") (quote \",\"))")))
            .isEqualTo(JsonArray.of(new JsonString("a"), new JsonString("b"), new JsonString("c")));
    }

    @Test
    void bA13_join() {
        assertThat(evaluator.eval(parse("(str-join (backquote (array \"a\" \"b\" \"c\")) (quote \",\"))")))
            .isEqualTo(new JsonString("a,b,c"));
    }

    @Test
    void bA14_replace() {
        assertThat(evaluator.eval(parse("(str-replace (quote \"hello\") (quote \"l\") (quote \"r\"))")))
            .isEqualTo(new JsonString("herro"));
    }

    @Test
    void bA15_indexOf() {
        assertThat(evaluator.eval(parse("(str-index-of (quote \"hello\") (quote \"ll\"))"))).isEqualTo(new JaloInt(2));
    }

    @Test
    void bA21_strCountTypeIsInt() {
        assertThat(evaluator.eval(parse("(type (str-count (quote \"hello\")))"))).isEqualTo(new JsonString("int"));
    }

    @Test
    void bA22_strIndexOfTypeIsInt() {
        assertThat(evaluator.eval(parse("(type (str-index-of (quote \"hello\") (quote \"ll\")))")))
            .isEqualTo(new JsonString("int"));
    }

    @Test
    void bA23_strCountPromotesToDoubleInMixedArithmetic() {
        assertThat(evaluator.eval(parse("(+ (str-count (quote \"hi\")) 0.5)"))).isEqualTo(new JsonNumber(2.5));
    }

    @Test
    void bA16_strToNumberInt() {
        assertThat(evaluator.eval(parse("(str->number (quote \"42\"))"))).isEqualTo(new JsonNumber(42.0));
    }

    @Test
    void bA17_strToNumberDouble() {
        assertThat(evaluator.eval(parse("(str->number (quote \"3.14\"))"))).isEqualTo(new JsonNumber(3.14));
    }

    @Test
    void bA18_strToNumberBadError() {
        assertThat(evaluator.eval(parse("(handle (str->number (quote \"bad\")) [(quote error) e e])")))
            .isEqualTo(new JsonString("invalid number"));
    }

    @Test
    void bA19_numberToStr() {
        assertThat(evaluator.eval(parse("(number->str 42i)"))).isEqualTo(new JsonString("42"));
    }

    @Test
    void bA20_strEmpty() {
        assertThat(evaluator.eval(parse("(str-empty? (quote \"\"))"))).isEqualTo(JsonBool.TRUE);
        assertThat(evaluator.eval(parse("(str-empty? (quote \"a\"))"))).isEqualTo(JsonBool.FALSE);
    }
}
