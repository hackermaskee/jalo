package org.bsdclub.furuta.jalo.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloValue;
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
                assertThat(sig.tag()).isEqualTo(new JsonString("error"));
                assertThat(sig.value()).isEqualTo(new JsonString("Unbound variable: foo"));
            });
    }

    @Test
    void eA5_defPersistsInGlobalEnvironment() {
        assertThat(evaluator.eval(parse("(def x 42)"))).isEqualTo(JsonNull.INSTANCE);
        assertThat(evaluator.eval(parse("x"))).isEqualTo(new JsonNumber(42.0));
    }

    @Test void eA6_letParallelBinding() { assertThat(evaluator.eval(parse("(let [x 1] x)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA7_letStarSequentialBinding() { assertThat(evaluator.eval(parse("(let* [x 1 y x] y)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA8_letrecMutualRecursionBase() { assertThat(evaluator.eval(parse("(letrec [f (fn [n] (if (= n 0) 0 (f (- n 1))))] (f 3))"))).isEqualTo(new JsonNumber(0.0)); }
    @Test void eA9_ifTrueBranch() { assertThat(evaluator.eval(parse("(if #true 1 2)"))).isEqualTo(new JsonNumber(1.0)); }
    @Test void eA10_ifFalseBranch() { assertThat(evaluator.eval(parse("(if #false 1 2)"))).isEqualTo(new JsonNumber(2.0)); }
    @Test void eA11_functionApplicationWithLet() { assertThat(evaluator.eval(parse("(let [f (fn [x] (* x 2))] (f 3))"))).isEqualTo(new JsonNumber(6.0)); }
    @Test void eA12_closureCapturesOuterBinding() { assertThat(evaluator.eval(parse("(let [x 10] ((fn [y] (+ x y)) 5))"))).isEqualTo(new JsonNumber(15.0)); }

    @Test void eB1_handleCatchesMatchingTag() { assertThat(evaluator.eval(parse("(handle (raise (quote x) 1) [(quote x) v v])"))).isEqualTo(new JsonNumber(1.0)); }

    @Test
    void eB2_raiseWithoutHandlePropagates() {
        assertThatThrownBy(() -> evaluator.eval(parse("(raise (quote x) 1)")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> assertThat(((JaloEffectSignal) ex).tag()).isEqualTo(new JsonString("x")));
    }

    @Test
    void eB3_handleTagMismatchPropagates() {
        assertThatThrownBy(() -> evaluator.eval(parse("(handle (raise (quote y) 1) [(quote x) v v])")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> assertThat(((JaloEffectSignal) ex).tag()).isEqualTo(new JsonString("y")));
    }

    @Test
    void eB4_nestedInnerHandleWins() {
        assertThat(evaluator.eval(parse("(handle (handle (raise (quote x) 1) [(quote x) v v]) [(quote x) v 999])")))
            .isEqualTo(new JsonNumber(1.0));
    }

    @Test
    void eB5_nestedOuterHandleCatchesRethrow() {
        assertThat(evaluator.eval(parse("(handle (handle (raise (quote y) 1) [(quote x) v v]) [(quote y) v 999])")))
            .isEqualTo(new JsonNumber(999.0));
    }
    @Test void eB6_errorBuiltinEffectCaught() { assertThat(evaluator.eval(parse("(handle (error (quote msg)) [(quote error) e e])"))).isEqualTo(new JsonString("msg")); }
    @Test
    void eB7_unboundVariableRaisedAsErrorEffect() {
        assertThat(evaluator.eval(parse("(handle foo [(quote error) e e])")))
            .isEqualTo(new JsonString("Unbound variable: foo"));
    }

    @Test
    void eB8_divisionByZeroRaisedAsErrorEffect() {
        assertThat(evaluator.eval(parse("(handle (/ 1i 0i) [(quote error) e e])")))
            .isEqualTo(new JsonString("Division by zero"));
    }

    @Test
    void eB9_handleRaisingAnotherSignalPropagates() {
        assertThatThrownBy(() -> evaluator.eval(parse("(handle (raise (quote x) 1) [(quote x) v (raise (quote y) 2)])")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> assertThat(((JaloEffectSignal) ex).tag()).isEqualTo(new JsonString("y")));
    }

    @Test void eB10_handleWithLexicalValue() { assertThat(evaluator.eval(parse("(handle (let [x 1] (raise (quote x) x)) [(quote x) v v])"))).isEqualTo(new JsonNumber(1.0)); }

    @Test
    void letrecVariablesDoNotLeakToGlobal() {
        assertThat(evaluator.eval(parse("(letrec [hidden 42] hidden)"))).isEqualTo(new JsonNumber(42.0));
        assertThatThrownBy(() -> evaluator.eval(parse("hidden")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> assertThat(((JaloEffectSignal) ex).tag()).isEqualTo(new JsonString("error")));
    }

    @Test void eC1_declareIsNoOp() { assertThat(evaluator.eval(parse("(declare foo)"))).isEqualTo(JsonNull.INSTANCE); }
    @Test
    void eC2_defThenAdd() {
        evaluator.eval(parse("(def x 1)"));
        evaluator.eval(parse("(def y 2)"));
        assertThat(evaluator.eval(parse("(+ x y)"))).isEqualTo(new JsonNumber(3.0));
    }

    @Test
    void eC3_mutualRecursionWithDeclareAndDef() {
        assertThat(
            evaluator.eval(
                parse("(letrec [even? (fn [n] (if (= n 0) #true (odd? (- n 1)))) odd? (fn [n] (if (= n 0) #false (even? (- n 1))))] (even? 4))")))
            .isEqualTo(JsonBool.TRUE);
    }
    @Test void eC4_addDouble() { assertThat(evaluator.eval(parse("(+ 1 2)"))).isEqualTo(new JsonNumber(3.0)); }
    @Test void eC5_addIntLongPromotion() { assertThat(evaluator.eval(parse("(+ 1i 2l)"))).isEqualTo(new JaloLong(3L)); }
    @Test void eC6_addIntDoublePromotion() { assertThat(evaluator.eval(parse("(+ 1i 2.0)"))).isEqualTo(new JsonNumber(3.0)); }
    @Test void eC7_integerDivideByZeroRaisesError() { assertThat(evaluator.eval(parse("(handle (/ 1i 0i) [(quote error) e e])"))).isEqualTo(new JsonString("Division by zero")); }
    @Test
    void eC8_equalsAndNaN() {
        assertThat(evaluator.eval(parse("(= 1 1)"))).isEqualTo(JsonBool.TRUE);
        assertThat(evaluator.eval(parse("(= (/ 0.0 0.0) (/ 0.0 0.0))"))).isEqualTo(JsonBool.FALSE);
    }

    @Test
    void eC9_typePredicates() {
        assertThat(evaluator.eval(parse("(number? 42)"))).isEqualTo(JsonBool.TRUE);
        assertThat(evaluator.eval(parse("(string? 42)"))).isEqualTo(JsonBool.FALSE);
    }
    @Test void eC10_typeDouble() { assertThat(evaluator.eval(parse("(type 42)"))).isEqualTo(new JsonString("double")); }
    @Test void eC11_typeString() { assertThat(evaluator.eval(parse("(type (quote \"hello\"))"))).isEqualTo(new JsonString("string")); }
    @Test void eC12_integrationHandle() { assertThat(evaluator.eval(parse("(let [x 1i] (handle (/ x 0i) [(quote error) e e]))"))).isEqualTo(new JsonString("Division by zero")); }

    @Test
    void integration_hello_world_level() {
        Evaluator ev = new Evaluator();
        ev.eval(parse("(def greet (fn [name] (type name)))"));
        JaloValue result = ev.eval(parse("(greet (quote \"world\"))"));
        assertThat(result).isEqualTo(new JsonString("string"));
    }

    @Test
    void prcT1_bindMultipleNamesInOneScope() {
        Environment env = Environment.root().bind("x", new JsonNumber(1)).bind("y", new JsonNumber(2));
        assertThat(env.lookup("x")).isEqualTo(new JsonNumber(1));
        assertThat(env.lookup("y")).isEqualTo(new JsonNumber(2));
    }

    @Test
    void prcT2_innerBindShadowsOuterInSameMap() {
        Environment env = Environment.root().bind("x", new JsonNumber(1)).bind("x", new JsonNumber(2));
        assertThat(env.lookup("x")).isEqualTo(new JsonNumber(2));
    }

    @Test
    void prcT3_immutableBindReturnsNewEnvironment() {
        Environment original = Environment.root().bind("x", new JsonNumber(1));
        Environment derived = original.bind("y", new JsonNumber(2));
        assertThat(original.lookup("x")).isEqualTo(new JsonNumber(1));
        assertThat(derived.lookup("y")).isEqualTo(new JsonNumber(2));
        assertThatThrownBy(() -> original.lookup("y"))
            .isInstanceOf(JaloEffectSignal.class);
    }

    @Test
    void mA1_backquoteLiteralNumber() {
        assertThat(evaluator.eval(parse("(backquote 42i)"))).isEqualTo(new JsonNumber(42.0));
    }

    @Test
    void mA2_backquoteLiteralString() {
        assertThat(evaluator.eval(parse("(backquote \"hello\")"))).isEqualTo(new JsonString("hello"));
    }

    @Test
    void mA3_backquoteLiteralNull() {
        assertThat(evaluator.eval(parse("(backquote #null)"))).isEqualTo(JsonNull.INSTANCE);
    }

    @Test
    void mA4_backquoteDollarVariableEmbed() {
        assertThat(evaluator.eval(parse("(let [x 5i] (backquote (dollar x)))"))).isEqualTo(new JsonNumber(5.0));
    }

    @Test
    void mA5_backquoteArrayWithDollars() {
        assertThat(evaluator.eval(parse("(let [x 1i y 2i] (backquote (array (dollar x) (dollar y))))")))
            .isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0)));
    }

    @Test
    void mA6_backquoteArrayMixedLiteralAndDollar() {
        assertThat(evaluator.eval(parse("(let [x 5i] (backquote (array 1i (dollar x) 3i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(5.0), new JsonNumber(3.0)));
    }

    @Test
    void mA7_backquoteAtSpliceInArray() {
        assertThat(evaluator.eval(parse("(let [arr (array 2i 3i)] (backquote (array 1i (at arr) 4i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0), new JsonNumber(4.0)));
    }

    @Test
    void mA8_backquoteAtSpliceEmptyArray() {
        assertThat(evaluator.eval(parse("(let [arr (array)] (backquote (array 0i (at arr) 1i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(0.0), new JsonNumber(1.0)));
    }

    @Test
    void mA9_backquoteAtSpliceNonArrayFails() {
        assertThatThrownBy(() -> evaluator.eval(parse("(let [x 5i] (backquote (array (at x))))")))
            .isInstanceOf(JaloEffectSignal.class)
            .satisfies(ex -> {
                JaloEffectSignal sig = (JaloEffectSignal) ex;
                assertThat(sig.tag()).isEqualTo(new JsonString("error"));
                assertThat(sig.value()).isEqualTo(new JsonString("splice target must be array"));
            });
    }

    @Test
    void mA10_backquoteMapLiteralKeyDollarValue() {
        assertThat(evaluator.eval(parse("(let [v 1i] (backquote (map (\"k\" (dollar v)))))")))
            .isEqualTo(JsonObject.empty().put("k", new JsonNumber(1.0)));
    }

    @Test
    void mA11_backquoteMapWithPercentSplice() {
        assertThat(evaluator.eval(parse("(let [extra (map (\"a\" 1i))] (backquote (map (\"b\" 2i) (percent extra))))")))
            .isEqualTo(JsonObject.empty().put("b", new JsonNumber(2.0)).put("a", new JsonNumber(1.0)));
    }

    @Test
    void mA12_backquoteNestedArrayOfMaps() {
        assertThat(evaluator.eval(parse("(backquote (array (map (\"x\" 1i)) (map (\"x\" 2i))))")))
            .isEqualTo(
                JsonArray.of(
                    JsonObject.empty().put("x", new JsonNumber(1.0)),
                    JsonObject.empty().put("x", new JsonNumber(2.0))));
    }
}
