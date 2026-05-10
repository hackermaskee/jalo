package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Name-to-function registry for built-in call dispatch.
 */
public final class BuiltinRegistry {
    private final Map<String, BuiltinFunction> map = new HashMap<>();

    /**
     * Registers or replaces a built-in function.
     *
     * @param name built-in name
     * @param fn function implementation
     */
    public void register(String name, BuiltinFunction fn) {
        map.put(name, fn);
    }

    /**
     * Finds a built-in function by name.
     *
     * @param name built-in name
     * @return lookup result
     */
    public Optional<BuiltinFunction> lookup(String name) {
        return Optional.ofNullable(map.get(name));
    }
}
