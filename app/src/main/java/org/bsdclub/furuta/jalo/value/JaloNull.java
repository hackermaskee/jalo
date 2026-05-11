package org.bsdclub.furuta.jalo.value;

/** Represents null in jalo runtime values. */
public final class JaloNull implements JaloValue {
    /** Singleton instance. */
    public static final JaloNull INSTANCE = new JaloNull();

    private JaloNull() {
    }

    @Override public boolean equals(Object o) { return o instanceof JaloNull; }
    @Override public int hashCode() { return 0; }
    @Override public String toString() { return "null"; }
}
