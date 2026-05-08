package org.bsdclub.furuta.jalo.typecheck;

import org.organicdesign.fp.collections.PersistentHashSet;

final class Scope {
    private final PersistentHashSet<String> bound;
    private final PersistentHashSet<String> declared;

    private Scope(PersistentHashSet<String> bound, PersistentHashSet<String> declared) {
        this.bound = bound;
        this.declared = declared;
    }

    static Scope empty() {
        return new Scope(PersistentHashSet.empty(), PersistentHashSet.empty());
    }

    Scope withBinding(String name) {
        return new Scope(bound.put(name), declared);
    }

    Scope withDeclaration(String name) {
        return new Scope(bound, declared.put(name));
    }

    boolean isResolved(String name) {
        return bound.contains(name) || declared.contains(name);
    }
}
