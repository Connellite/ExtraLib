package io.github.connellite.reflection;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodHandleReflectionUtilTest {

    static class Fixture {
        private static String STAT = "s";
        private int inst = 3;

        private static int staticM() {
            return 99;
        }

        private int instM() {
            return inst;
        }

        Fixture() {
        }

        Fixture(int v) {
            this.inst = v;
        }
    }

    static class Base {
        @SuppressWarnings("unused")
        protected int inherited = 7;
    }

    static class Child extends Base {
    }

    record RecFixture(int id, String name) {
    }

    @Test
    void invokeStatic_and_invoke_instance() throws Exception {
        MethodHandle staticM = MethodHandleReflectionUtil.methodHandle(Fixture.class, "staticM");
        assertEquals(99, MethodHandleReflectionUtil.invoke(staticM));

        Fixture t = new Fixture();
        MethodHandle instM = MethodHandleReflectionUtil.methodHandle(Fixture.class, "instM");
        assertEquals(3, MethodHandleReflectionUtil.invoke(instM, t));
    }

    @Test
    void asType_allowsInvokeExactAtCallSite() throws Throwable {
        MethodHandle instM = MethodHandleReflectionUtil.methodHandle(Fixture.class, "instM");
        MethodHandle typed = MethodHandleReflectionUtil.asType(instM, MethodType.methodType(int.class, Fixture.class));
        Fixture t = new Fixture();
        assertEquals(3, (int) typed.invokeExact(t));
    }

    @Test
    void get_set_static_and_instance_fields() throws Exception {
        Field statField = Fixture.class.getDeclaredField("STAT");
        VarHandle stat = MethodHandleReflectionUtil.varHandle(statField);
        assertEquals("s", MethodHandleReflectionUtil.get(stat, null, String.class));
        MethodHandleReflectionUtil.set(stat, statField, null, "t");
        assertEquals("t", MethodHandleReflectionUtil.get(stat, null, String.class));
        MethodHandleReflectionUtil.set(stat, statField, null, "s");
    }

    @Test
    void get_set_instance_and_super_fields() throws Exception {
        Field instField = Fixture.class.getDeclaredField("inst");
        VarHandle inst = MethodHandleReflectionUtil.varHandle(instField);
        Fixture t = new Fixture();
        assertEquals(3, MethodHandleReflectionUtil.get(inst, t, int.class));
        MethodHandleReflectionUtil.set(inst, instField, t, 5);
        assertEquals(5, MethodHandleReflectionUtil.get(inst, t, int.class));

        Child child = new Child();
        VarHandle inherited = MethodHandleReflectionUtil.varHandle(Base.class, "inherited");
        assertEquals(7, MethodHandleReflectionUtil.get(inherited, child, int.class));
    }

    @Test
    void getValueField_setValueField() throws Exception {
        Fixture t = new Fixture();
        Field field = Fixture.class.getDeclaredField("inst");
        VarHandle inst = MethodHandleReflectionUtil.varHandle(field);
        assertEquals(3, MethodHandleReflectionUtil.get(inst, t));
        MethodHandleReflectionUtil.set(inst, field, t, 8);
        assertEquals(8, MethodHandleReflectionUtil.get(inst, t));
    }

    @Test
    void set_finalThrows() throws Exception {
        class Holds {
            @SuppressWarnings("unused")
            private final int x = 1;
        }
        Holds h = new Holds();
        Field field = Holds.class.getDeclaredField("x");
        VarHandle x = MethodHandleReflectionUtil.varHandle(field);
        assertThrows(IllegalStateException.class, () -> MethodHandleReflectionUtil.set(x, field, h, 2));
    }

    @Test
    void constructorHandle_and_getInstance() throws Exception {
        MethodHandle noArg = MethodHandleReflectionUtil.constructorHandle(Fixture.class);
        Fixture a = MethodHandleReflectionUtil.getInstance(noArg);
        VarHandle inst = MethodHandleReflectionUtil.varHandle(Fixture.class, "inst");
        assertEquals(3, MethodHandleReflectionUtil.get(inst, a, int.class));

        MethodHandle intArg = MethodHandleReflectionUtil.constructorHandle(Fixture.class, int.class);
        Fixture b = MethodHandleReflectionUtil.getInstance(intArg, 42);
        assertEquals(42, MethodHandleReflectionUtil.get(inst, b, int.class));
    }

    @Test
    void resolveRecordConstructorHandle_forRecordAndNonRecord() throws Exception {
        assertNotNull(MethodHandleReflectionUtil.resolveRecordConstructorHandle(RecFixture.class));
        assertEquals(
                List.of(int.class, String.class),
                List.of(ReflectionUtil.resolveRecordConstructor(RecFixture.class).getParameterTypes())
        );
        assertThrows(IllegalArgumentException.class, () -> MethodHandleReflectionUtil.resolveRecordConstructorHandle(Fixture.class));
    }
}
