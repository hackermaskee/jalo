package org.bsdclub.furuta.jalo.repl;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.junit.jupiter.api.Test;

class PipelineTest {
    private final Pipeline pipeline = new Pipeline();

    @Test
    void p1_additionSuccess() {
        assertSuccessValue("(+ 1 2)", new JsonNumber(3.0));
    }

    @Test
    void p2_defAccumulation() {
        assertSuccessValue("(def x 10)", JsonNull.INSTANCE);
        assertSuccessValue("(* x 2)", new JsonNumber(20.0));
    }

    @Test
    void p3_fnDefinitionAndApply() {
        assertSuccessValue("(def f (fn [a] (* a a)))", JsonNull.INSTANCE);
        assertSuccessValue("(f 7)", new JsonNumber(49.0));
    }

    @Test
    void p4_localBinding() {
        assertSuccessValue("(let [a 5 b 3] (- a b))", new JsonNumber(2.0));
    }

    @Test
    void p5_effectHandling() {
        assertSuccessValue("(let [v 0] (handle (raise (quote err) 42) [(quote err) v v]))", new JsonNumber(42.0));
    }

    @Test
    void p6_numericPromotion() {
        assertSuccessValue("(+ 1.0 2)", new JsonNumber(3.0));
    }

    @Test
    void p7_stringEval() {
        assertSuccessValue("(quote \"abc\")", new JsonString("abc"));
    }

    @Test
    void p8_arrayEval() {
        assertSuccessValue("(quote [1 2 3])", JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0)));
    }

    @Test
    void p9_lexError() {
        assertFailureKind("'abc'", EvalResult.ErrorKind.LEX);
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
        assertSuccessValue("(def x 10)", JsonNull.INSTANCE);
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
