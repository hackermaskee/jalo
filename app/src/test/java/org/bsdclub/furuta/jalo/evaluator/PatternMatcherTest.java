package org.bsdclub.furuta.jalo.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.junit.jupiter.api.Test;

class PatternMatcherTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void mB1_literalExactMatch() {
        assertThat(evaluator.eval(parse("(match (quote \"x\") \"x\" (quote \"matched\") \"y\" (quote \"other\"))")))
            .isEqualTo(new JaloString("matched"));
    }

    @Test
    void mB2_literalNoMatchReturnsNull() {
        assertThat(evaluator.eval(parse("(match 0i 42i (quote \"matched\"))"))).isEqualTo(JaloNull.INSTANCE);
    }

    @Test
    void mB3_dollarVariableBind() {
        assertThat(evaluator.eval(parse("(match 5i (pattern (var x)) x)"))).isEqualTo(new JaloInt(5));
    }

    @Test
    void mB4_arrayExactLengthMultiBinding() {
        assertThat(evaluator.eval(parse("(match (quasiquote (array 1i 2i)) #[$a $b] (+ a b))")))
            .isEqualTo(new JaloInt(3));
    }

    @Test
    void mB5_arrayLengthMismatchFallsBack() {
        String input =
            "(match (quasiquote (array 1i 2i 3i)) #[$a $b] "
                + "(quote \"matched\") (pattern (var x)) (quote \"fallback\"))";
        assertThat(evaluator.eval(parse(input)))
            .isEqualTo(new JaloString("fallback"));
    }

    @Test
    void mB6_atSpliceRestArrayBinding() {
        assertThat(evaluator.eval(parse("(match (quasiquote (array 1i 2i 3i)) #[$head @tail] (quasiquote (array (var head) (var tail))))")))
            .hasToString(JaloArray.of(new JaloNumber(1.0), JaloArray.of(new JaloNumber(2.0), new JaloNumber(3.0))).toString());
    }

    @Test
    void mB7_mapKeyValueMatch() {
        assertThat(evaluator.eval(parse("(let [m (quasiquote (map (\"name\" \"Alice\")))] (match m #{name: $n} n))")))
            .isEqualTo(new JaloString("Alice"));
    }

    @Test
    void mB8_percentRestInMap() {
        assertThat(evaluator.eval(parse("(let [m (quasiquote (map (\"a\" 1i) (\"b\" 2i)))] (match m (pattern (map (\"a\" (var v)) (rest-map rest))) rest))")))
            .isEqualTo(JaloMap.empty().put("b", new JaloInt(2)));
    }

    @Test
    void mB9_wildcardUnderscore() {
        assertThat(evaluator.eval(parse("(match 42i (pattern (var _)) (quote \"any\"))"))).isEqualTo(new JaloString("any"));
    }

    @Test
    void mB10_allPatternsFailReturnsNull() {
        assertThat(evaluator.eval(parse("(match (quote \"hello\") 42i (quote \"num\") (pattern (array (var x))) (quote \"arr\"))")))
            .isEqualTo(JaloNull.INSTANCE);
    }
}
