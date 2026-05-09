package org.bsdclub.furuta.jalo.syntaxcheck;

import org.organicdesign.fp.collections.PersistentHashSet;

/**
 * Name-resolution scope used by {@link SyntaxChecker}.
 *
 * <p>Layer: SyntaxChecker (support structure, per DESIGN.md §1).
 * Tracks bound identifiers and separately-tracked declared names.
 */
final class Scope {
    private final PersistentHashSet<String> bound;
    private final PersistentHashSet<String> declared;

    private Scope(PersistentHashSet<String> bound, PersistentHashSet<String> declared) {
        this.bound = bound;
        this.declared = declared;
    }

    /**
     * Creates an empty scope.
     *
     * @return empty scope with no bound or declared names
     */
    static Scope empty() {
        return new Scope(PersistentHashSet.empty(), PersistentHashSet.empty());
    }

    /**
     * Returns a new scope with a bound identifier.
     *
     * @param name identifier to bind
     * @return copied scope containing the binding
     */
    Scope withBinding(String name) {
        return new Scope(bound.put(name), declared);
    }

    /**
     * Returns a new scope with a declared identifier.
     *
     * @param name identifier to declare
     * @return copied scope containing the declaration
     */
    Scope withDeclaration(String name) {
        return new Scope(bound, declared.put(name));
    }

    /**
     * Checks whether an identifier resolves in this scope.
     *
     * @param name identifier to resolve
     * @return {@code true} if bound or declared in this scope
     */
    boolean isResolved(String name) {
        return bound.contains(name) || declared.contains(name);
    }
}
