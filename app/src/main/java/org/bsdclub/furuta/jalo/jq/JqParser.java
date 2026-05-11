package org.bsdclub.furuta.jalo.jq;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Transpiles a jq-compatible filter subset into jalo S-expression AST.
 */
public final class JqParser {
    /**
     * Transpiles jq filter text into a jalo AST expression.
     *
     * @param source jq filter source
     * @return jalo AST expression
     */
    public JaloValue transpile(String source) {
        return transpileExpr(source.trim(), "x");
    }

    private JaloValue transpileExpr(String source, String inputSym) {
        String s = source.trim();

        Matcher ifMatcher = Pattern.compile("^if\\s+(.+)\\s+then\\s+(.+)\\s+else\\s+(.+)\\s+end$").matcher(s);
        if (ifMatcher.matches()) {
            return JaloArray.of(
                new JaloString("if"),
                transpileExpr(ifMatcher.group(1), inputSym),
                transpileExpr(ifMatcher.group(2), inputSym),
                transpileExpr(ifMatcher.group(3), inputSym));
        }

        Matcher asArrayMatcher = Pattern.compile("^\\.\\s+as\\s+\\[\\s*\\$(\\w+)\\s*,\\s*\\$(\\w+)\\s*\\]\\s*\\|\\s*(.+)$").matcher(s);
        if (asArrayMatcher.matches()) {
            String a = asArrayMatcher.group(1);
            String b = asArrayMatcher.group(2);
            String body = asArrayMatcher.group(3);
            return JaloArray.of(
                new JaloString("let"),
                JaloArray.of(
                    new JaloString(a), JaloArray.of(new JaloString("nth"), new JaloString(inputSym), new JaloInt(0)),
                    new JaloString(b), JaloArray.of(new JaloString("nth"), new JaloString(inputSym), new JaloInt(1))),
                transpileExpr(body, inputSym));
        }

        Matcher asMapMatcher = Pattern.compile("^\\.\\s+as\\s+\\{\\s*(\\w+)\\s*:\\s*\\$(\\w+)\\s*}\\s*\\|\\s*(.+)$").matcher(s);
        if (asMapMatcher.matches()) {
            String key = asMapMatcher.group(1);
            String name = asMapMatcher.group(2);
            String body = asMapMatcher.group(3);
            return JaloArray.of(
                new JaloString("let"),
                JaloArray.of(
                    new JaloString(name),
                    JaloArray.of(
                        new JaloString("get-in"),
                        new JaloString(inputSym),
                        JaloArray.of(
                            new JaloString("quasiquote"),
                            JaloArray.of(new JaloString("array"), new JaloString(key))))),
                transpileExpr(body, inputSym));
        }

        int pipeAt = topLevelPipe(s);
        if (pipeAt >= 0) {
            String left = s.substring(0, pipeAt).trim();
            String right = s.substring(pipeAt + 1).trim();
            return JaloArray.of(new JaloString("let"), JaloArray.of(new JaloString("x"), transpileExpr(left, inputSym)), transpileExpr(right, "x"));
        }

        int andAt = s.indexOf(" and ");
        if (andAt >= 0) {
            return JaloArray.of(new JaloString("and"), transpileExpr(s.substring(0, andAt), inputSym), transpileExpr(s.substring(andAt + 5), inputSym));
        }
        int orAt = s.indexOf(" or ");
        if (orAt >= 0) {
            return JaloArray.of(new JaloString("or"), transpileExpr(s.substring(0, orAt), inputSym), transpileExpr(s.substring(orAt + 4), inputSym));
        }
        Matcher gt = Pattern.compile("^(.+)\\s*>\\s*(.+)$").matcher(s);
        if (gt.matches()) {
            return JaloArray.of(new JaloString(">"), transpileExpr(gt.group(1), inputSym), transpileExpr(gt.group(2), inputSym));
        }

        if (".".equals(s)) {
            return new JaloString(inputSym);
        }
        if (s.startsWith("$") && s.length() > 1) {
            return new JaloString(s.substring(1));
        }
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return JaloArray.of(new JaloString("quote"), new JaloString(s.substring(1, s.length() - 1)));
        }
        if (s.matches("-?\\d+")) {
            return new JaloInt(Integer.parseInt(s));
        }
        if (s.startsWith(".") && s.length() > 1 && Character.isAlphabetic(s.charAt(1))) {
            return JaloArray.of(
                new JaloString("get-in"),
                new JaloString(inputSym),
                JaloArray.of(new JaloString("quasiquote"), JaloArray.of(new JaloString("array"), new JaloString(s.substring(1)))));
        }
        if (".[]".equals(s)) {
            return JaloArray.of(new JaloString("identity"), new JaloString(inputSym));
        }
        if (s.startsWith(".[") && s.endsWith("]")) {
            int index = Integer.parseInt(s.substring(2, s.length() - 1));
            return JaloArray.of(new JaloString("nth"), new JaloString(inputSym), new JaloInt(index));
        }
        if ("[.]".equals(s)) {
            return JaloArray.of(new JaloString("quasiquote"), JaloArray.of(new JaloString("array"), new JaloString(inputSym)));
        }
        if (s.startsWith("map(") && s.endsWith(")")) {
            JaloValue body = transpileExpr(s.substring(4, s.length() - 1).trim(), "v");
            return JaloArray.of(new JaloString("map"), JaloArray.of(new JaloString("fn"), JaloArray.of(new JaloString("v")), body), new JaloString(inputSym));
        }
        if (s.startsWith("select(") && s.endsWith(")")) {
            JaloValue body = transpileExpr(s.substring(7, s.length() - 1).trim(), "v");
            return JaloArray.of(new JaloString("filter"), JaloArray.of(new JaloString("fn"), JaloArray.of(new JaloString("v")), body), new JaloString(inputSym));
        }
        if ("length".equals(s)) {
            return JaloArray.of(new JaloString("count"), new JaloString(inputSym));
        }
        if (s.startsWith("@")) {
            return JaloArray.of(new JaloString(s.replace("_", "-").replace("@", "at-")), new JaloString(inputSym));
        }
        throw new IllegalArgumentException("unsupported jq filter: " + s);
    }

    private int topLevelPipe(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            if (c == ')' || c == ']' || c == '}') depth--;
            if (c == '|' && depth == 0) return i;
        }
        return -1;
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
            sb.append(token.text()).append(' ');
        }
        return transpile(sb.toString());
    }
}
