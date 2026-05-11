package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloNumber;
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
}
