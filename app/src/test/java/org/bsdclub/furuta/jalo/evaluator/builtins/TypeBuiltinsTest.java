package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.junit.jupiter.api.Test;

class TypeBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private JaloValue run(String src) {
        return evaluator.eval(parser.parseStandard(lexer.tokenize(src)));
    }

    @Test void nullIsPureJson() { assertThat(run("(pure-json? null)")).isEqualTo(JaloBool.TRUE); }
    @Test void boolIsPureJson() {
        assertThat(run("(pure-json? #true)")).isEqualTo(JaloBool.TRUE);
        assertThat(run("(pure-json? #false)")).isEqualTo(JaloBool.TRUE);
    }
    @Test void doubleIsPureJson() {
        assertThat(run("(pure-json? 3.14)")).isEqualTo(JaloBool.TRUE);
        assertThat(run("(pure-json? 42.0)")).isEqualTo(JaloBool.TRUE);
    }
    @Test void stringIsPureJson() { assertThat(run("(pure-json? \"hello\")")).isEqualTo(JaloBool.TRUE); }
    @Test void intIsNotPureJson() { assertThat(run("(pure-json? 42i)")).isEqualTo(JaloBool.FALSE); }
    @Test void longIsNotPureJson() { assertThat(run("(pure-json? 42l)")).isEqualTo(JaloBool.FALSE); }
    @Test void emptyArrayIsPureJson() { assertThat(run("(pure-json? [])")).isEqualTo(JaloBool.TRUE); }
    @Test void doubleArrayIsPureJson() { assertThat(run("(pure-json? [1.0 2.0 3.0])")).isEqualTo(JaloBool.TRUE); }
    @Test void intElementArrayIsNotPureJson() { assertThat(run("(pure-json? [1i 2i])")).isEqualTo(JaloBool.FALSE); }
    @Test void emptyMapIsPureJson() { assertThat(run("(pure-json? {})")).isEqualTo(JaloBool.TRUE); }
    @Test void doubleMapIsPureJson() { assertThat(run("(pure-json? {a: 1.0})")).isEqualTo(JaloBool.TRUE); }
    @Test void intValueMapIsNotPureJson() { assertThat(run("(pure-json? {a: 42i})")).isEqualTo(JaloBool.FALSE); }
    @Test void fnIsNotPureJson() { assertThat(run("(pure-json? (fn [x] x))")).isEqualTo(JaloBool.FALSE); }
    @Test void nestedArrayWithIntIsNotPureJson() { assertThat(run("(pure-json? [1.0 [2i 3.0]])")).isEqualTo(JaloBool.FALSE); }
    @Test void nestedMapWithIntIsNotPureJson() { assertThat(run("(pure-json? {a: {b: 42i}})")).isEqualTo(JaloBool.FALSE); }
}
