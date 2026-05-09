package org.bsdclub.furuta.jalo.evaluator;

import org.bsdclub.furuta.jalo.value.JaloValue;

public final class JaloEffectSignal extends RuntimeException {
    private final JaloValue tag;
    private final JaloValue value;

    public JaloEffectSignal(JaloValue tag, JaloValue value) {
        super(tag + ": " + value);
        this.tag = tag;
        this.value = value;
    }

    public JaloValue tag() {
        return tag;
    }

    public JaloValue value() {
        return value;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
