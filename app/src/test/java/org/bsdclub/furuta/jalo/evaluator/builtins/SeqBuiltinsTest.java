package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.junit.jupiter.api.Test;

class SeqBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void bB21_empty() {
        assertThat(evaluator.eval(parse("(empty? (backquote (array)))"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(empty? (backquote (array 1i)))"))).isEqualTo(JaloBool.FALSE);
    }

    @Test
    void bB22_getIn() {
        assertThat(evaluator.eval(parse("(get-in (backquote (map (\"a\" (map (\"b\" 42i))))) (backquote (array \"a\" \"b\")))")))
            .isEqualTo(new JaloNumber(42));
    }

    @Test
    void bB23_assocIn() {
        assertThat(evaluator.eval(parse("(assoc-in (backquote (map (\"a\" (map (\"b\" 1i))))) (backquote (array \"a\" \"b\")) 9i)")))
            .isEqualTo(JaloMap.empty().put("a", JaloMap.empty().put("b", new JaloInt(9))));
    }

    @Test
    void bB24_updateIn() {
        assertThat(evaluator.eval(parse("(update-in (backquote (map (\"a\" (array 1i 2i)))) (backquote (array \"a\")) (fn [x] (reverse x)))")))
            .hasToString(JaloMap.empty().put("a", JaloArray.of(new JaloInt(2), new JaloInt(1))).toString());
    }
}
