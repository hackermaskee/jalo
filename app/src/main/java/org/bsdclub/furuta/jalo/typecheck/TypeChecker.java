package org.bsdclub.furuta.jalo.typecheck;

import org.bsdclub.furuta.jalo.json.JsonArray;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.json.JsonValue;

public final class TypeChecker {
    private Scope topLevel = Scope.empty();

    public void check(JsonValue value) {
        topLevel = checkValue(value, topLevel);
    }

    private Scope checkValue(JsonValue value, Scope scope) {
        if (value instanceof JsonString s) {
            if (!scope.isResolved(s.value())) {
                throw new TypeCheckException("Unbound variable: " + s.value() + " in " + value);
            }
            return scope;
        }

        if (!(value instanceof JsonArray array)) {
            return scope;
        }

        if (array.size() == 0 || !(array.get(0) instanceof JsonString op)) {
            for (int i = 0; i < array.size(); i++) {
                checkValue(array.get(i), scope);
            }
            return scope;
        }

        return switch (op.value()) {
            case "quote" -> checkQuote(array, scope);
            case "if" -> checkIf(array, scope);
            case "def" -> checkDef(array, scope);
            case "declare" -> checkDeclare(array, scope);
            case "fn" -> checkFn(array, scope);
            case "let" -> checkLet(array, scope, false);
            case "let*" -> checkLet(array, scope, true);
            case "letrec" -> checkLetRec(array, scope);
            default -> {
                for (int i = 1; i < array.size(); i++) {
                    checkValue(array.get(i), scope);
                }
                yield scope;
            }
        };
    }

    private Scope checkDef(JsonArray form, Scope scope) {
        if (form.size() != 3 || !(form.get(1) instanceof JsonString name)) {
            throw new TypeCheckException("Wrong arity for def: " + form);
        }
        checkValue(form.get(2), scope);
        return scope.withBinding(name.value());
    }

    private Scope checkQuote(JsonArray form, Scope scope) {
        if (form.size() != 2) {
            throw new TypeCheckException("Wrong arity for quote: " + form);
        }
        return scope;
    }

    private Scope checkIf(JsonArray form, Scope scope) {
        if (form.size() != 4) {
            throw new TypeCheckException("Wrong arity for if: " + form);
        }
        checkValue(form.get(1), scope);
        checkValue(form.get(2), scope);
        checkValue(form.get(3), scope);
        return scope;
    }

    private Scope checkDeclare(JsonArray form, Scope scope) {
        if (form.size() < 2) {
            throw new TypeCheckException("Wrong arity for declare: " + form);
        }
        Scope declared = scope;
        for (int i = 1; i < form.size(); i++) {
            if (!(form.get(i) instanceof JsonString name)) {
                throw new TypeCheckException("declare arguments must be identifiers: " + form);
            }
            declared = declared.withDeclaration(name.value());
        }
        return declared;
    }

    private Scope checkFn(JsonArray form, Scope scope) {
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray params)) {
            throw new TypeCheckException("Wrong arity for fn: " + form);
        }

        Scope fnScope = scope;
        boolean restSeen = false;
        for (int i = 0; i < params.size(); i++) {
            if (!(params.get(i) instanceof JsonString p)) {
                throw new TypeCheckException("fn params must be identifiers: " + form);
            }

            if ("&".equals(p.value())) {
                if (restSeen || i != params.size() - 2) {
                    throw new TypeCheckException("fn rest marker '&' must appear once before rest param: " + form);
                }
                if (!(params.get(i + 1) instanceof JsonString restParam) || "&".equals(restParam.value())) {
                    throw new TypeCheckException("fn rest parameter must be one identifier: " + form);
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

    private Scope checkLet(JsonArray form, Scope scope, boolean sequential) {
        String formName = sequential ? "let*" : "let";
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray bindings)) {
            throw new TypeCheckException("Wrong arity for " + formName + ": " + form);
        }
        if (bindings.size() % 2 != 0) {
            throw new TypeCheckException(formName + " bindings must be even-length: " + form);
        }

        Scope bodyScope = scope;
        Scope evalScope = scope;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new TypeCheckException("binding name must be identifier: " + form);
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

    private Scope checkLetRec(JsonArray form, Scope scope) {
        if (form.size() < 3 || !(form.get(1) instanceof JsonArray bindings) || bindings.size() % 2 != 0) {
            throw new TypeCheckException("Wrong arity for letrec: " + form);
        }

        Scope recScope = scope;
        for (int i = 0; i < bindings.size(); i += 2) {
            if (!(bindings.get(i) instanceof JsonString name)) {
                throw new TypeCheckException("binding name must be identifier: " + form);
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
}
