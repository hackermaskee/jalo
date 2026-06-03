package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.junit.jupiter.api.Test;

class HofBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) { return parser.parseStandard(lexer.tokenize(input)); }

    private org.bsdclub.furuta.jalo.value.JaloValue eval(String input) { return evaluator.eval(parse(input)); }

    @Test
    void mapFilterReduce() {
        assertThat(evaluator.eval(parse("(map (fn [x] (+ x 1i)) (quasiquote (array 1i 2i 3i)))")).toString())
            .isEqualTo(JaloArray.of(new JaloInt(2), new JaloInt(3), new JaloInt(4)).toString());
        assertThat(evaluator.eval(parse("(filter (fn [x] (> x 1i)) (quasiquote (array 1i 2i 3i)))")))
            .hasToString(JaloArray.of(new JaloInt(2), new JaloInt(3)).toString());
        assertThat(evaluator.eval(parse("(reduce (fn [a b] (+ a b)) 0i (quasiquote (array 1i 2i 3i)))")))
            .hasToString(new JaloInt(6).toString());
    }

    @Test
    void andOrNot() {
        assertThat(evaluator.eval(parse("(and)"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(or #false #null 5i)"))).isEqualTo(new JaloInt(5));
        assertThat(evaluator.eval(parse("(not #false)"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void mapcatKeepTakeDropWhile() {
        assertThat(evaluator.eval(parse("(keep (fn [x] (if (> x 2i) x #null)) (quasiquote (array 1i 2i 3i)))")))
            .hasToString(JaloArray.of(new JaloInt(3)).toString());
        assertThat(evaluator.eval(parse("(take-while (fn [x] (< x 3i)) (quasiquote (array 1i 2i 3i 4i)))")))
            .hasToString(JaloArray.of(new JaloInt(1), new JaloInt(2)).toString());
        assertThat(evaluator.eval(parse("(drop-while (fn [x] (< x 3i)) (quasiquote (array 1i 2i 3i 4i)))")))
            .hasToString(JaloArray.of(new JaloInt(3), new JaloInt(4)).toString());
    }

    @Test
    void reduceRightAndFunctionCombinators() {
        assertThat(evaluator.eval(parse("(reduce-right (fn [x acc] (conj acc x)) (quasiquote (array)) (quasiquote (array 1i 2i 3i)))")))
            .hasToString(JaloArray.of(new JaloInt(3), new JaloInt(2), new JaloInt(1)).toString());
        assertThat(evaluator.eval(parse("((comp (fn [x] (+ x 1i)) (fn [x] (* x 2i))) 3i)"))).isEqualTo(new JaloInt(7));
        assertThat(evaluator.eval(parse("((partial (fn [x y] (+ x y)) 3i) 7i)"))).isEqualTo(new JaloInt(10));
        assertThat(evaluator.eval(parse("((constantly 42i) (quasiquote \"ignored\"))"))).isEqualTo(new JaloInt(42));
        assertThat(evaluator.eval(parse("((complement (fn [x] (> x 0i))) -1i)"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void mapKeysMapValsAndNotEmpty() {
        assertThat(evaluator.eval(parse("(map-keys (fn [k] (str-upper k)) (quasiquote (map (\"a\" 1i) (\"b\" 2i))))")))
            .isEqualTo(evaluator.eval(parse("(quasiquote (map (\"A\" 1i) (\"B\" 2i)))")));
        assertThat(evaluator.eval(parse("(map-vals (fn [v] (+ v 1i)) (quasiquote (map (\"a\" 1i) (\"b\" 2i))))")))
            .isEqualTo(evaluator.eval(parse("(quasiquote (map (\"a\" 2i) (\"b\" 3i)))")));
        assertThat(evaluator.eval(parse("(not-empty (quasiquote (array 1i)))")))
            .hasToString(JaloArray.of(new JaloInt(1)).toString());
        assertThat(evaluator.eval(parse("(not-empty (quasiquote (array)))"))).isEqualTo(JaloNull.INSTANCE);
        assertThat(evaluator.eval(parse("(not-empty (quasiquote (map (\"a\" 1i))))"))).isNotEqualTo(JaloNull.INSTANCE);
    }

    @Test
    void predicates() {
        assertThat(evaluator.eval(parse("(not-any? (fn [x] (> x 10i)) (quasiquote (array 1i 2i 3i)))"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(every? (fn [x] (< x 10i)) (quasiquote (array 1i 2i 3i)))"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(some (fn [x] (if (> x 2i) x #false)) (quasiquote (array 1i 2i 3i)))")))
            .hasToString(new JaloInt(3).toString());
    }

    @Test
    void trampolineReturnsImmediateValue() {
        assertThat(eval("(trampoline (fn [] 42i))")).isEqualTo(new JaloInt(42));
    }

    @Test
    void trampolineSelfTailRecursionWithoutStackOverflow() {
        eval("(def count-down (fn [n] (if (= n 0i) 0i (fn [] (count-down (- n 1i))))))");

        assertThat(eval("(trampoline count-down 1000000i)")).isEqualTo(new JaloInt(0));
    }

    @Test
    void trampolineMutualTailRecursionWithoutStackOverflow() {
        eval("(def is-even (fn [n] (if (= n 0i) #true (fn [] (is-odd (- n 1i))))))");
        eval("(def is-odd (fn [n] (if (= n 0i) #false (fn [] (is-even (- n 1i))))))");

        assertThat(eval("(trampoline is-even 1000000i)")).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void trampolinePassesInitialArgumentsOnly() {
        assertThat(eval("(trampoline (fn [x] x) 42i)")).isEqualTo(new JaloInt(42));
    }
}
