package org.bsdclub.furuta.jalo.typecheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class NameResolutionTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private void check(TypeChecker checker, String input) {
        checker.check(parser.parseStandard(lexer.tokenize(input)));
    }

    @Test void n1_defRegistersBinding() {
        assertThatCode(() -> check(new TypeChecker(), "(def x 42)"))
                .doesNotThrowAnyException();
    }

    @Test void n2_referenceAfterDefResolves() {
        TypeChecker checker = new TypeChecker();
        assertThatCode(() -> {
            check(checker, "(def x 42)");
            check(checker, "x");
        }).doesNotThrowAnyException();
    }

    @Test void n3_unboundVariableThrows() {
        assertThatThrownBy(() -> check(new TypeChecker(), "foo"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Unbound variable: foo")
                .hasMessageContaining("\"foo\"");
    }

    @Test void n4_declareOnlyIsAllowed() {
        assertThatCode(() -> check(new TypeChecker(), "(declare foo)"))
                .doesNotThrowAnyException();
    }

    @Test void n5_referenceAfterDeclareResolves() {
        TypeChecker checker = new TypeChecker();
        assertThatCode(() -> {
            check(checker, "(declare foo)");
            check(checker, "foo");
        }).doesNotThrowAnyException();
    }

    @Test void n6_fnParamResolvesInsideBody() {
        assertThatCode(() -> check(new TypeChecker(), "(fn [x] x)"))
                .doesNotThrowAnyException();
    }

    @Test void n7_unboundInsideFnThrows() {
        assertThatThrownBy(() -> check(new TypeChecker(), "(fn [x] y)"))
                .isInstanceOf(TypeCheckException.class)
                .hasMessageContaining("Unbound variable: y")
                .hasMessageContaining("\"y\"");
    }

    @Test void n8_letBindingResolvesInBody() {
        assertThatCode(() -> check(new TypeChecker(), "(let [x 1] x)"))
                .doesNotThrowAnyException();
    }

    @Test void n9_letStarIsSequential() {
        assertThatCode(() -> check(new TypeChecker(), "(let* [x 1 y x] y)"))
                .doesNotThrowAnyException();
    }

    @Test void n10_letRecMutualRecursionResolves() {
        assertThatCode(() -> check(new TypeChecker(),
                "(letrec [even? (fn [n] (if (= n 0) #true (odd? (- n 1)))) odd? (fn [n] (if (= n 0) #false (even? (- n 1))))] (even? 4))"))
                .doesNotThrowAnyException();
    }

    @Test void n11_innerLetShadowsOuterDef() {
        TypeChecker checker = new TypeChecker();
        assertThatCode(() -> {
            check(checker, "(def x 1)");
            check(checker, "(let [x 2] x)");
        }).doesNotThrowAnyException();
    }

    @Test void n12_nestedScopeResolvesOuterAndInner() {
        assertThatCode(() -> check(new TypeChecker(), "(let [x 1] (let [y 2] [x y]))"))
                .doesNotThrowAnyException();
    }
}
