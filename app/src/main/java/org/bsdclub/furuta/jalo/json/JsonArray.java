package org.bsdclub.furuta.jalo.json;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.organicdesign.fp.collections.PersistentVector;

/**
 * Represents an immutable JSON array.
 *
 * <p>Layer: JSON Model (per DESIGN.md §1 architecture table).
 * Provides array operations through persistent vector storage without in-place mutation.
 *
 * @see JsonValue
 */
public final class JsonArray implements JsonValue {
    private final PersistentVector<JsonValue> elements;

    /**
     * Creates a JSON array from the provided persistent vector.
     *
     * @param elements the persistent vector of JSON values
     */
    public JsonArray(PersistentVector<JsonValue> elements) {
        this.elements = elements;
    }

    /**
     * Returns an empty JSON array.
     *
     * @return a JSON array with zero elements
     */
    public static JsonArray empty() {
        return new JsonArray(PersistentVector.empty());
    }

    /**
     * Returns a new JSON array containing the provided values.
     *
     * @param values the array elements to include
     * @return a JSON array containing the given values in order
     */
    public static JsonArray of(JsonValue... values) {
        return new JsonArray(PersistentVector.ofIter(Arrays.asList(values)));
    }

    /**
     * Returns the underlying persistent vector of elements.
     *
     * @return the persistent vector storing this array's elements
     */
    public PersistentVector<JsonValue> elements() {
        return elements;
    }

    /**
     * Returns the number of elements in this array.
     *
     * @return the array size
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns the element at the specified index.
     *
     * @param idx the zero-based index to access
     * @return the JSON value stored at the given index
     */
    public JsonValue get(int idx) {
        return elements.get(idx);
    }

    /**
     * Returns a new array with the given value appended.
     *
     * @param v the value to append
     * @return a new JSON array with the appended value
     */
    public JsonArray append(JsonValue v) {
        return new JsonArray(elements.append(v));
    }

    /**
     * Returns true when the other object is a JSON array with equal elements.
     *
     * @param o the object to compare
     * @return true if the compared array has equal persistent vector contents
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof JsonArray a && elements.equals(a.elements);
    }

    /**
     * Returns the hash code of this array based on its elements.
     *
     * @return the hash code delegated from the underlying persistent vector
     */
    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    /**
     * Returns the JSON text form of this array.
     *
     * @return the bracketed list representation of the array elements
     */
    @Override
    public String toString() {
        return "[" + elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }
}
