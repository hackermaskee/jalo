package org.bsdclub.furuta.jalo.evaluator;

import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** Numeric promotion helper shared by evaluator and numeric built-ins. */
public final class NumericPromotion {
    private NumericPromotion() { }

    /**
     * Promoted numeric representation.
     *
     * @param asDouble numeric value as double
     * @param isDouble true when the origin type is double
     * @param isLong true when the origin integral type is long
     */
    public record Numeric(double asDouble, boolean isDouble, boolean isLong) {
        public boolean integral() { return !isDouble; }
        public long asLong() { return (long) asDouble; }
    }

    /**
     * Converts jalo numeric value to promoted representation.
     *
     * @param value numeric jalo value
     * @return promoted numeric record
     * @throws JaloEffectSignal when value is not numeric
     */
    public static Numeric of(JaloValue value) {
        if (value instanceof JaloNumber n) return new Numeric(n.value(), true, false);
        if (value instanceof JaloInt n) return new Numeric(n.value(), false, false);
        if (value instanceof JaloLong n) return new Numeric(n.value(), false, true);
        throw new JaloEffectSignal(new JaloString("error"), new JaloString("Expected number"));
    }

    /**
     * Returns numeric result with promotion rules compatible with evaluator arithmetic.
     *
     * @param left left operand promotion metadata
     * @param right right operand promotion metadata
     * @param raw computed double value
     * @return promoted jalo numeric value
     */
    public static JaloValue result(Numeric left, Numeric right, double raw) {
        if (!left.isDouble() && !right.isDouble()) {
            if (left.isLong() || right.isLong()) return new JaloLong((long) raw);
            return new JaloInt((int) raw);
        }
        return new JaloNumber(raw);
    }

    /**
     * Division preserving integer/float behavior and integer divide-by-zero check.
     *
     * @param l left operand
     * @param r right operand
     * @return division result with promotion rules
     * @throws JaloEffectSignal when dividing integral numbers by zero
     */
    public static JaloValue divide(JaloValue l, JaloValue r) {
        Numeric left = of(l);
        Numeric right = of(r);
        if (right.asDouble() == 0.0 && left.integral() && right.integral()) {
            throw new JaloEffectSignal(new JaloString("error"), new JaloString("Division by zero"));
        }
        return result(left, right, left.asDouble() / right.asDouble());
    }
}
