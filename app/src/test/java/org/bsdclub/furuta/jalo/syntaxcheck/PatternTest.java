package org.bsdclub.furuta.jalo.syntaxcheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class PatternTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private void check(SyntaxChecker checker, String input) {
        checker.check(parser.parseStandard(lexer.tokenize(input)));
    }

    @Test void p1_dollarVariableOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (array (var x)))"))
                .doesNotThrowAnyException();
    }

    @Test void p2_dollarNonVariableFails() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "(quasiquote (array (var (+ 1 2))))"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Pattern: dollar followed by non-variable");
    }

    @Test void p3_atSingleInArrayOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (array (rest-seq rest)))"))
                .doesNotThrowAnyException();
    }

    @Test void p4_multipleAtInArrayFails() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "(quasiquote (array (rest-seq r1) (rest-seq r2)))"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Pattern: multiple 'at' in array");
    }

    @Test void p5_mapStaticKeyOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (map (\"k\" v)))"))
                .doesNotThrowAnyException();
    }

    @Test void p6_mapDollarKeyFails() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "(quasiquote (map ((var k) v)))"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Pattern: dollar key not allowed in map");
    }

    @Test void p7_percentSingleInMapOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (map (rest-map rest)))"))
                .doesNotThrowAnyException();
    }

    @Test void p8_multiplePercentInMapFails() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "(quasiquote (map (rest-map r1) (rest-map r2)))"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Pattern: multiple 'percent' in map");
    }

    @Test void p9_matchWithBackquotePatternOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(let [val 1 result 2] (match val (quasiquote (array (var x))) result))"))
                .doesNotThrowAnyException();
    }

    @Test void p10_atWildcardOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (array (rest-seq _)))"))
                .doesNotThrowAnyException();
    }

    @Test void p11_dollarWildcardOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (array (var _)))"))
                .doesNotThrowAnyException();
    }

    @Test void p12_nestedPatternOk() {
        assertThatCode(() -> check(new SyntaxChecker(), "(quasiquote (array (array (var x))))"))
                .doesNotThrowAnyException();
    }
}
