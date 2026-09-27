package io.github.connellite.reflection;

import io.github.connellite.function.SerializableFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LambdaPropertyUtilTest {

    static class Bean {
        public String getName() {
            return "x";
        }

        public boolean isActive() {
            return true;
        }
    }

    @Test
    void methodToProperty() {
        assertEquals("name", LambdaPropertyUtil.methodToProperty("getName"));
        assertEquals("active", LambdaPropertyUtil.methodToProperty("isActive"));
        assertEquals("name", LambdaPropertyUtil.methodToProperty("setName"));
        assertEquals("URL", LambdaPropertyUtil.methodToProperty("getURL"));
        assertThrows(IllegalArgumentException.class, () -> LambdaPropertyUtil.methodToProperty("name"));
    }

    @Test
    void serializedLambda_and_propertyName() {
        SerializableFunction<Bean, String> getter = Bean::getName;
        assertEquals("getName", LambdaPropertyUtil.serializedLambda(getter).getImplMethodName());
        assertEquals("name", LambdaPropertyUtil.propertyName(getter));
        assertEquals("active", LambdaPropertyUtil.propertyName((SerializableFunction<Bean, Boolean>) Bean::isActive));
    }
}
