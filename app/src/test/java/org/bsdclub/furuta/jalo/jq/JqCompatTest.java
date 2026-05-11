package org.bsdclub.furuta.jalo.jq;

import static org.assertj.core.api.Assertions.assertThat;

import org.bsdclub.furuta.jalo.value.JaloArray;
import org.bsdclub.furuta.jalo.value.JaloInt;
import org.bsdclub.furuta.jalo.value.JaloMap;
import org.bsdclub.furuta.jalo.value.JaloString;
import org.junit.jupiter.api.Test;

class JqCompatTest {
    private final JqRuntime runtime = new JqRuntime();

    @Test
    void identityFilter() {
        assertThat(runtime.eval(".", new JaloInt(7))).isEqualTo(new JaloInt(7));
    }

    @Test
    void fieldFilter() {
        assertThat(runtime.eval(".foo", JaloMap.empty().put("foo", new JaloInt(42)))).isEqualTo(new JaloInt(42));
    }

    @Test
    void indexFilter() {
        assertThat(runtime.eval(".[0]", JaloArray.of(new JaloInt(11), new JaloInt(12)))).isEqualTo(new JaloInt(11));
    }

    @Test
    void lengthFilter() {
        assertThat(runtime.eval("length", JaloArray.of(new JaloInt(1), new JaloInt(2), new JaloInt(3))))
            .isEqualTo(new JaloInt(3));
    }

    @Test
    void atCsvFilter() {
        assertThat(runtime.eval("@csv", JaloArray.of(new JaloString("a"), new JaloString("b"))))
            .isEqualTo(new JaloString("a,b"));
    }

    @Test
    void ifThenElseEndFilter() {
        assertThat(runtime.eval("if . > 0 then \"positive\" else \"non-positive\" end", new JaloInt(1)))
            .isEqualTo(new JaloString("positive"));
        assertThat(runtime.eval("if . > 0 then \"positive\" else \"non-positive\" end", new JaloInt(0)))
            .isEqualTo(new JaloString("non-positive"));
    }

    @Test
    void asDestructuringFilter() {
        assertThat(runtime.eval(". as [$a, $b] | $a", JaloArray.of(new JaloInt(7), new JaloInt(9))))
            .isEqualTo(new JaloInt(7));
        assertThat(runtime.eval(". as {foo: $f} | $f", JaloMap.empty().put("foo", new JaloInt(21))))
            .isEqualTo(new JaloInt(21));
    }
}
