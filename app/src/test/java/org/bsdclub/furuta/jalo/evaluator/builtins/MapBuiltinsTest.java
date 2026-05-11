package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.evaluator.Evaluator;
import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonNumber;
import org.bsdclub.furuta.jalo.json.JsonObject;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.lexer.Lexer;
import org.bsdclub.furuta.jalo.parser.Parser;
import org.junit.jupiter.api.Test;

class MapBuiltinsTest {
    private final Lexer lexer = new Lexer();
    private final Parser parser = new Parser();
    private final Evaluator evaluator = new Evaluator();

    private org.bsdclub.furuta.jalo.json.JsonValue parse(String input) {
        return parser.parseStandard(lexer.tokenize(input));
    }

    @Test
    void bB16_assoc() {
        assertThat(evaluator.eval(parse("(assoc (backquote (map)) (quote \"a\") 1i)")))
            .isEqualTo(JsonObject.empty().put("a", new JsonNumber(1)));
    }

    @Test
    void bB17_dissoc() {
        assertThat(evaluator.eval(parse("(dissoc (backquote (map (\"a\" 1i) (\"b\" 2i))) (quote \"a\"))")))
            .isEqualTo(JsonObject.empty().put("b", new JsonNumber(2)));
    }

    @Test
    void bB18_keys() {
        Object value = evaluator.eval(parse("(keys (backquote (map (\"a\" 1i) (\"b\" 2i))))"));
        assertThat(value).isInstanceOf(JsonArray.class);
        JsonArray arr = (JsonArray) value;
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0)).isIn(new JsonString("a"), new JsonString("b"));
        assertThat(arr.get(1)).isIn(new JsonString("a"), new JsonString("b"));
        assertThat(arr.get(0)).isNotEqualTo(arr.get(1));
    }

    @Test
    void bB19_vals() {
        Object value = evaluator.eval(parse("(vals (backquote (map (\"a\" 1i) (\"b\" 2i))))"));
        assertThat(value).isInstanceOf(JsonArray.class);
        JsonArray arr = (JsonArray) value;
        assertThat(arr.size()).isEqualTo(2);
        assertThat(arr.get(0)).isIn(new JsonNumber(1), new JsonNumber(2));
        assertThat(arr.get(1)).isIn(new JsonNumber(1), new JsonNumber(2));
        assertThat(arr.get(0)).isNotEqualTo(arr.get(1));
    }

    @Test
    void bB20_merge() {
        assertThat(evaluator.eval(parse("(merge (backquote (map (\"a\" 1i))) (backquote (map (\"b\" 2i))))")))
            .isEqualTo(JsonObject.empty().put("a", new JsonNumber(1)).put("b", new JsonNumber(2)));
    }
}
