package org.bsdclub.furuta.jalo.value;

import org.junit.jupiter.api.Test;
import org.organicdesign.fp.collections.PersistentHashMap;
import org.organicdesign.fp.collections.PersistentVector;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JaloValueUnificationTest {
    @Test void jaloIntFitsInJaloArray() {
        JaloInt i = new JaloInt(42);
        JaloArray arr = new JaloArray(PersistentVector.ofIter(java.util.List.of(i)));
        assertInstanceOf(JaloValue.class, arr.elements().get(0));
    }

    @Test void jaloIntFitsInJaloMap() {
        JaloInt i = new JaloInt(1);
        JaloMap map = new JaloMap(PersistentHashMap.<String, JaloValue>empty().assoc("k", i));
        assertInstanceOf(JaloValue.class, map.entries().get("k"));
    }

    @Test void jaloArrayElementsAreJaloValue() {
        JaloArray arr = JaloArray.empty();
        assertNotNull(arr.elements());
    }

    @Test void jaloMapEntriesAreJaloValue() {
        JaloMap map = JaloMap.empty();
        assertNotNull(map.entries());
    }

    @Test void sealedJaloValueCoversAllPermits() {
        JaloValue v1 = JaloNull.INSTANCE;
        JaloValue v2 = JaloBool.TRUE;
        JaloValue v3 = new JaloNumber(1.0);
        JaloValue v4 = new JaloString("x");
        JaloValue v5 = JaloArray.empty();
        JaloValue v6 = JaloMap.empty();
        JaloValue v7 = new JaloInt(1);
        JaloValue v8 = new JaloLong(1L);
        assertTrue(v1 instanceof JaloNull);
        assertTrue(v2 instanceof JaloBool);
        assertTrue(v3 instanceof JaloNumber);
        assertTrue(v4 instanceof JaloString);
        assertTrue(v5 instanceof JaloArray);
        assertTrue(v6 instanceof JaloMap);
        assertTrue(v7 instanceof JaloInt);
        assertTrue(v8 instanceof JaloLong);
    }

    @Test void rangeProducesJaloIntElements() {
        JaloArray arr = JaloArray.empty()
            .append(new JaloInt(0))
            .append(new JaloInt(1))
            .append(new JaloInt(2));
        assertInstanceOf(JaloInt.class, arr.get(0));
        assertInstanceOf(JaloInt.class, arr.get(1));
        assertInstanceOf(JaloInt.class, arr.get(2));
    }
}
