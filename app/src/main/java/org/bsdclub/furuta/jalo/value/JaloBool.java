package org.bsdclub.furuta.jalo.value;

/** Represents boolean values in jalo runtime. */
public enum JaloBool implements JaloValue {
    TRUE(true),
    FALSE(false);

    private final boolean value;

    JaloBool(boolean value) { this.value = value; }

    /**
     * Returns primitive boolean payload.
     *
     * @return the primitive boolean value
     */
    public boolean value() { return value; }

    @Override public String toString() { return value ? "#true" : "#false"; }
}
