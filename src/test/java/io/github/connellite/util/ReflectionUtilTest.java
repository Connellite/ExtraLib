package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionUtilTest {

    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.METHOD)
    @interface Marker {
    }

    enum Color {
        RED(1), GREEN(2);
        private final int code;

        Color(int code) {
            this.code = code;
        }

        int getCode() {
            return code;
        }
    }

    private enum DupKey {
        A, B;

        int k() {
            return 1;
        }
    }

    static class Base {
        @SuppressWarnings("unused")
        protected int inherited = 7;
    }

    @SuppressWarnings("unused")
    static class Fixture extends Base {
        private static String STAT = "s";
        private int inst = 3;

        private static int staticM() {
            return 99;
        }

        private int instM() {
            return inst;
        }

        @Marker
        private void marked() {
        }

        @Marker
        private void marked2() {
        }

        Fixture() {
        }

        Fixture(int v) {
            this.inst = v;
        }
    }

    @SuppressWarnings("unused")
    static class GenericFixture {
        private List<Integer> ints;
        private Map<Integer, String> indexToName;
        private List<Map<Long, Double>> nested;
        private String plain;
    }

    record RecFixture(int id, String name) {
    }

    @Test
    void invokeStatic_and_invoke_instance() throws Exception {
        assertEquals(99, ReflectionUtil.invokeStatic(Fixture.class, "staticM"));
        Fixture t = new Fixture();
        assertEquals(3, ReflectionUtil.invoke(t, "instM"));
    }

    @Test
    void getStatic_setStatic_get_set_instance_getSuper() throws Exception {
        assertEquals("s", ReflectionUtil.getStatic(Fixture.class, "STAT", String.class));
        ReflectionUtil.setStatic(Fixture.class, "STAT", "t");
        assertEquals("t", ReflectionUtil.getStatic(Fixture.class, "STAT", String.class));
        ReflectionUtil.setStatic(Fixture.class, "STAT", "s");

        Fixture t = new Fixture();
        assertEquals(3, ReflectionUtil.get(t, "inst", int.class));
        ReflectionUtil.set(t, "inst", 5);
        assertEquals(5, ReflectionUtil.get(t, "inst", int.class));
        assertEquals(7, ReflectionUtil.getSuper(t, "inherited", int.class));
    }

    @Test
    void get_withDeclaringClass() throws Exception {
        Fixture t = new Fixture();
        assertEquals(7, ReflectionUtil.get(t, Base.class, "inherited", int.class));
    }

    @Test
    void getValueField_setValueField() throws Exception {
        Fixture t = new Fixture();
        Field f = Fixture.class.getDeclaredField("inst");
        assertEquals(3, ReflectionUtil.getValueField(t, f));
        ReflectionUtil.setValueField(t, f, 8);
        assertEquals(8, ReflectionUtil.getValueField(t, f));
    }

    @Test
    void setValueField_finalThrows() throws Exception {
        class Holds {
            @SuppressWarnings("unused")
            private final int x = 1;
        }
        Holds h = new Holds();
        Field f = Holds.class.getDeclaredField("x");
        assertThrows(IllegalStateException.class, () -> ReflectionUtil.setValueField(h, f, 2));
    }

    @Test
    void getMethodByName_and_byAnnotation() {
        assertNotNull(ReflectionUtil.getMethodByName(Fixture.class, "instM"));
        assertNull(ReflectionUtil.getMethodByName(Fixture.class, "noSuchMethod"));
        assertNotNull(ReflectionUtil.getMethodByAnnotation(Fixture.class, Marker.class));
        List<?> list = ReflectionUtil.getAllMethodsByAnnotation(Fixture.class, Marker.class);
        assertEquals(2, list.size());
    }

    @Test
    void getConstructor_and_getInstance() throws Exception {
        Fixture a = ReflectionUtil.getInstance(Fixture.class);
        assertEquals(3, ReflectionUtil.get(a, "inst", int.class));
        Fixture b = ReflectionUtil.getInstance(Fixture.class, 42);
        assertEquals(42, ReflectionUtil.get(b, "inst", int.class));
        Fixture c = ReflectionUtil.getInstance(Fixture.class, new Class<?>[]{int.class}, 11);
        assertEquals(11, ReflectionUtil.get(c, "inst", int.class));
    }

    @Test
    void resolveRecordConstructor_forRecordAndNonRecord() {
        assertEquals(
                List.of(int.class, String.class),
                List.of(ReflectionUtil.resolveRecordConstructor(RecFixture.class).getParameterTypes())
        );
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.resolveRecordConstructor(Fixture.class));
    }

    @Test
    void getInstance_nullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtil.getInstance(Fixture.class, new Object[]{null}));
    }

    @Test
    void getAllInterfaces() {
        class Local implements Runnable, java.io.Serializable {
            @Override
            public void run() {
            }
        }
        List<Class<?>> ifaces = ReflectionUtil.getAllInterfaces(Local.class);
        assertTrue(ifaces.contains(Runnable.class));
        assertTrue(ifaces.contains(java.io.Serializable.class));
    }

    @Test
    void isInnerClass() {
        assertFalse(ReflectionUtil.isInnerClass(String.class));
        assertFalse(ReflectionUtil.isInnerClass(null));
        assertTrue(ReflectionUtil.isInnerClass(Fixture.class));
    }

    @Test
    void getEnumMap_byName_and_byKey() {
        Map<String, Color> byName = ReflectionUtil.getEnumMap(Color.class);
        assertEquals(Color.RED, byName.get("RED"));
        Map<Integer, Color> byCode = ReflectionUtil.getEnumMap(Color.class, Color::getCode);
        assertEquals(Color.GREEN, byCode.get(2));
    }

    @Test
    void getEnumMap_duplicateKeyThrows() {
        assertThrows(IllegalStateException.class, () -> ReflectionUtil.getEnumMap(DupKey.class, DupKey::k));
    }

    @Test
    void primitiveToWrapper_mapsPrimitivesAndKeepsOthers() {
        assertEquals(Boolean.class, ReflectionUtil.primitiveToWrapper(boolean.class));
        assertEquals(Byte.class, ReflectionUtil.primitiveToWrapper(byte.class));
        assertEquals(Short.class, ReflectionUtil.primitiveToWrapper(short.class));
        assertEquals(Integer.class, ReflectionUtil.primitiveToWrapper(int.class));
        assertEquals(Long.class, ReflectionUtil.primitiveToWrapper(long.class));
        assertEquals(Float.class, ReflectionUtil.primitiveToWrapper(float.class));
        assertEquals(Double.class, ReflectionUtil.primitiveToWrapper(double.class));
        assertEquals(Character.class, ReflectionUtil.primitiveToWrapper(char.class));
        assertEquals(Void.class, ReflectionUtil.primitiveToWrapper(void.class));
        assertEquals(String.class, ReflectionUtil.primitiveToWrapper(String.class));
    }

    @Test
    void castFieldValue_handlesPrimitiveTokenNullAndInvalidCast() {
        assertEquals(Integer.valueOf(7), ReflectionUtil.castFieldValue(int.class, 7));
        assertEquals("x", ReflectionUtil.castFieldValue(String.class, "x"));
        assertNull(ReflectionUtil.castFieldValue(Integer.class, null));
        assertThrows(ClassCastException.class, () -> ReflectionUtil.castFieldValue(Integer.class, "7"));
    }

    @Test
    void getAllGenericParameterClasses_forList() throws Exception {
        Field field = GenericFixture.class.getDeclaredField("ints");
        List<Class<?>> classes = ReflectionUtil.getAllGenericParameterClasses(field.getGenericType());
        assertEquals(List.of(Integer.class), classes);
    }

    @Test
    void getAllGenericParameterClasses_forMap() throws Exception {
        Field field = GenericFixture.class.getDeclaredField("indexToName");
        List<Class<?>> classes = ReflectionUtil.getAllGenericParameterClasses(field);
        assertEquals(List.of(Integer.class, String.class), classes);
    }

    @Test
    void getAllGenericParameterClasses_forNestedAndNonGeneric() throws Exception {
        Field nested = GenericFixture.class.getDeclaredField("nested");
        List<Class<?>> nestedClasses = ReflectionUtil.getAllGenericParameterClasses(nested.getGenericType());
        assertEquals(List.of(Map.class, Long.class, Double.class), nestedClasses);

        Field plain = GenericFixture.class.getDeclaredField("plain");
        assertTrue(ReflectionUtil.getAllGenericParameterClasses(plain.getGenericType()).isEmpty());
    }
}
