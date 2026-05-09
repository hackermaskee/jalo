package org.bsdclub.furuta.jalo.evaluator;

import java.util.HashMap;
import java.util.Map;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.bsdclub.furuta.jalo.value.JaloValue;
import org.organicdesign.fp.collections.PersistentHashMap;

public final class Environment {
    private final PersistentHashMap<String, JaloValue> localBindings;
    private final Map<String, JaloValue> globalBindings;

    private Environment(PersistentHashMap<String, JaloValue> localBindings, Map<String, JaloValue> globalBindings) {
        this.localBindings = localBindings;
        this.globalBindings = globalBindings;
    }

    public static Environment root() {
        return new Environment(PersistentHashMap.empty(), new HashMap<>());
    }

    public Environment bind(String name, JaloValue value) {
        return new Environment(localBindings.assoc(name, value), globalBindings);
    }

    public void defineGlobal(String name, JaloValue value) {
        globalBindings.put(name, value);
    }

    public boolean hasGlobal(String name) {
        return globalBindings.containsKey(name);
    }

    public JaloValue getGlobal(String name) {
        return globalBindings.get(name);
    }

    public void removeGlobal(String name) {
        globalBindings.remove(name);
    }

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
