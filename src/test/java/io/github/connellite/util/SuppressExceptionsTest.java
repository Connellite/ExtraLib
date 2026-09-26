package io.github.connellite.util;

import io.github.connellite.jdbc.SqliteMemory;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void accept_closesConnectionWithMethodReference() throws Exception {
        Connection connection = SqliteMemory.open();
        assertFalse(connection.isClosed());
        assertDoesNotThrow(() -> SuppressExceptions.accept(Connection::close, connection));
        assertTrue(connection.isClosed());
    }

    @Test
    void run_closesConnectionWithBoundMethodReference() throws Exception {
        Connection connection = SqliteMemory.open();
        assertFalse(connection.isClosed());
        assertDoesNotThrow(() -> SuppressExceptions.run(connection::close));
        assertTrue(connection.isClosed());
    }

    @Test
    void call_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals("ok", SuppressExceptions.call(Checked::ok));
        assertNull(SuppressExceptions.call(Checked::failGet));
    }

    @Test
    void get_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals("ok", SuppressExceptions.get(Checked::ok));
        assertNull(SuppressExceptions.get(Checked::failGet));
    }

    @Test
    void apply_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals("x", SuppressExceptions.apply(Checked::identity, "x"));
        assertNull(SuppressExceptions.apply(Checked::failApply, "x"));
    }

    @Test
    void applyBi_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals("ab", SuppressExceptions.apply(Checked::concat, "a", "b"));
        assertNull(SuppressExceptions.apply(Checked::failApply2, "a", "b"));
    }

    @Test
    void accept_acceptsMethodReferenceThatThrowsChecked() {
        assertDoesNotThrow(() -> SuppressExceptions.accept(Checked::accept, "x"));
        assertDoesNotThrow(() -> SuppressExceptions.accept(Checked::failAccept, "x"));
    }

    @Test
    void acceptBi_acceptsMethodReferenceThatThrowsChecked() {
        assertDoesNotThrow(() -> SuppressExceptions.accept(Checked::accept2, "a", "b"));
        assertDoesNotThrow(() -> SuppressExceptions.accept(Checked::failAccept2, "a", "b"));
    }

    @Test
    void test_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals(Boolean.TRUE, SuppressExceptions.test(Checked::isNonEmpty, "a"));
        assertNull(SuppressExceptions.test(Checked::failTest, "a"));
    }

    @Test
    void testBi_acceptsMethodReferenceThatThrowsChecked() {
        assertEquals(Boolean.TRUE, SuppressExceptions.test(Checked::startsWith, "hello", "he"));
        assertNull(SuppressExceptions.test(Checked::failTest2, "a", "b"));
    }

    @Test
    void run_acceptsMethodReferenceThatThrowsChecked() {
        assertDoesNotThrow(() -> SuppressExceptions.run(Checked::okRun));
        assertDoesNotThrow(() -> SuppressExceptions.run(Checked::failRun));
    }

    private static final class Checked {
        static String ok() throws Exception {
            return "ok";
        }

        static String failGet() throws Exception {
            throw new Exception("get");
        }

        static String identity(String s) throws Exception {
            return s;
        }

        static String failApply(String s) throws Exception {
            throw new Exception("apply");
        }

        static String concat(String a, String b) throws Exception {
            return a + b;
        }

        static String failApply2(String a, String b) throws Exception {
            throw new Exception("apply2");
        }

        static void accept(String s) throws Exception {
        }

        static void failAccept(String s) throws Exception {
            throw new Exception("accept");
        }

        static void accept2(String a, String b) throws Exception {
        }

        static void failAccept2(String a, String b) throws Exception {
            throw new Exception("accept2");
        }

        static boolean isNonEmpty(String s) throws Exception {
            return !s.isEmpty();
        }

        static boolean failTest(String s) throws Exception {
            throw new Exception("test");
        }

        static boolean startsWith(String s, String prefix) throws Exception {
            return s.startsWith(prefix);
        }

        static boolean failTest2(String a, String b) throws Exception {
            throw new Exception("test2");
        }

        static void okRun() throws Exception {
        }

        static void failRun() throws Exception {
            throw new Exception("run");
        }
    }
}
