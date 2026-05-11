package org.bsdclub.furuta.jalo.evaluator.builtins;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.evaluator.Environment;
import org.junit.jupiter.api.Test;

class BuiltinRegistryTest {
    @Test
    void registerAndLookupReturnsFunction() {
        BuiltinRegistry registry = new BuiltinRegistry();
        BuiltinFunction fn = (args, env) -> new JaloNumber(42.0);

        registry.register("x", fn);

        assertThat(registry.lookup("x")).containsSame(fn);
    }

    @Test
    void lookupUnknownReturnsEmpty() {
        BuiltinRegistry registry = new BuiltinRegistry();
        assertThat(registry.lookup("missing")).isEmpty();
    }

    @Test
    void registerOverridesExistingName() {
        BuiltinRegistry registry = new BuiltinRegistry();
        BuiltinFunction first = (args, env) -> new JaloNumber(1.0);
        BuiltinFunction second = (args, env) -> new JaloNumber(2.0);

        registry.register("x", first);
        registry.register("x", second);

        assertThat(registry.lookup("x")).containsSame(second);
    }

    @Test
    void functionCanBeAppliedFromLookup() {
        BuiltinRegistry registry = new BuiltinRegistry();
        registry.register("id", (args, env) -> args.get(0));

        BuiltinFunction fn = registry.lookup("id").orElseThrow();
        assertThat(fn.apply(java.util.List.of(new JaloNumber(3.0)), Environment.root()))
            .isEqualTo(new JaloNumber(3.0));
    }

    @Test
    void lookupIsCaseSensitive() {
        BuiltinRegistry registry = new BuiltinRegistry();
        registry.register("str-count", (args, env) -> new JaloNumber(1.0));

        assertThat(registry.lookup("STR-COUNT")).isEmpty();
    }
}
