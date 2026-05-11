package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class MapBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void bB16_assoc() {
        assertThat(evaluator.eval(parse("(assoc (backquote (map)) (quote \"a\") 1i)")))
            .isEqualTo(JaloMap.empty().put("a", new org.bsdclub.furuta.jalo.value.JaloInt(1)));
    }

    @Test
    void bB17_dissoc() {
        assertThat(evaluator.eval(parse("(dissoc (backquote (map (\"a\" 1i) (\"b\" 2i))) (quote \"a\"))")))
            .isEqualTo(JaloMap.empty().put("b", new JaloNumber(2)));
    }

    @Test
    void bB18_keys() {
        Object value = evaluator.eval(parse("(keys (backquote (map (\"a\" 1i) (\"b\" 2i))))"));
        assertThat(value).isInstanceOf(JaloArray.class);
        JaloArray arr = (JaloArray) value;
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0)).isIn(new JaloString("a"), new JaloString("b"));
        assertThat(arr.get(1)).isIn(new JaloString("a"), new JaloString("b"));
        assertThat(arr.get(0)).isNotEqualTo(arr.get(1));
    }

    @Test
    void bB19_vals() {
        Object value = evaluator.eval(parse("(vals (backquote (map (\"a\" 1i) (\"b\" 2i))))"));
        assertThat(value).isInstanceOf(JaloArray.class);
        JaloArray arr = (JaloArray) value;
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0)).isIn(new JaloNumber(1), new JaloNumber(2));
        assertThat(arr.get(1)).isIn(new JaloNumber(1), new JaloNumber(2));
        assertThat(arr.get(0)).isNotEqualTo(arr.get(1));
    }

    @Test
    void bB20_merge() {
        assertThat(evaluator.eval(parse("(merge (backquote (map (\"a\" 1i))) (backquote (map (\"b\" 2i))))")))
            .isEqualTo(JaloMap.empty().put("a", new JaloNumber(1)).put("b", new JaloNumber(2)));
    }
}
