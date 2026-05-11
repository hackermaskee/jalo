package org.bsdclub.furuta.jalo.jq;

import java.util.List;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Transpiles a minimal jq-compatible filter subset into jalo S-expression AST.
 *
 * <p>This parser intentionally targets PR-A basic filters only.
 */
public final class JqParser {
    /**
     * Transpiles jq filter text into a jalo AST expression.
     *
     * @param source jq filter source
     * @return jalo AST expression
     * @throws IllegalArgumentException when the filter is outside the PR-A subset
     */
    public JaloValue transpile(String source) {
        String s = source.trim();
        if (".".equals(s)) {
            return new JaloString("x");
        }
        if (s.startsWith(".") && s.length() > 1 && Character.isAlphabetic(s.charAt(1))) {
            return JaloArray.of(
                new JaloString("get-in"),
                new JaloString("x"),
                JaloArray.of(
                    new JaloString("quasiquote"),
                    JaloArray.of(new JaloString("array"), new JaloString(s.substring(1)))));
        }
        if (s.contains("|")) {
            String[] parts = s.split("\\|", 2);
            JaloValue left = transpile(parts[0].trim());
            JaloValue right = transpile(parts[1].trim());
            return JaloArray.of(
                new JaloString("let"),
                JaloArray.of(new JaloString("x"), left),
                right
            );
        }
        if (".[]".equals(s)) {
            return JaloArray.of(new JaloString("identity"), new JaloString("x"));
        }
        if (s.startsWith(".[") && s.endsWith("]")) {
            int index = Integer.parseInt(s.substring(2, s.length() - 1));
            return JaloArray.of(new JaloString("nth"), new JaloString("x"), new JaloInt(index));
        }
        if ("[.]".equals(s)) {
            return JaloArray.of(new JaloString("quasiquote"), JaloArray.of(new JaloString("array"), new JaloString("x")));
        }
        if (s.startsWith("map(") && s.endsWith(")")) {
            JaloValue body = transpileInLambda(s.substring(4, s.length() - 1).trim());
            return JaloArray.of(new JaloString("map"), JaloArray.of(new JaloString("fn"), JaloArray.of(new JaloString("v")), body), new JaloString("x"));
        }
        if (s.startsWith("select(") && s.endsWith(")")) {
            JaloValue body = transpileInLambda(s.substring(7, s.length() - 1).trim());
            return JaloArray.of(new JaloString("filter"), JaloArray.of(new JaloString("fn"), JaloArray.of(new JaloString("v")), body), new JaloString("x"));
        }
        if ("length".equals(s)) {
            return JaloArray.of(new JaloString("count"), new JaloString("x"));
        }
        if (s.startsWith("@")) {
            return JaloArray.of(new JaloString(s.replace("_", "-").replace("@", "at-")), new JaloString("x"));
        }
        throw new IllegalArgumentException("unsupported jq filter: " + s);
    }

    private JaloValue transpileInLambda(String expr) {
        if (".".equals(expr)) {
            return new JaloString("v");
        }
        if (expr.startsWith(".") && expr.length() > 1) {
            return JaloArray.of(
                new JaloString("get-in"),
                new JaloString("v"),
                JaloArray.of(
                    new JaloString("quasiquote"),
                    JaloArray.of(new JaloString("array"), new JaloString(expr.substring(1)))));
        }
        return new JaloString("v");
    }

    /**
     * Transpiles already-tokenized jq source.
     *
     * @param tokens token list
     * @return jalo AST expression
     */
    public JaloValue transpile(List<JqLexer.Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (JqLexer.Token token : tokens) {
            if (token.kind() == JqLexer.Kind.END) {
                break;
            }
            sb.append(token.text());
        }
        return transpile(sb.toString());
    }
}
