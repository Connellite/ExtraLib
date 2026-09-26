package io.github.connellite.collections.function;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UncheckedFunctionInterfacesTest {

    @Test
    void consumer_and_biConsumer() {
        UncheckedConsumer<String> consumer = s -> assertEquals("a", s);
        consumer.accept("a");
        IOException io = new IOException("c");
        UncheckedConsumer<String> throwing = s -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwing.accept("x")));

        UncheckedBiConsumer<String, Integer> bi = (s, n) -> assertEquals(2, s.length() + n);
        bi.accept("x", 1);
        UncheckedBiConsumer<String, Integer> throwingBi = (s, n) -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingBi.accept("x", 1)));
    }

    @Test
    void function_operator_and_supplier() {
        UncheckedFunction<Integer, String> fn = i -> String.valueOf(i);
        assertEquals("7", fn.apply(7));
        IOException io = new IOException("f");
        UncheckedFunction<Integer, String> throwingFn = i -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingFn.apply(1)));

        UncheckedUnaryOperator<String> unary = String::toUpperCase;
        assertEquals("HI", unary.apply("hi"));
        UncheckedUnaryOperator<String> throwingUnary = s -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingUnary.apply("x")));

        UncheckedBiFunction<Integer, Integer, Integer> biFn = Integer::sum;
        assertEquals(5, biFn.apply(2, 3));
        UncheckedBiFunction<Integer, Integer, Integer> throwingBiFn = (a, b) -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingBiFn.apply(1, 2)));

        UncheckedBinaryOperator<Integer> bin = Integer::sum;
        assertEquals(9, bin.apply(4, 5));
        UncheckedBinaryOperator<Integer> throwingBin = (a, b) -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingBin.apply(1, 2)));

        UncheckedSupplier<String> supplier = () -> "ok";
        assertEquals("ok", supplier.get());
        UncheckedSupplier<String> throwingSupplier = () -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingSupplier.get()));
    }

    @Test
    void predicates_and_runnable() {
        UncheckedPredicate<String> pred = s -> s.length() > 1;
        assertTrue(pred.test("ab"));
        assertFalse(pred.test("a"));
        IOException io = new IOException("p");
        UncheckedPredicate<String> throwingPred = s -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingPred.test("x")));

        UncheckedBiPredicate<String, String> biPred = String::startsWith;
        assertTrue(biPred.test("hello", "he"));
        UncheckedBiPredicate<String, String> throwingBiPred = (a, b) -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingBiPred.test("a", "b")));

        UncheckedRunnable runnable = () -> {
        };
        runnable.run();
        UncheckedRunnable throwingRunnable = () -> {
            throw io;
        };
        assertSame(io, assertThrows(IOException.class, () -> throwingRunnable.run()));
    }
}
