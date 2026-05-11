package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.junit.jupiter.api.Test;

class JqBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private JaloValue parse(String source) {
        return parser.parseStandard(lexer.tokenize(source));
    }

    @Test
    void acceptsAtCsv() {
        JaloValue result = evaluator.eval(parse("(at-csv (quasiquote (array \"a\" \"b\" \"c\")))"));
        assertThat(result).isEqualTo(new JaloString("a,b,c"));
    }

    @Test
    void acceptsAtBase64() {
        JaloValue result = evaluator.eval(parse("(at-base64 (quote \"hello\"))"));
        assertThat(result).isEqualTo(new JaloString("aGVsbG8="));
    }

    @Test
    void acceptsGroupBy() {
        evaluator.eval(parse("(def even? (fn [n] (= (* (/ n 2i) 2i) n)))"));
        JaloValue result = evaluator.eval(parse("(group-by even? (quasiquote (array 1i 2i 3i 4i)))"));
        assertThat(result).isEqualTo(
            JaloMap.empty()
                .put("#false", JaloArray.of(new JaloInt(1), new JaloInt(3)))
                .put("#true", JaloArray.of(new JaloInt(2), new JaloInt(4))));
    }

    @Test
    void acceptsUniqueBy() {
        JaloValue result = evaluator.eval(parse("(unique-by (fn [x] x) (quasiquote (array 1i 2i 1i 3i 2i)))"));
        assertThat(result).isEqualTo(JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3)));
    }

    @Test
    void acceptsRecurse() {
        JaloValue result = evaluator.eval(parse("(recurse {a: 1 b: {c: 2}})"));
        assertThat(result).isEqualTo(
            JaloArray.of(
                JaloMap.empty().put("a", new JaloInt(1)).put("b", JaloMap.empty().put("c", new JaloInt(2))),
                new JaloInt(1),
                JaloMap.empty().put("c", new JaloInt(2)),
                new JaloInt(2)));
    }
}
