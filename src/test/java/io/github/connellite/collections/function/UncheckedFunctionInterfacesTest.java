package io.github.connellite.collections.function;

import io.github.connellite.function.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Test
    void of_adapts_throwing_method_references() throws IOException {
        assertEquals(List.of("a", "b"), List.of("a", "b").stream()
                .map(UncheckedFunction.of(UncheckedFunctionInterfacesTest::read))
                .toList());
        assertSame(MISSING, assertThrows(IOException.class, () -> List.of("missing").stream()
                .map(UncheckedFunction.of(UncheckedFunctionInterfacesTest::read))
                .toList()));

        assertEquals(List.of("A"), List.of("a").stream()
                .map(UncheckedUnaryOperator.of(UncheckedFunctionInterfacesTest::upper))
                .toList());
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedUnaryOperator.of(UncheckedFunctionInterfacesTest::upper).apply("missing")));

        assertEquals(5, UncheckedBiFunction.of(UncheckedFunctionInterfacesTest::add).apply(2, 3));
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedBiFunction.of(UncheckedFunctionInterfacesTest::add).apply(-1, 1)));

        assertEquals(6, List.of(1, 2, 3).stream()
                .reduce(UncheckedBinaryOperator.of(UncheckedFunctionInterfacesTest::add))
                .orElseThrow());
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedBinaryOperator.of(UncheckedFunctionInterfacesTest::add).apply(-1, 1)));

        List.of("a", "b").forEach(UncheckedConsumer.of(this::consume));
        assertEquals(List.of("a", "b"), consumed);
        assertSame(MISSING, assertThrows(IOException.class, () ->
                List.of("missing").forEach(UncheckedConsumer.of(this::consume))));

        Map.of("ab", 1).forEach(UncheckedBiConsumer.of(this::consumeBoth));
        assertEquals(List.of("ab1"), consumedPairs);
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedBiConsumer.of(this::consumeBoth).accept("ab", -1)));

        assertEquals(List.of("ab"), List.of("a", "ab").stream()
                .filter(UncheckedPredicate.of(UncheckedFunctionInterfacesTest::keep))
                .toList());
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedPredicate.of(UncheckedFunctionInterfacesTest::keep).test("missing")));

        assertTrue(UncheckedBiPredicate.of(UncheckedFunctionInterfacesTest::bothNonEmpty).test("a", "b"));
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedBiPredicate.of(UncheckedFunctionInterfacesTest::bothNonEmpty).test("", "b")));

        assertEquals("ok", UncheckedSupplier.of(UncheckedFunctionInterfacesTest::supply).get());
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedSupplier.of(UncheckedFunctionInterfacesTest::supplyMissing).get()));

        UncheckedRunnable.of(UncheckedFunctionInterfacesTest::runOk).run();
        assertSame(MISSING, assertThrows(IOException.class, () ->
                UncheckedRunnable.of(UncheckedFunctionInterfacesTest::runMissing).run()));

        Path file = Files.createTempFile("unchecked", ".txt");
        try {
            Files.writeString(file, "hello");
            assertEquals(List.of("hello"), List.of(file).stream()
                    .map(UncheckedFunction.of(Files::readString))
                    .toList());
        } finally {
            Files.deleteIfExists(file);
        }

        assertThrows(NullPointerException.class, () -> UncheckedFunction.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedBiFunction.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedUnaryOperator.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedBinaryOperator.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedConsumer.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedBiConsumer.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedPredicate.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedBiPredicate.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedSupplier.of(null));
        assertThrows(NullPointerException.class, () -> UncheckedRunnable.of(null));
    }

    private final List<String> consumed = new ArrayList<>();
    private final List<String> consumedPairs = new ArrayList<>();

    private static final IOException MISSING = new IOException("missing");

    private static String read(String value) throws IOException {
        if ("missing".equals(value)) {
            throw MISSING;
        }
        return value;
    }

    private static String upper(String value) throws IOException {
        if ("missing".equals(value)) {
            throw MISSING;
        }
        return value.toUpperCase();
    }

    private static Integer add(Integer left, Integer right) throws IOException {
        if (left < 0) {
            throw MISSING;
        }
        return left + right;
    }

    private void consume(String value) throws IOException {
        if ("missing".equals(value)) {
            throw MISSING;
        }
        consumed.add(value);
    }

    private void consumeBoth(String left, Integer right) throws IOException {
        if (right < 0) {
            throw MISSING;
        }
        consumedPairs.add(left + right);
    }

    private static boolean keep(String value) throws IOException {
        if ("missing".equals(value)) {
            throw MISSING;
        }
        return value.length() > 1;
    }

    private static boolean bothNonEmpty(String left, String right) throws IOException {
        if (left.isEmpty()) {
            throw MISSING;
        }
        return !right.isEmpty();
    }

    private static String supply() throws IOException {
        return "ok";
    }

    private static String supplyMissing() throws IOException {
        throw MISSING;
    }

    private static void runOk() throws IOException {
    }

    private static void runMissing() throws IOException {
        throw MISSING;
    }
}
