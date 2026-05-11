package org.bsdclub.furuta.jalo.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JaloValueTest {
    @Test
    void a1_jsonNull_isSingleton() {
        assertThat(JaloNull.INSTANCE).isSameAs(JaloNull.INSTANCE);
    }

    @Test
    void a2_jsonNull_equalsItself() {
        assertThat(JaloNull.INSTANCE.equals(JaloNull.INSTANCE)).isTrue();
    }

    @Test
    void a3_jsonBool_valueAccessor() {
        assertThat(JaloBool.TRUE.value()).isTrue();
        assertThat(JaloBool.FALSE.value()).isFalse();
    }

    @Test
    void a4_jsonBool_equalsContract() {
        assertThat(JaloBool.TRUE).isEqualTo(JaloBool.TRUE);
        assertThat(JaloBool.TRUE).isNotEqualTo(JaloBool.FALSE);
    }

    @Test
    void a5_jsonNumber_preservesDoubleValue() {
        assertThat(new JaloNumber(42.0).value()).isEqualTo(42.0);
    }

    @Test
    void a6_jsonNumber_equalsSameValue() {
        assertThat(new JaloNumber(1.0)).isEqualTo(new JaloNumber(1.0));
    }

    @Test
    void a7_jsonNumber_nanEqualsNan() {
        assertThat(new JaloNumber(Double.NaN)).isEqualTo(new JaloNumber(Double.NaN));
    }

    @Test
    void a8_jsonString_preservesValueAndEquals() {
        assertThat(new JaloString("hello").value()).isEqualTo("hello");
        assertThat(new JaloString("hello")).isEqualTo(new JaloString("hello"));
    }

    @Test
    void a9_jsonString_rejectsNull() {
        assertThatThrownBy(() -> new JaloString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("JaloString value cannot be null");
    }

    @Test
    void a10_jsonArray_empty() {
        assertThat(JaloArray.empty().size()).isEqualTo(0);
    }

    @Test
    void a11_jsonArray_singleElement() {
        assertThat(JaloArray.of(new JaloNumber(1.0)).get(0)).isInstanceOf(JaloNumber.class);
    }

    @Test
    void a12_jsonArray_multipleElements() {
        assertThat(JaloArray.of(new JaloNumber(1.0), new JaloNumber(2.0), new JaloNumber(3.0)).size()).isEqualTo(3);
    }

    @Test
    void a13_jsonArray_nestedArrays() {
        JaloArray nested = JaloArray.of(
            JaloArray.of(new JaloNumber(1.0), new JaloNumber(2.0)),
            JaloArray.of(new JaloNumber(3.0), new JaloNumber(4.0))
        );
        assertThat(nested.size()).isEqualTo(2);
        assertThat(nested.get(0)).isInstanceOf(JaloArray.class);
        assertThat(((JaloArray) nested.get(0)).size()).isEqualTo(2);
    }

    @Test
    void a14_jsonArray_appendIsImmutable() {
        JaloArray original = JaloArray.of(new JaloNumber(1.0));
        JaloArray appended = original.append(new JaloNumber(2.0));

        assertThat(appended).isNotSameAs(original);
        assertThat(original.size()).isEqualTo(1);
        assertThat(appended.size()).isEqualTo(2);
    }

    @Test
    void a15_jsonObject_emptyPutSize() {
        JaloMap withEntry = JaloMap.empty().put("a", new JaloNumber(1.0));
        assertThat(withEntry.size()).isEqualTo(1);
        assertThat(withEntry.get("a")).isEqualTo(new JaloNumber(1.0));
    }

    @Test
    void a16_jsonObject_putIsImmutable() {
        JaloMap original = JaloMap.empty();
        JaloMap updated = original.put("a", new JaloNumber(1.0));

        assertThat(updated).isNotSameAs(original);
        assertThat(original.size()).isEqualTo(0);
        assertThat(updated.size()).isEqualTo(1);
    }
}
