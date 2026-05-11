package org.bsdclub.furuta.jalo.value;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentVector;

/** Immutable array of jalo values. */
public final class JaloArray implements JaloValue {
    private final PersistentVector<JaloValue> elements;

    /**
     * Creates an array from a persistent vector.
     *
     * @param elements persistent vector of jalo values
     */
    public JaloArray(PersistentVector<JaloValue> elements) { this.elements = elements; }

    /**
     * Returns an empty array.
     *
     * @return empty JaloArray
     */
    public static JaloArray empty() { return new JaloArray(PersistentVector.empty()); }

    /**
     * Returns an array from varargs values.
     *
     * @param values jalo values to include
     * @return JaloArray containing the given values
     */
    public static JaloArray of(JaloValue... values) {
        return new JaloArray(PersistentVector.ofIter(Arrays.asList(values)));
    }

    /**
     * Returns underlying elements.
     *
     * @return persistent vector of elements
     */
    public PersistentVector<JaloValue> elements() { return elements; }
    public int size() { return elements.size(); }
    public JaloValue get(int idx) { return elements.get(idx); }

    /**
     * Returns new array with appended value.
     *
     * @param v value to append
     * @return new JaloArray with v appended
     */
    public JaloArray append(JaloValue v) { return new JaloArray(elements.append(v)); }

    @Override public boolean equals(Object o) {
        return o instanceof JaloArray a && elements.equals(a.elements);
    }

    @Override public int hashCode() { return elements.hashCode(); }

    @Override public String toString() {
        return "[" + elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }
}
