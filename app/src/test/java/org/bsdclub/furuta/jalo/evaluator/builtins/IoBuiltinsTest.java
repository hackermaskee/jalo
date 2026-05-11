package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.junit.jupiter.api.Test;

class IoBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.value.JaloValue parse(String input) { return parser.parseStandard(lexer.tokenize(input)); }

    @Test
    void toJson() {
        assertThat(evaluator.eval(parse("(to-json (backquote (map (\"a\" 1.0) (\"b\" (array 2.0 3.0)))))")))
            .isEqualTo(new JaloString("{\"a\":1.0,\"b\":[2.0,3.0]}"));
    }

    @Test
    void printlnAndToJsonErrorForInt() {
        assertThat(evaluator.eval(parse("(println (backquote \"hello\"))"))).isEqualTo(JaloNull.INSTANCE);
        assertThatThrownBy(() -> evaluator.eval(parse("(to-json 42i)")))
            .isInstanceOf(JaloEffectSignal.class);
    }
}
