package org.bsdclub.furuta.jalo.syntaxcheck;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class NameResolutionTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();

    private void check(SyntaxChecker checker, String input) {
        checker.check(parser.parseStandard(lexer.tokenize(input)));
    }

    @Test void n1_defRegistersBinding() {
        assertThatCode(() -> check(new SyntaxChecker(), "(def x 42)"))
                .doesNotThrowAnyException();
    }

    @Test void n2_referenceAfterDefResolves() {
        SyntaxChecker checker = new SyntaxChecker();
        assertThatCode(() -> {
            check(checker, "(def x 42)");
            check(checker, "x");
        }).doesNotThrowAnyException();
    }

    @Test void n3_unboundVariableThrows() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "foo"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Unbound variable: foo")
                .hasMessageContaining("\"foo\"");
    }

    @Test void n4_declareOnlyIsAllowed() {
        assertThatCode(() -> check(new SyntaxChecker(), "(declare foo)"))
                .doesNotThrowAnyException();
    }

    @Test void n5_referenceAfterDeclareResolves() {
        SyntaxChecker checker = new SyntaxChecker();
        assertThatCode(() -> {
            check(checker, "(declare foo)");
            check(checker, "foo");
        }).doesNotThrowAnyException();
    }

    @Test void n6_fnParamResolvesInsideBody() {
        assertThatCode(() -> check(new SyntaxChecker(), "(fn [x] x)"))
                .doesNotThrowAnyException();
    }

    @Test void n7_unboundInsideFnThrows() {
        assertThatThrownBy(() -> check(new SyntaxChecker(), "(fn [x] y)"))
                .isInstanceOf(SyntaxCheckException.class)
                .hasMessageContaining("Unbound variable: y")
                .hasMessageContaining("\"y\"");
    }

    @Test void n8_letBindingResolvesInBody() {
        assertThatCode(() -> check(new SyntaxChecker(), "(let [x 1] x)"))
                .doesNotThrowAnyException();
    }

    @Test void n9_letStarIsSequential() {
        assertThatCode(() -> check(new SyntaxChecker(), "(let* [x 1 y x] y)"))
                .doesNotThrowAnyException();
    }

    @Test void n10_letRecMutualRecursionResolves() {
        assertThatCode(() -> check(new SyntaxChecker(),
                "(letrec [even? (fn [n] (if (= n 0) #true (odd? (- n 1)))) odd? (fn [n] (if (= n 0) #false (even? (- n 1))))] (even? 4))"))
                .doesNotThrowAnyException();
    }

    @Test void n11_innerLetShadowsOuterDef() {
        SyntaxChecker checker = new SyntaxChecker();
        assertThatCode(() -> {
            check(checker, "(def x 1)");
            check(checker, "(let [x 2] x)");
        }).doesNotThrowAnyException();
    }

    @Test void n12_nestedScopeResolvesOuterAndInner() {
        assertThatCode(() -> check(new SyntaxChecker(), "(let [x 1] (let [y 2] [x y]))"))
                .doesNotThrowAnyException();
    }
}
