package org.bsdclub.furuta.jalo.evaluator;

import org.bsdclub.furuta.jalo.value.JaloValue;

/**
 * Runtime signal used to propagate algebraic effects and interpreter errors.
 *
 * <p>Layer: Evaluation (effect channel, per DESIGN.md §1).
 */
public final class JaloEffectSignal extends RuntimeException {
    private final JaloValue tag;
    private final JaloValue value;

    /**
     * Creates an effect signal.
     *
     * @param tag effect tag
     * @param value effect payload
     */
    public JaloEffectSignal(JaloValue tag, JaloValue value) {
        super(tag + ": " + value);
        this.tag = tag;
        this.value = value;
    }

    /**
     * Returns the effect tag.
     *
     * @return tag value
     */
    public JaloValue tag() {
        return tag;
    }

    /**
     * Returns the effect payload.
     *
     * @return payload value
     */
    public JaloValue value() {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote Stack trace generation is suppressed to reduce JVM exception cost by
     *           ~10x (see DESIGN.md §2.5, §2.11).
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
