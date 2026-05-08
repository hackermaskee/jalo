package jalo.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LexerTest {
    private final Lexer lexer = new Lexer();

    @Test void step1_emptyInput() { assertThat(lexer.tokenize("")).isEqualTo(List.of(new Token.Eof(1, 1))); }
    @Test void step2_spaces() { assertThat(lexer.tokenize("   ")).isEqualTo(List.of(new Token.Eof(1, 4))); }
    @Test void step3_newlinesAndSpaces() { assertThat(lexer.tokenize("\n\n  ")).isEqualTo(List.of(new Token.Eof(3, 3))); }
    @Test void step4_zero() { assertThat(lexer.tokenize("0")).isEqualTo(List.of(new Token.NumberDouble(0.0, 1, 1), new Token.Eof(1, 2))); }
    @Test void step5_multiDigit() { assertThat(lexer.tokenize("42")).isEqualTo(List.of(new Token.NumberDouble(42.0, 1, 1), new Token.Eof(1, 3))); }
    @Test void step6_decimal() { assertThat(lexer.tokenize("1.5")).isEqualTo(List.of(new Token.NumberDouble(1.5, 1, 1), new Token.Eof(1, 4))); }
    @Test void step7_exponent() { assertThat(lexer.tokenize("1e3")).isEqualTo(List.of(new Token.NumberDouble(1000.0, 1, 1), new Token.Eof(1, 4))); }
    @Test void step8_negative() { assertThat(lexer.tokenize("-5")).isEqualTo(List.of(new Token.NumberDouble(-5.0, 1, 1), new Token.Eof(1, 3))); }
    @Test void step9_leadingZeroError() { assertThatThrownBy(() -> lexer.tokenize("01")).isInstanceOf(LexerException.class).hasMessageContaining("leading zero"); }
    @Test void step9b_emptyFraction() { assertThatThrownBy(() -> lexer.tokenize("1.")).isInstanceOf(LexerException.class).hasMessageContaining("invalid number"); }
    @Test void step9c_emptyExponent() { assertThatThrownBy(() -> lexer.tokenize("1e")).isInstanceOf(LexerException.class).hasMessageContaining("invalid exponent"); }
    @Test void step10_intSuffix() { assertThat(lexer.tokenize("42i")).isEqualTo(List.of(new Token.NumberInt(42, 1, 1), new Token.Eof(1, 4))); }
    @Test void step11_longSuffix() { assertThat(lexer.tokenize("42l")).isEqualTo(List.of(new Token.NumberLong(42L, 1, 1), new Token.Eof(1, 4))); }
    @Test void step12_nullLiteral() { assertThat(lexer.tokenize("#null")).isEqualTo(List.of(new Token.Null(1, 1), new Token.Eof(1, 6))); }
    @Test void step13_trueLiteral() { assertThat(lexer.tokenize("#true")).isEqualTo(List.of(new Token.True(1, 1), new Token.Eof(1, 6))); }
    @Test void step14_falseLiteral() { assertThat(lexer.tokenize("#false")).isEqualTo(List.of(new Token.False(1, 1), new Token.Eof(1, 7))); }
    @Test void step15_unknownHashLiteralError() { assertThatThrownBy(() -> lexer.tokenize("#abc")).isInstanceOf(LexerException.class).hasMessageContaining("#abc"); }
    @Test void step16_stringBasic() { assertThat(lexer.tokenize("\"hello\"")).isEqualTo(List.of(new Token.Str("hello", 1, 1), new Token.Eof(1, 8))); }
    @Test void step17_stringEmpty() { assertThat(lexer.tokenize("\"\"")).isEqualTo(List.of(new Token.Str("", 1, 1), new Token.Eof(1, 3))); }
    @Test void step18_stringEscape() { assertThat(lexer.tokenize("\"a\\nb\"")).isEqualTo(List.of(new Token.Str("a\nb", 1, 1), new Token.Eof(1, 7))); }
    @Test void step19_unterminatedString() { assertThatThrownBy(() -> lexer.tokenize("\"abc")).isInstanceOf(LexerException.class).hasMessageContaining("unterminated string"); }
    @Test void step19b_invalidEscape() { assertThatThrownBy(() -> lexer.tokenize("\"\\q\"")).isInstanceOf(LexerException.class).hasMessageContaining("invalid escape"); }
    @Test void step20_identifierAlpha() { assertThat(lexer.tokenize("foo")).isEqualTo(List.of(new Token.Identifier("foo", 1, 1), new Token.Eof(1, 4))); }
    @Test void step21_identifierQuote() { assertThat(lexer.tokenize("foo'")).isEqualTo(List.of(new Token.Identifier("foo'", 1, 1), new Token.Eof(1, 5))); }
    @Test void step22_identifierOperator() { assertThat(lexer.tokenize("+")).isEqualTo(List.of(new Token.Identifier("+", 1, 1), new Token.Eof(1, 2))); }
    @Test void step23_identifierOperatorComposite() { assertThat(lexer.tokenize("<=")).isEqualTo(List.of(new Token.Identifier("<=", 1, 1), new Token.Eof(1, 3))); }
    @Test void step24_bracketsParensBraces() {
        assertThat(lexer.tokenize("[](){}")).isEqualTo(List.of(
            new Token.LBracket(1, 1), new Token.RBracket(1, 2),
            new Token.LParen(1, 3), new Token.RParen(1, 4),
            new Token.LBrace(1, 5), new Token.RBrace(1, 6),
            new Token.Eof(1, 7)
        ));
    }
    @Test void step25_colonComma() { assertThat(lexer.tokenize(":,")).isEqualTo(List.of(new Token.Colon(1, 1), new Token.Comma(1, 2), new Token.Eof(1, 3))); }
    @Test void step26_specialChars() {
        assertThat(lexer.tokenize("`$@%")).isEqualTo(List.of(
            new Token.Backquote(1, 1), new Token.Dollar(1, 2), new Token.At(1, 3), new Token.Percent(1, 4), new Token.Eof(1, 5)
        ));
    }
    @Test void step27_comboListCall() {
        assertThat(lexer.tokenize("(+ 1 2)")).isEqualTo(List.of(
            new Token.LParen(1, 1), new Token.Identifier("+", 1, 2),
            new Token.NumberDouble(1.0, 1, 4), new Token.NumberDouble(2.0, 1, 6),
            new Token.RParen(1, 7), new Token.Eof(1, 8)
        ));
    }
    @Test void step28_comboVector() {
        assertThat(lexer.tokenize("[1, 2, 3]")).isEqualTo(List.of(
            new Token.LBracket(1, 1), new Token.NumberDouble(1.0, 1, 2), new Token.Comma(1, 3),
            new Token.NumberDouble(2.0, 1, 5), new Token.Comma(1, 6),
            new Token.NumberDouble(3.0, 1, 8), new Token.RBracket(1, 9), new Token.Eof(1, 10)
        ));
    }
    @Test void step29_comboMap() {
        assertThat(lexer.tokenize("{a: 1}")).isEqualTo(List.of(
            new Token.LBrace(1, 1), new Token.Identifier("a", 1, 2), new Token.Colon(1, 3),
            new Token.NumberDouble(1.0, 1, 5), new Token.RBrace(1, 6), new Token.Eof(1, 7)
        ));
    }
    @Test void step30_comboTemplateLike() {
        assertThat(lexer.tokenize("`[$x @rest]")).isEqualTo(List.of(
            new Token.Backquote(1, 1), new Token.LBracket(1, 2), new Token.Dollar(1, 3),
            new Token.Identifier("x", 1, 4), new Token.At(1, 6), new Token.Identifier("rest", 1, 7),
            new Token.RBracket(1, 11), new Token.Eof(1, 12)
        ));
    }
    @Test void step31_multilinePosition() {
        assertThat(lexer.tokenize("foo\nbar")).isEqualTo(List.of(
            new Token.Identifier("foo", 1, 1), new Token.Identifier("bar", 2, 1), new Token.Eof(2, 4)
        ));
    }
}
