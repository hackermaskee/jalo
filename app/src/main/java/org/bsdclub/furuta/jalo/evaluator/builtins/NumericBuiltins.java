package org.bsdclub.furuta.jalo.evaluator.builtins;

import java.util.List;
import org.bsdclub.furuta.jalo.evaluator.JaloEffectSignal;
import org.bsdclub.furuta.jalo.evaluator.NumericPromotion;
import org.bsdclub.furuta.jalo.value.JaloBool;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloLong;
import org.bsdclub.furuta.jalo.value.JaloNumber;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.bsdclub.furuta.jalo.value.JaloValue;

/** Numeric utility built-ins. */
public final class NumericBuiltins {
    private NumericBuiltins() { }

    /**
     * Registers numeric helper built-ins.
     *
     * @param registry target registry
     */
    public static void registerAll(BuiltinRegistry registry) {
        registry.register("quot", (a, e) -> intBinary("quot", a, (x, y) -> x / y));
        registry.register("rem", (a, e) -> intBinary("rem", a, (x, y) -> x % y));
        registry.register("mod", (a, e) -> intBinary("mod", a, Math::floorMod));
        registry.register("floor", (a, e) -> unaryDouble("floor", a, Math::floor));
        registry.register("ceil", (a, e) -> unaryDouble("ceil", a, Math::ceil));
        registry.register("round", (a, e) -> unaryDouble("round", a, Math::rint));
        registry.register("trunc", (a, e) -> unaryDouble("trunc", a, x -> x < 0 ? Math.ceil(x) : Math.floor(x)));
        registry.register("abs", (a, e) -> abs(a));
        registry.register("max", (a, e) -> minmax(a, true));
        registry.register("min", (a, e) -> minmax(a, false));
        registry.register("pow", (a, e) -> binaryDouble("pow", a, Math::pow));
        registry.register("sqrt", (a, e) -> unaryDouble("sqrt", a, Math::sqrt));
        registry.register("log", (a, e) -> unaryDouble("log", a, Math::log));
        registry.register("exp", (a, e) -> unaryDouble("exp", a, Math::exp));
        registry.register("nan?", (a, e) -> bool("nan?", a, Double::isNaN));
        registry.register("infinite?", (a, e) -> bool("infinite?", a, Double::isInfinite));
        registry.register("pos?", (a, e) -> cmpBool("pos?", a, x -> x > 0));
        registry.register("neg?", (a, e) -> cmpBool("neg?", a, x -> x < 0));
        registry.register("zero?", (a, e) -> cmpBool("zero?", a, x -> x == 0));
        registry.register("double", (a, e) -> new JaloNumber(requireNum("double", a, 1, 0).asDouble()));
        registry.register("not=", (a, e) -> {
            requireArity("not=", a, 2);
            NumericPromotion.Numeric x = NumericPromotion.of(a.get(0));
            NumericPromotion.Numeric y = NumericPromotion.of(a.get(1));
            return x.asDouble() != y.asDouble() ? JaloBool.TRUE : JaloBool.FALSE;
        });
    }

    private static JaloValue abs(List<JaloValue> args) {
        requireArity("abs", args, 1);
        JaloValue v = args.get(0);
        if (v instanceof JaloInt n) return new JaloInt(Math.abs(n.value()));
        if (v instanceof JaloLong n) return new JaloLong(Math.abs(n.value()));
        NumericPromotion.Numeric n = NumericPromotion.of(v);
        return new JaloNumber(Math.abs(n.asDouble()));
    }

    private static JaloValue minmax(List<JaloValue> args, boolean max) {
        requireArity(max ? "max" : "min", args, 2);
        NumericPromotion.Numeric x = NumericPromotion.of(args.get(0));
        NumericPromotion.Numeric y = NumericPromotion.of(args.get(1));
        double raw = max ? Math.max(x.asDouble(), y.asDouble()) : Math.min(x.asDouble(), y.asDouble());
        return NumericPromotion.result(x, y, raw);
    }

    private static JaloValue intBinary(String name, List<JaloValue> args, LongOp op) {
        requireArity(name, args, 2);
        NumericPromotion.Numeric x = requireIntNum(name, args.get(0));
        NumericPromotion.Numeric y = requireIntNum(name, args.get(1));
        long rhs = y.asLong();
        if (rhs == 0L) throw error("Division by zero");
        long raw = op.apply(x.asLong(), rhs);
        if (x.isLong() || y.isLong()) return new JaloLong(raw);
        return new JaloInt((int) raw);
    }

    private static JaloValue unaryDouble(String name, List<JaloValue> args, DoubleOp op) {
        requireArity(name, args, 1);
        NumericPromotion.Numeric x = requireNum(name, args, 1, 0);
        return new JaloNumber(op.apply(x.asDouble()));
    }

    private static JaloValue binaryDouble(String name, List<JaloValue> args, DoubleBiOp op) {
        requireArity(name, args, 2);
        NumericPromotion.Numeric x = NumericPromotion.of(args.get(0));
        NumericPromotion.Numeric y = NumericPromotion.of(args.get(1));
        return new JaloNumber(op.apply(x.asDouble(), y.asDouble()));
    }

    private static JaloValue bool(String name, List<JaloValue> args, DoublePred pred) {
        requireArity(name, args, 1);
        return pred.test(requireNum(name, args, 1, 0).asDouble()) ? JaloBool.TRUE : JaloBool.FALSE;
    }

    private static JaloValue cmpBool(String name, List<JaloValue> args, DoublePred pred) {
        requireArity(name, args, 1);
        return pred.test(requireNum(name, args, 1, 0).asDouble()) ? JaloBool.TRUE : JaloBool.FALSE;
    }

    private static NumericPromotion.Numeric requireNum(String name, List<JaloValue> args, int arity, int idx) {
        requireArity(name, args, arity);
        return NumericPromotion.of(args.get(idx));
    }

    private static NumericPromotion.Numeric requireIntNum(String name, JaloValue value) {
        NumericPromotion.Numeric n = NumericPromotion.of(value);
        if (n.isDouble()) throw error(name + ": expected int/long");
        return n;
    }

    private static void requireArity(String name, List<JaloValue> args, int arity) {
        if (args.size() != arity) throw error("Wrong arity for " + name);
    }

    private static JaloEffectSignal error(String msg) {
        return new JaloEffectSignal(new JaloString("error"), new JaloString(msg));
    }

    @FunctionalInterface
    private interface LongOp { long apply(long x, long y); }

    @FunctionalInterface
    private interface DoubleOp { double apply(double x); }

    @FunctionalInterface
    private interface DoubleBiOp { double apply(double x, double y); }

    @FunctionalInterface
    private interface DoublePred { boolean test(double x); }
}
