package org.bsdclub.furuta.jalo.repl;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.junit.jupiter.api.Test;

class PipelineTest {
    private final Pipeline pipeline = new Pipeline();

    @Test
    void p1_additionSuccess() {
        assertSuccessValue("(+ 1 2)", new JaloNumber(3.0));
    }

    @Test
    void p2_defAccumulation() {
        assertSuccessValue("(def x 10)", JaloNull.INSTANCE);
        assertSuccessValue("(* x 2)", new JaloNumber(20.0));
    }

    @Test
    void p3_fnDefinitionAndApply() {
        assertSuccessValue("(def f (fn [a] (* a a)))", JaloNull.INSTANCE);
        assertSuccessValue("(f 7)", new JaloNumber(49.0));
    }

    @Test
    void p4_localBinding() {
        assertSuccessValue("(let [a 5 b 3] (- a b))", new JaloNumber(2.0));
    }

    @Test
    void p5_effectHandling() {
        assertSuccessValue("(let [v 0] (handle (raise (quote err) 42) [(quote err) v v]))", new JaloNumber(42.0));
    }

    @Test
    void p6_numericPromotion() {
        assertSuccessValue("(+ 1.0 2)", new JaloNumber(3.0));
    }

    @Test
    void p7_stringEval() {
        assertSuccessValue("(quote \"abc\")", new JaloString("abc"));
    }

    @Test
    void p8_arrayEval() {
        assertSuccessValue("(quote [1 2 3])", JaloArray.of(new JaloNumber(1.0), new JaloNumber(2.0), new JaloNumber(3.0)));
    }

    @Test
    void p9_lexError() {
        assertFailureKind("#abc", EvalResult.ErrorKind.LEX);
    }

    @Test
    void p10_parseError() {
        assertFailureKind("(+", EvalResult.ErrorKind.PARSE);
    }

    @Test
    void p11_syntaxError() {
        assertFailureKind("(if 1 2 3 4)", EvalResult.ErrorKind.SYNTAX);
    }

    @Test
    void p12_effectError() {
        assertFailureKind("(error (quote oops))", EvalResult.ErrorKind.EFFECT);
    }

    @Test
    void p13_resetClearsDefs() {
        assertSuccessValue("(def x 10)", JaloNull.INSTANCE);
        pipeline.reset();
        assertFailureKind("x", EvalResult.ErrorKind.SYNTAX);
    }

    @Test
    void p14_emptyInputFails() {
        EvalResult result = pipeline.run("");
        assertThat(result).isInstanceOf(EvalResult.Failure.class);
        EvalResult.Failure failure = (EvalResult.Failure) result;
        assertThat(failure.kind()).isIn(EvalResult.ErrorKind.LEX, EvalResult.ErrorKind.PARSE);
    }

    @Test
    void p15_multiExprReturnsFailureWithCurrentParser() {
        assertFailureKind("(def a 1)(def b 2)", EvalResult.ErrorKind.PARSE);
    }

    private void assertSuccessValue(String src, JaloValue expected) {
        EvalResult result = pipeline.run(src);
        assertThat(result).isInstanceOf(EvalResult.Success.class);
        assertThat(((EvalResult.Success) result).value()).isEqualTo(expected);
    }

    private void assertFailureKind(String src, EvalResult.ErrorKind expectedKind) {
        EvalResult result = pipeline.run(src);
        assertThat(result).isInstanceOf(EvalResult.Failure.class);
        assertThat(((EvalResult.Failure) result).kind()).isEqualTo(expectedKind);
    }
}
