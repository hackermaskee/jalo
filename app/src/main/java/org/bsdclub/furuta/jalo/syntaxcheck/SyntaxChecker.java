package org.bsdclub.furuta.jalo.syntaxcheck;

import java.util.HashSet;
import java.util.Set;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Static syntactic checker for jalo JSON model AST.
 *
 * <p>Layer: SyntaxChecker (per DESIGN.md §1 architecture table).
 * Validates name resolution, special-form structure, and pattern syntax
 * over JSON model produced by parser layer.
 *
 * @see Scope
 * @see SyntaxCheckException
 * @see <a href="../../../docs/SPEC.md#42">SPEC §4.2 special forms</a>
 * @see <a href="../../../docs/SPEC.md#53">SPEC §5.3 pattern constraints</a>
 */
public final class SyntaxChecker {
    private Scope topLevel = Scope.empty();

    /**
     * Validates a JSON model AST.
     *
     * @param value AST node to validate
     * @throws SyntaxCheckException if name resolution or special-form syntax is invalid
     */
    public void check(JaloValue value) {
        topLevel = checkValue(value, topLevel);
    }

    private Scope checkValue(JaloValue value, Scope scope) {
        if (value instanceof JaloString s) {
            if (!scope.isResolved(s.value())) {
                throw new SyntaxCheckException("Unbound variable: " + s.value() + " in " + value);
            }
            return scope;
        }

        if (!(value instanceof JaloArray array)) {
            return scope;
        }

        if (array.size() == 0 || !(array.get(0) instanceof JaloString op)) {
            for (int i = 0; i < array.size(); i++) {
                checkValue(array.get(i), scope);
            }
            return scope;
        }

        return switch (op.value()) {
            case "quote" -> checkQuote(array, scope);
            case "quasiquote" -> checkBackquote(array, scope);
            case "if" -> checkIf(array, scope);
            case "def" -> checkDef(array, scope);
            case "declare" -> checkDeclare(array, scope);
            case "fn" -> checkFn(array, scope);
            case "let" -> checkLet(array, scope, false);
            case "let*" -> checkLet(array, scope, true);
            case "letrec" -> checkLetRec(array, scope);
            case "match" -> checkMatch(array, scope);
            default -> {
                for (int i = 1; i < array.size(); i++) {
                    checkValue(array.get(i), scope);
                }
                yield scope;
            }
        };
    }

    private Scope checkDef(JaloArray form, Scope scope) {
        if (form.size() != 3 || !(form.get(1) instanceof JaloString name)) {
            throw new SyntaxCheckException("Wrong arity for def: " + form);
        }
        checkValue(form.get(2), scope);
        return scope.withBinding(name.value());
    }

    private Scope checkQuote(JaloArray form, Scope scope) {
        if (form.size() != 2) {
            throw new SyntaxCheckException("Wrong arity for quote: " + form);
        }
        return scope;
    }

    private Scope checkBackquote(JaloArray form, Scope scope) {
        if (form.size() != 2) {
            throw new SyntaxCheckException("Wrong arity for quasiquote: " + form);
        }
        checkPattern(form.get(1));
        return scope;
    }

    private void checkPattern(JaloValue node) {
        if (!(node instanceof JaloArray array) || array.size() == 0 || !(array.get(0) instanceof JaloString op)) {
            return;
        }

        switch (op.value()) {
            case "var" -> checkDollar(array);
            case "rest-seq" -> checkAt(array);
            case "rest-map" -> checkPercent(array);
            case "array" -> checkPatternArray(array);
            case "map" -> checkPatternMap(array);
            default -> {
                for (int i = 1; i < array.size(); i++) {
                    checkPattern(array.get(i));
                }
            }
        }
    }

    private void checkDollar(JaloArray form) {
        if (form.size() != 2 || !(form.get(1) instanceof JaloString)) {
            throw new SyntaxCheckException("Pattern: var followed by non-variable: " + form);
        }
    }

    private void checkAt(JaloArray form) {
        if (form.size() != 2 || !(form.get(1) instanceof JaloString)) {
            throw new SyntaxCheckException("Pattern: rest-seq followed by non-variable: " + form);
        }
    }

    private void checkPercent(JaloArray form) {
        if (form.size() != 2 || !(form.get(1) instanceof JaloString)) {
            throw new SyntaxCheckException("Pattern: rest-map followed by non-variable: " + form);
        }
    }

    private void checkPatternArray(JaloArray form) {
        int atCount = 0;
        for (int i = 1; i < form.size(); i++) {
            JaloValue child = form.get(i);
            if (child instanceof JaloArray childArray
                    && childArray.size() > 0
                    && childArray.get(0) instanceof JaloString op
                    && "rest-seq".equals(op.value())) {
                atCount++;
            }
            checkPattern(child);
        }
        if (atCount > 1) {
            throw new SyntaxCheckException("Pattern: multiple 'rest-seq' in array: " + form);
        }
    }

    private void checkPatternMap(JaloArray form) {
        int percentCount = 0;
        for (int i = 1; i < form.size(); i++) {
            JaloValue child = form.get(i);
            if (child instanceof JaloArray childArray && childArray.size() > 0 && childArray.get(0) instanceof JaloString op) {
                if ("rest-map".equals(op.value())) {
                    percentCount++;
                    checkPattern(child);
                    continue;
                }
            }

            if (isDollarKeyEntry(child)) {
                throw new SyntaxCheckException("Pattern: var key not allowed in map: " + form);
            }
            checkPattern(child);
        }
        if (percentCount > 1) {
            throw new SyntaxCheckException("Pattern: multiple 'rest-map' in map: " + form);
        }
    }

