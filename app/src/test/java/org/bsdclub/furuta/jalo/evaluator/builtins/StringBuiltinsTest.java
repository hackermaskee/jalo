package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.junit.jupiter.api.Test;

class StringBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) {
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
        assertThat(evaluator.eval(parse("(str-get (quote \"abc\") 1i)"))).isEqualTo(new JaloString("b"));
    }

    @Test
    void bA4_strGetOutOfRangeError() {
        assertThat(evaluator.eval(parse("(handle (str-get (quote \"abc\") 5i) [(quote error) e e])")))
            .isEqualTo(new JaloString("index out of range"));
    }

    @Test
    void bA5_subs() {
        assertThat(evaluator.eval(parse("(subs (quote \"hello\") 1i 3i)"))).isEqualTo(new JaloString("el"));
    }

    @Test
    void bA6_strUpper() {
        assertThat(evaluator.eval(parse("(str-upper (quote \"hello\"))"))).isEqualTo(new JaloString("HELLO"));
    }

    @Test
    void bA7_strLower() {
        assertThat(evaluator.eval(parse("(str-lower (quote \"HELLO\"))"))).isEqualTo(new JaloString("hello"));
    }

    @Test
    void bA8_strTrim() {
        assertThat(evaluator.eval(parse("(str-trim (quote \"  hi  \"))"))).isEqualTo(new JaloString("hi"));
    }

    @Test
    void bA9_startsWith() {
        assertThat(evaluator.eval(parse("(str-starts-with? (quote \"hello\") (quote \"he\"))"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void bA10_endsWith() {
        assertThat(evaluator.eval(parse("(str-ends-with? (quote \"hello\") (quote \"lo\"))"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void bA11_contains() {
        assertThat(evaluator.eval(parse("(str-contains? (quote \"hello\") (quote \"ell\"))"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void bA12_split() {
        assertThat(evaluator.eval(parse("(str-split (quote \"a,b,c\") (quote \",\"))")))
            .isEqualTo(JaloArray.of(new JaloString("a"), new JaloString("b"), new JaloString("c")));
    }

    @Test
    void bA13_join() {
        assertThat(evaluator.eval(parse("(str-join (backquote (array \"a\" \"b\" \"c\")) (quote \",\"))")))
            .isEqualTo(new JaloString("a,b,c"));
    }

    @Test
    void bA14_replace() {
        assertThat(evaluator.eval(parse("(str-replace (quote \"hello\") (quote \"l\") (quote \"r\"))")))
            .isEqualTo(new JaloString("herro"));
    }

    @Test
    void bA15_indexOf() {
        assertThat(evaluator.eval(parse("(str-index-of (quote \"hello\") (quote \"ll\"))"))).isEqualTo(new JaloInt(2));
    }

    @Test
    void bA21_strCountTypeIsInt() {
        assertThat(evaluator.eval(parse("(type (str-count (quote \"hello\")))"))).isEqualTo(new JaloString("int"));
    }

    @Test
    void bA22_strIndexOfTypeIsInt() {
        assertThat(evaluator.eval(parse("(type (str-index-of (quote \"hello\") (quote \"ll\")))")))
            .isEqualTo(new JaloString("int"));
    }

    @Test
    void bA23_strCountPromotesToDoubleInMixedArithmetic() {
        assertThat(evaluator.eval(parse("(+ (str-count (quote \"hi\")) 0.5)"))).isEqualTo(new JaloNumber(2.5));
    }

    @Test
    void bA16_strToNumberInt() {
        assertThat(evaluator.eval(parse("(str->number (quote \"42\"))"))).isEqualTo(new JaloNumber(42.0));
    }

    @Test
    void bA17_strToNumberDouble() {
        assertThat(evaluator.eval(parse("(str->number (quote \"3.14\"))"))).isEqualTo(new JaloNumber(3.14));
    }

    @Test
    void bA18_strToNumberBadError() {
        assertThat(evaluator.eval(parse("(handle (str->number (quote \"bad\")) [(quote error) e e])")))
            .isEqualTo(new JaloString("invalid number"));
    }

    @Test
    void bA19_numberToStr() {
        assertThat(evaluator.eval(parse("(number->str 42i)"))).isEqualTo(new JaloString("42"));
    }

    @Test
    void bA20_strEmpty() {
        assertThat(evaluator.eval(parse("(str-empty? (quote \"\"))"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(str-empty? (quote \"a\"))"))).isEqualTo(JaloBool.FALSE);
    }
}
