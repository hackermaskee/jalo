package org.bsdclub.furuta.jalo.typecheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class SpecialFormTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private void check(TypeChecker checker, String input) {
        checker.check(parser.parseStandard(lexer.tokenize(input)));
    }

    @Test void f1_quoteArity1Ok() {
        assertThatCode(() -> check(new TypeChecker(), "(quote x)"))
                .doesNotThrowAnyException();
    }

    @Test void f2_quoteArity0Fails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(quote)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Wrong arity for quote");
    }

    @Test void f3_quoteArity2Fails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(quote x y)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Wrong arity for quote");
    }

    @Test void f4_ifArity3Ok() {
        assertThatCode(() -> check(new TypeChecker(), "(if #true 1 2)"))
                .doesNotThrowAnyException();
    }

    @Test void f5_ifArity2Fails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(if #true 1)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Wrong arity for if");
    }

    @Test void f6_defArity2Ok() {
        assertThatCode(() -> check(new TypeChecker(), "(def x 42)"))
                .doesNotThrowAnyException();
    }

    @Test void f7_defArity1Fails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(def x)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Wrong arity for def");
    }

    @Test void f8_fnParamsAndBodyOk() {
        assertThatCode(() -> check(new TypeChecker(), "(fn [x y] (+ x y))"))
                .doesNotThrowAnyException();
    }

    @Test void f9_fnZeroParamOk() {
        assertThatCode(() -> check(new TypeChecker(), "(fn [] 1)"))
                .doesNotThrowAnyException();
    }

    @Test void f10_fnParamListMustBeArray() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(fn x x)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Wrong arity for fn");
    }

    @Test void f11_fnRestOk() {
        assertThatCode(() -> check(new TypeChecker(), "(fn [x \"&\" rest] rest)"))
                .doesNotThrowAnyException();
    }

    @Test void f12_fnRestExtraFails() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(fn [x \"&\" rest extra] rest)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("rest marker '&'");
    }

    @Test void f13_letEvenBindingsOk() {
        assertThatCode(() -> check(new TypeChecker(), "(let [x 1 y 2] y)"))
                .doesNotThrowAnyException();
    }

    @Test void f14_letOddBindingsFail() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(let [x] x)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("let bindings must be even-length");
    }

    @Test void f15_declareMultiArgsOk() {
        assertThatCode(() -> check(new TypeChecker(), "(declare a b c)"))
                .doesNotThrowAnyException();
    }
}