    private boolean isDollarKeyEntry(JaloValue node) {
        if (!(node instanceof JaloArray array) || array.size() == 0) {
            return false;
        }

        if (isDollarForm(array.get(0))) {
            return true;
        }

        if (array.size() == 1 && array.get(0) instanceof JaloArray nested) {
            return isDollarKeyEntry(nested);
        }
        return false;
    }

    private boolean isDollarForm(JaloValue node) {
        if (!(node instanceof JaloArray array) || array.size() == 0 || !(array.get(0) instanceof JaloString op)) {
            return false;
        }
        return "var".equals(op.value());
    }

    private Scope checkIf(JaloArray form, Scope scope) {
        if (form.size() != 4) {
            throw new SyntaxCheckException("Wrong arity for if: " + form);
        }
        checkValue(form.get(1), scope);
        checkValue(form.get(2), scope);
        checkValue(form.get(3), scope);
        return scope;
    }

    private Scope checkDeclare(JaloArray form, Scope scope) {
        if (form.size() < 2) {
            throw new SyntaxCheckException("Wrong arity for declare: " + form);
        }
        Scope declared = scope;
        for (int i = 1; i < form.size(); i++) {
            if (!(form.get(i) instanceof JaloString name)) {
                throw new SyntaxCheckException("declare arguments must be identifiers: " + form);
            }
            declared = declared.withDeclaration(name.value());
        }
        return declared;
    }

    private Scope checkFn(JaloArray form, Scope scope) {
        if (form.size() < 3 || !(form.get(1) instanceof JaloArray params)) {
            throw new SyntaxCheckException("Wrong arity for fn: " + form);
        }

        Scope fnScope = scope;
        boolean restSeen = false;
        for (int i = 0; i < params.size(); i++) {
            if (!(params.get(i) instanceof JaloString p)) {
                throw new SyntaxCheckException("fn params must be identifiers: " + form);
            }

            if ("&".equals(p.value())) {
                if (restSeen || i != params.size() - 2) {
                    throw new SyntaxCheckException("fn rest marker '&' must appear once before rest param: " + form);
                }
                if (!(params.get(i + 1) instanceof JaloString restParam) || "&".equals(restParam.value())) {
                    throw new SyntaxCheckException("fn rest parameter must be one identifier: " + form);
                }
                fnScope = fnScope.withBinding(restParam.value());
                restSeen = true;
                i++;
                continue;
            }

            fnScope = fnScope.withBinding(p.value());
        }

        for (int i = 2; i < form.size(); i++) {
            checkValue(form.get(i), fnScope);
        }
        return scope;
    }

    private Scope checkLet(JaloArray form, Scope scope, boolean sequential) {
        String formName = sequential ? "let*" : "let";
        if (form.size() < 3 || !(form.get(1) instanceof JaloArray bindings)) {
            throw new SyntaxCheckException("Wrong arity for " + formName + ": " + form);
        }
        if (bindings.size() % 2 != 0) {
            throw new SyntaxCheckException(formName + " bindings must be even-length: " + form);
        }

        Scope bodyScope = scope;
        Scope evalScope = scope;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JaloString name)) {
                throw new SyntaxCheckException("binding name must be identifier: " + form);
            }
            checkValue(bindings.get(i + 1), sequential ? evalScope : scope);
            bodyScope = bodyScope.withBinding(name.value());
            if (sequential) {
                evalScope = bodyScope;
            }
        }

        for (int i = 2; i < form.size(); i++) {
            checkValue(form.get(i), bodyScope);
        }
        return scope;
    }

    private Scope checkLetRec(JaloArray form, Scope scope) {
        if (form.size() < 3 || !(form.get(1) instanceof JaloArray bindings) || bindings.size() % 2 != 0) {
            throw new SyntaxCheckException("Wrong arity for letrec: " + form);
        }

        Scope recScope = scope;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JaloString name)) {
                throw new SyntaxCheckException("binding name must be identifier: " + form);
            }
            recScope = recScope.withBinding(name.value());
        }

        for (int i = 0; i < bindings.size(); i += 2) {
            checkValue(bindings.get(i + 1), recScope);
        }
        for (int i = 2; i < form.size(); i++) {
            checkValue(form.get(i), recScope);
        }
        return scope;
    }

    private Scope checkMatch(JaloArray form, Scope scope) {
        if (form.size() < 4 || ((form.size() - 2) % 2 != 0)) {
            throw new SyntaxCheckException("Wrong arity for match: " + form);
        }
        checkValue(form.get(1), scope);
        for (int i = 2; i < form.size(); i += 2) {
            JaloValue pattern = form.get(i);
            checkPattern(pattern);
            Scope branchScope = scope.bindAll(collectPatternBindings(pattern));
            checkValue(form.get(i + 1), branchScope);
        }
        return scope;
    }

    private Set<String> collectPatternBindings(JaloValue pattern) {
        Set<String> names = new HashSet<>();
        collectPatternBindingsInto(pattern, names);
        return names;
    }

    private void collectPatternBindingsInto(JaloValue node, Set<String> names) {
        if (!(node instanceof JaloArray array) || array.size() == 0 || !(array.get(0) instanceof JaloString op)) {
            return;
        }
        switch (op.value()) {
            case "var", "rest-seq", "rest-map" -> {
                if (array.size() == 2 && array.get(1) instanceof JaloString name && !"_".equals(name.value())) {
                    names.add(name.value());
                }
            }
            default -> {
                for (int i = 1; i < array.size(); i++) {
                    collectPatternBindingsInto(array.get(i), names);
                }
            }
        }
    }
}
