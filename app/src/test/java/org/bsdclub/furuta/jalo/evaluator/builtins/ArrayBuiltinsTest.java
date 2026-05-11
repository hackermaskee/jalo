package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonBool;
import org.bsdclub.furuta.jalo.json.JsonNull;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.junit.jupiter.api.Test;

class ArrayBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.json.JsonValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void bB1_count() {
        assertThat(evaluator.eval(parse("(count (backquote (array 1i 2i 3i)))"))).isEqualTo(new JaloInt(3));
    }

    @Test
    void bB2_countEmpty() {
        assertThat(evaluator.eval(parse("(count (backquote (array)))"))).isEqualTo(new JaloInt(0));
    }

    @Test
    void bB3_first() {
        assertThat(evaluator.eval(parse("(first (backquote (array 10i 20i)))"))).isEqualTo(new JsonNumber(10));
    }

    @Test
    void bB4_firstEmpty() {
        assertThat(evaluator.eval(parse("(first (backquote (array)))"))).isEqualTo(JsonNull.INSTANCE);
    }

    @Test
    void bB5_last() {
        assertThat(evaluator.eval(parse("(last (backquote (array 10i 20i 30i)))"))).isEqualTo(new JsonNumber(30));
    }

    @Test
    void bB6_nth() {
        assertThat(evaluator.eval(parse("(nth (backquote (array 10i 20i 30i)) 1i)"))).isEqualTo(new JsonNumber(20));
    }

    @Test
    void bB7_nthOutOfRangeError() {
        assertThat(evaluator.eval(parse("(handle (nth (backquote (array 10i 20i)) 5i) [(quote error) e e])")))
            .isEqualTo(new org.bsdclub.furuta.jalo.json.JsonString("nth: index out of range: 5"));
    }

    @Test
    void bB8_rest() {
        assertThat(evaluator.eval(parse("(rest (backquote (array 1i 2i 3i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(2), new JsonNumber(3)));
    }

    @Test
    void bB9_conj() {
        assertThat(evaluator.eval(parse("(conj (backquote (array 1i 2i)) 3i)")))
            .isEqualTo(JsonArray.of(new JsonNumber(1), new JsonNumber(2), new JsonNumber(3)));
    }

    @Test
    void bB10_concat() {
        assertThat(evaluator.eval(parse("(concat (backquote (array 1i 2i)) (backquote (array 3i 4i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(1), new JsonNumber(2), new JsonNumber(3), new JsonNumber(4)));
    }

    @Test
    void bB11_reverse() {
        assertThat(evaluator.eval(parse("(reverse (backquote (array 1i 2i 3i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(3), new JsonNumber(2), new JsonNumber(1)));
    }

    @Test
    void bB12_range() {
        assertThat(evaluator.eval(parse("(range 3i)")))
            .isEqualTo(JsonArray.of(new JsonNumber(0), new JsonNumber(1), new JsonNumber(2)));
    }

    @Test
    void bB13_indexOf() {
        assertThat(evaluator.eval(parse("(index-of (backquote (array 10i 20i 30i)) 20i)"))).isEqualTo(new JaloInt(1));
    }

    @Test
    void bB14_contains() {
        assertThat(evaluator.eval(parse("(contains? (backquote (array 1i 2i 3i)) 2i)"))).isEqualTo(JsonBool.TRUE);
    }

    @Test
    void bB15_sort() {
        assertThat(evaluator.eval(parse("(sort (backquote (array 3i 1i 2i)))")))
            .isEqualTo(JsonArray.of(new JsonNumber(1), new JsonNumber(2), new JsonNumber(3)));
    }
}
