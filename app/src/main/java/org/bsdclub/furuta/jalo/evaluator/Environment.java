package org.bsdclub.furuta.jalo.evaluator;

import java.util.HashMap;
import java.util.Map;
import org.bsdclub.furuta.jalo.json.JsonString;
import org.organicdesign.fp.collections.PersistentHashMap;

public final class Environment {
    private final Environment parent;
    private final PersistentHashMap<String, JaloValue> localBindings;
    private final Map<String, JaloValue> globalBindings;

    private Environment(Environment parent, PersistentHashMap<String, JaloValue> localBindings, Map<String, JaloValue> globalBindings) {
        this.parent = parent;
        this.localBindings = localBindings;
        this.globalBindings = globalBindings;
    }

    public static Environment root() {
        return new Environment(null, PersistentHashMap.empty(), new HashMap<>());
    }

    public Environment bind(String name, JaloValue value) {
        return new Environment(this, PersistentHashMap.<String, JaloValue>empty().assoc(name, value), globalBindings);
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
            JaloValue local = localBindings.get(name);
            return local;
        }
        if (parent != null) {
            return parent.lookup(name);
        }
        if (globalBindings.containsKey(name)) {
            return globalBindings.get(name);
        }
        throw new JaloEffectSignal(new JsonString("error"), new JsonString("Unbound variable: " + name));
    }
}
