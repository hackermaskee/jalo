package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.Map;
import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloFunction;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloNull;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** Type predicate built-ins. */
public final class TypeBuiltins {
    private TypeBuiltins() { }

    /**
     * Registers type predicates into the built-in registry.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("pure-json?", (args, env) -> {
            if (args.size() != 1) {
                throw new RuntimeException("pure-json?: expected 1 argument, got " + args.size());
            }
            return isPureJson(args.get(0)) ? JaloBool.TRUE : JaloBool.FALSE;
        });
    }

    static boolean isPureJson(JaloValue value) {
        return switch (value) {
            case JaloNull ignored -> true;
            case JaloBool ignored -> true;
            case JaloNumber ignored -> true;
            case JaloString ignored -> true;
            case JaloArray arr -> {
                for (int i = 0; i < arr.size(); i++) {
                    if (!isPureJson(arr.get(i))) {
                        yield false;
                    }
                }
                yield true;
            }
            case JaloMap map -> {
                for (Map.Entry<String, JaloValue> entry : map.entries().entrySet()) {
                    if (!isPureJson(entry.getValue())) {
                        yield false;
                    }
                }
                yield true;
            }
            case JaloInt ignored -> false;
            case JaloLong ignored -> false;
            case JaloFunction ignored -> false;
        };
    }
}
