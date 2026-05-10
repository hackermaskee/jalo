package org.bsdclub.furuta.jalo.evaluator.builtins;

/**
 * Marker type for callable entities.
 */
public interface Callable {
    /**
     * Named built-in callable value.
     *
     * @param name built-in name
     * @param fn function body
     */
    record BuiltinFn(String name, BuiltinFunction fn) implements Callable {}
}
