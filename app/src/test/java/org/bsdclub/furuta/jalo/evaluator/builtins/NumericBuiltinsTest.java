package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.junit.jupiter.api.Test;

class NumericBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) { return parser.parseStandard(lexer.tokenize(input)); }

    @Test
    void quotAndMod() {
        assertThat(evaluator.eval(parse("(quot 7i 2i)"))).isEqualTo(new JaloInt(3));
        assertThat(evaluator.eval(parse("(mod -7i 3i)"))).isEqualTo(new JaloInt(2));
    }

    @Test
    void floorRoundAndNotEq() {
        assertThat(evaluator.eval(parse("(floor 3.9)"))).isEqualTo(new JaloNumber(3));
        assertThat(evaluator.eval(parse("(round 2.5)"))).isEqualTo(new JaloNumber(2));
        assertThat(evaluator.eval(parse("(not= 1i 2i)"))).isEqualTo(JaloBool.TRUE);
    }

    @Test
    void remAbsPowPosAndNan() {
        assertThat(evaluator.eval(parse("(rem -7i 3i)"))).isEqualTo(new JaloInt(-1));
        assertThat(evaluator.eval(parse("(abs -5i)"))).isEqualTo(new JaloInt(5));
        assertThat(evaluator.eval(parse("(pow 2.0 3.0)"))).isEqualTo(new JaloNumber(8.0));
        assertThat(evaluator.eval(parse("(pos? 1i)"))).isEqualTo(JaloBool.TRUE);
        assertThat(evaluator.eval(parse("(nan? (sqrt -1.0))"))).isEqualTo(JaloBool.TRUE);
    }
}
