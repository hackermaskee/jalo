package org.bsdclub.furuta.jalo.typecheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class PatternTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private void check(TypeChecker checker, String input) {
        checker.check(parser.parseStandard(lexer.tokenize(input)));
    }

    @Test void p1_dollarVariableOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (array (dollar x)))"))
                .doesNotThrowAnyException();
    }

    @Test void p2_dollarNonVariableFails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(backquote (array (dollar (+ 1 2))))"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Pattern: dollar followed by non-variable");
    }

    @Test void p3_atSingleInArrayOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (array (at rest)))"))
                .doesNotThrowAnyException();
    }

    @Test void p4_multipleAtInArrayFails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(backquote (array (at r1) (at r2)))"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Pattern: multiple 'at' in array");
    }

    @Test void p5_mapStaticKeyOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (map (\"k\" v)))"))
                .doesNotThrowAnyException();
    }

    @Test void p6_mapDollarKeyFails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(backquote (map ((dollar k) v)))"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Pattern: dollar key not allowed in map");
    }

    @Test void p7_percentSingleInMapOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (map (percent rest)))"))
                .doesNotThrowAnyException();
    }

    @Test void p8_multiplePercentInMapFails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(backquote (map (percent r1) (percent r2)))"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Pattern: multiple 'percent' in map");
    }

    @Test void p9_matchWithBackquotePatternOk() {
        assertThatCode(() -> check(new TypeChecker(), "(let [val 1 result 2] (match val (backquote (array (dollar x))) result))"))
                .doesNotThrowAnyException();
    }

    @Test void p10_atWildcardOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (array (at _)))"))
                .doesNotThrowAnyException();
    }

    @Test void p11_dollarWildcardOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (array (dollar _)))"))
                .doesNotThrowAnyException();
    }

    @Test void p12_nestedPatternOk() {
        assertThatCode(() -> check(new TypeChecker(), "(backquote (array (array (dollar x))))"))
                .doesNotThrowAnyException();
    }
}
