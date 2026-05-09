package org.bsdclub.furuta.jalo.evaluator;

public final class JaloEffectSignal extends RuntimeException {
    private final String tag;
    private final String value;

    public JaloEffectSignal(String tag, String value) {
        super(tag + ": " + value);
        this.tag = tag;
        this.value = value;
    }

    public String tag() {
        return tag;
    }

    public String value() {
        return value;
    }
}
