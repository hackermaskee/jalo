package org.bsdclub.furuta.jalo.evaluator;

import java.util.HashMap;
import java.util.Map;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.organicdesign.fp.collections.PersistentHashMap;

/**
 * Functional name environment for {@link Evaluator}.
 *
 * <p>Layer: Evaluation (support structure, per DESIGN.md §1).
 * Provides immutable local bindings and a mutable global namespace shared
 * across derived environments.
 */
public final class Environment {
    private final PersistentHashMap<String, JaloValue> localBindings;
    private final Map<String, JaloValue> globalBindings;

    private Environment(PersistentHashMap<String, JaloValue> localBindings, Map<String, JaloValue> globalBindings) {
        this.localBindings = localBindings;
        this.globalBindings = globalBindings;
    }

    /**
     * Creates a root environment with empty local and global bindings.
     *
     * @return root evaluation environment
     */
    public static Environment root() {
        return new Environment(PersistentHashMap.empty(), new HashMap<>());
    }

    /**
     * Returns a derived environment with one additional local binding.
     *
     * @param name local binding name
     * @param value local binding value
     * @return derived environment
     * @implNote Local bindings are flat-flattened in Paguro
     *           {@code PersistentHashMap.assoc} for O(log32 N) lookup.
     *           Parent chain removed in cmd_404 PR-C (see DESIGN.md §2.10).
     */
    public Environment bind(String name, JaloValue value) {
        return new Environment(localBindings.assoc(name, value), globalBindings);
    }

    /**
     * Defines or replaces a global binding.
     *
     * @param name global name
     * @param value global value
     */
    public void defineGlobal(String name, JaloValue value) {
        globalBindings.put(name, value);
    }

    /**
     * Checks whether a global binding exists.
     *
     * @param name global name
     * @return {@code true} if defined globally
     */
    public boolean hasGlobal(String name) {
        return globalBindings.containsKey(name);
    }

    /**
     * Looks up a value from the global namespace without local fallback.
     *
     * @param name global name
     * @return global value or {@code null} if absent
     */
    public JaloValue getGlobal(String name) {
        return globalBindings.get(name);
    }

    /**
     * Removes a global binding.
     *
     * @param name global name
     */
    public void removeGlobal(String name) {
        globalBindings.remove(name);
    }

    /**
     * Resolves a name from local, then global bindings.
     *
     * @param name identifier to resolve
     * @return resolved value
     * @throws JaloEffectSignal if the identifier is unbound
     */
    public JaloValue lookup(String name) {
        if (localBindings.containsKey(name)) {
            return localBindings.get(name);
        }
        if (globalBindings.containsKey(name)) {
            return globalBindings.get(name);
        }
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Unbound variable: " + name));
    }
}
