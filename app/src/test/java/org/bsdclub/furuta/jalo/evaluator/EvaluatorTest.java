package org.bsdclub.furuta.jalo.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class EvaluatorTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.json.JsonValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test void eA1_evalNullLiteral() { assertThat(evaluator.eval(parse("#null"))).isEqualTo(JsonNull.INSTANCE); }
    @Test void eA2_evalTrueLiteral() { assertThat(evaluator.eval(parse("#true"))).isEqualTo(JsonBool.TRUE); }
    @Test void eA3_evalNumberLiteral() { assertThat(evaluator.eval(parse("42"))).isEqualTo(new JsonNumber(42.0)); }

    @Test
    void eA4_unboundVariableThrowsEffectSignal() {
        assertThatThrownBy(() -> evaluator.eval(parse("foo")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> {
                JaloEffectSignal sig = (JaloEffectSignal) ex;
                assertThat(sig.tag()).isEqualTo("error");
                assertThat(sig.value()).isEqualTo("Unbound variable: foo");
            });
    }

    @Test
    void eA5_defPersistsInGlobalEnvironment() {
        assertThat(evaluator.eval(parse("(def x 42)"))).isEqualTo(new JsonNumber(42.0));
        assertThat(evaluator.eval(parse("x"))).isEqualTo(new JsonNumber(42.0));
    }

    @Test void eA6_letParallelBinding() { assertThat(evaluator.eval(parse("(let [x 1] x)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA7_letStarSequentialBinding() { assertThat(evaluator.eval(parse("(let* [x 1 y x] y)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA8_letrecMutualRecursionBase() { assertThat(evaluator.eval(parse("(letrec [f (fn [n] (if (= n 0) 0 (f (- n 1))))] (f 3))"))).isEqualTo(new JsonNumber(0.0)); }
    @Test void eA9_ifTrueBranch() { assertThat(evaluator.eval(parse("(if #true 1 2)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA10_ifFalseBranch() { assertThat(evaluator.eval(parse("(if #false 1 2)"))).isEqualTo(new JsonNumber(2.0)); }
    @Test void eA11_functionApplicationWithLet() { assertThat(evaluator.eval(parse("(let [f (fn [x] (* x 2))] (f 3))"))).isEqualTo(new JsonNumber(6.0)); }
    @Test void eA12_closureCapturesOuterBinding() { assertThat(evaluator.eval(parse("(let [x 10] ((fn [y] (+ x y)) 5))"))).isEqualTo(new JsonNumber(15.0)); }
}
