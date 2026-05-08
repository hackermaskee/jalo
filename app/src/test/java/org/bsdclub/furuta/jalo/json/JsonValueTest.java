package org.bsdclub.furuta.jalo.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonValueTest {
    @Test
    void a1_jsonNull_isSingleton() {
        assertThat(JsonNull.INSTANCE).isSameAs(JsonNull.INSTANCE);
    }

    @Test
    void a2_jsonNull_equalsItself() {
        assertThat(JsonNull.INSTANCE.equals(JsonNull.INSTANCE)).isTrue();
    }

    @Test
    void a3_jsonBool_valueAccessor() {
        assertThat(JsonBool.TRUE.value()).isTrue();
        assertThat(JsonBool.FALSE.value()).isFalse();
    }

    @Test
    void a4_jsonBool_equalsContract() {
        assertThat(JsonBool.TRUE).isEqualTo(JsonBool.TRUE);
        assertThat(JsonBool.TRUE).isNotEqualTo(JsonBool.FALSE);
    }

    @Test
    void a5_jsonNumber_preservesDoubleValue() {
        assertThat(new JsonNumber(42.0).value()).isEqualTo(42.0);
    }

    @Test
    void a6_jsonNumber_equalsSameValue() {
        assertThat(new JsonNumber(1.0)).isEqualTo(new JsonNumber(1.0));
    }

    @Test
    void a7_jsonNumber_nanEqualsNan() {
        assertThat(new JsonNumber(Double.NaN)).isEqualTo(new JsonNumber(Double.NaN));
    }

    @Test
    void a8_jsonString_preservesValueAndEquals() {
        assertThat(new JsonString("hello").value()).isEqualTo("hello");
        assertThat(new JsonString("hello")).isEqualTo(new JsonString("hello"));
    }

    @Test
    void a9_jsonString_rejectsNull() {
        assertThatThrownBy(() -> new JsonString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("JsonString value cannot be null");
    }

    @Test
    void a10_jsonArray_empty() {
        assertThat(JsonArray.empty().size()).isEqualTo(0);
    }

    @Test
    void a11_jsonArray_singleElement() {
        assertThat(JsonArray.of(new JsonNumber(1.0)).get(0)).isInstanceOf(JsonNumber.class);
    }

    @Test
    void a12_jsonArray_multipleElements() {
        assertThat(JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0), new JsonNumber(3.0)).size()).isEqualTo(3);
    }

    @Test
    void a13_jsonArray_nestedArrays() {
        JsonArray nested = JsonArray.of(
            JsonArray.of(new JsonNumber(1.0), new JsonNumber(2.0)),
            JsonArray.of(new JsonNumber(3.0), new JsonNumber(4.0))
        );
        assertThat(nested.size()).isEqualTo(2);
        assertThat(nested.get(0)).isInstanceOf(JsonArray.class);
        assertThat(((JsonArray) nested.get(0)).size()).isEqualTo(2);
    }

    @Test
    void a14_jsonArray_appendIsImmutable() {
        JsonArray original = JsonArray.of(new JsonNumber(1.0));
        JsonArray appended = original.append(new JsonNumber(2.0));

        assertThat(appended).isNotSameAs(original);
        assertThat(original.size()).isEqualTo(1);
        assertThat(appended.size()).isEqualTo(2);
    }

    @Test
    void a15_jsonObject_emptyPutSize() {
        JsonObject withEntry = JsonObject.empty().put("a", new JsonNumber(1.0));
        assertThat(withEntry.size()).isEqualTo(1);
        assertThat(withEntry.get("a")).isEqualTo(new JsonNumber(1.0));
    }

    @Test
    void a16_jsonObject_putIsImmutable() {
        JsonObject original = JsonObject.empty();
        JsonObject updated = original.put("a", new JsonNumber(1.0));

        assertThat(updated).isNotSameAs(original);
        assertThat(original.size()).isEqualTo(0);
        assertThat(updated.size()).isEqualTo(1);
    }
}
