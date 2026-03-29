package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class SuppressExceptionsTest {

    @Test
    void runSwallowsException() {
        assertDoesNotThrow(() -> SuppressExceptions.run(() -> {
            throw new IllegalStateException("x");
        }));
    }

    @Test
    void callReturnsNullOnFailure() {
        assertNull(SuppressExceptions.call((Callable<String>) () -> {
            throw new Exception("x");
        }));
    }

    @Test
    void getReturnsValue() {
        assertEquals("ok", SuppressExceptions.get(() -> "ok"));
    }

    @Test
    void testPredicateNullOnThrow() {
        assertNull(SuppressExceptions.test(s -> {
            throw new RuntimeException();
        }, "a"));
    }
}
