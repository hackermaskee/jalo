package org.bsdclub.furuta.jalo.value;

import org.bsdclub.furuta.jalo.evaluator.builtins.BuiltinFunction;
import org.bsdclub.furuta.jalo.evaluator.builtins.Callable;

/**
 * First-class built-in callable value.
 *
 * @param name built-in function name
 * @param fn function implementation
 */
public record JaloBuiltinFunction(String name, BuiltinFunction fn) implements JaloValue, Callable { }
