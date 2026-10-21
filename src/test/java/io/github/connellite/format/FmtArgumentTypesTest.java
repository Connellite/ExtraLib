package io.github.connellite.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Currency;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github.connellite.exception.FormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Feeds many kinds of argument values through {@link Fmt} with different specs, guarding against
 * regressions such as the {@link StackOverflowError} on {@link Path} (an {@code Iterable<Path>}
 * whose single-name elements iterate to themselves).
 */
class FmtArgumentTypesTest {

    private static final String[] SPECS = {
            "{}", "{:}", "{:>40}", "{:<40}", "{:*^40}", "{:?}", "{::}", "{:n}", "{:s}", "{:.3}", "{:p}",
    };

    /** Iterable whose iterator yields the iterable itself. */
    private static final class SelfIterable implements Iterable<Object> {
        @Override
        public Iterator<Object> iterator() {
            return List.<Object>of(this).iterator();
        }

        @Override
        public String toString() {
            return "self";
        }
    }

    /** Iterable that yields a fresh, equally deep iterable forever (no identity cycle). */
    private static final class EndlessNesting implements Iterable<Object> {
        @Override
        public Iterator<Object> iterator() {
            return List.<Object>of(new EndlessNesting()).iterator();
        }
    }

    private enum Color { RED }

    static Stream<Arguments> values() throws Exception {
        List<Object> cyclicList = new ArrayList<>();
        cyclicList.add(1);
        cyclicList.add(cyclicList);
        Map<String, Object> cyclicMap = new HashMap<>();
        cyclicMap.put("me", cyclicMap);
        Object[] cyclicArray = new Object[1];
        cyclicArray[0] = cyclicArray;
        // indirect cycles are covered in selfReferencingRangesRenderPlaceholder only: specs such as {:s}
        // delegate to the value's own toString(), and JDK collections overflow on indirect cycles there

        return Stream.of(
                // file system
                Arguments.of("relative single-name path", Path.of("a")),
                Arguments.of("relative multi-name path", Path.of("dir", "sub", "file.txt")),
                Arguments.of("absolute path", Path.of("").toAbsolutePath()),
                Arguments.of("empty path", Path.of("")),
                Arguments.of("root path", FileSystems.getDefault().getRootDirectories().iterator().next()),
                Arguments.of("path name element", Path.of("x", "y").getFileName()),
                Arguments.of("list of paths", List.of(Path.of("a"), Path.of("b", "c"))),
                Arguments.of("array of paths", new Path[]{Path.of("a"), Path.of("b")}),
                Arguments.of("map of paths", Map.of(Path.of("k"), Path.of("v"))),
                Arguments.of("optional path", Optional.of(Path.of("a"))),
                Arguments.of("file", new File("dir/file.txt")),
                Arguments.of("absolute file", new File("x").getAbsoluteFile()),
                // links
                Arguments.of("uri", URI.create("https://example.com/a/b?q=1#f")),
                Arguments.of("file uri", Path.of("a").toUri()),
                Arguments.of("url", URI.create("https://example.com/path").toURL()),
                Arguments.of("loopback address", InetAddress.getLoopbackAddress()),
                // misc JDK types
                Arguments.of("uuid", UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
                Arguments.of("enum", Color.RED),
                Arguments.of("enum set", EnumSet.of(Color.RED)),
                Arguments.of("time unit", TimeUnit.SECONDS),
                Arguments.of("optional empty", Optional.empty()),
                Arguments.of("string builder", new StringBuilder("sb")),
                Arguments.of("class", String.class),
                Arguments.of("plain object", new Object()),
                Arguments.of("exception", new IllegalStateException("boom")),
                Arguments.of("charset", StandardCharsets.UTF_8),
                Arguments.of("pattern", Pattern.compile("a+b")),
                Arguments.of("atomic int", new AtomicInteger(7)),
                Arguments.of("atomic ref to path", new AtomicReference<>(Path.of("a"))),
                Arguments.of("bitset", BitSet.valueOf(new long[]{5})),
                Arguments.of("locale", Locale.US),
                Arguments.of("currency", Currency.getInstance("USD")),
                Arguments.of("byte buffer", ByteBuffer.allocate(4)),
                Arguments.of("big integer", BigInteger.TEN),
                Arguments.of("big decimal", new BigDecimal("1.50")),
                Arguments.of("local date", LocalDate.of(2024, 1, 2)),
                Arguments.of("duration", Duration.ofSeconds(3)),
                Arguments.of("thread", Thread.currentThread()),
                Arguments.of("string", "text"),
                Arguments.of("char", 'c'),
                Arguments.of("int", 42),
                Arguments.of("double", 1.5),
                Arguments.of("boolean", true),
                Arguments.of("null", null),
                // ranges and edge-case ranges
                Arguments.of("empty list", List.of()),
                Arguments.of("nested list", List.of(List.of(1), List.of(List.of(2)))),
                Arguments.of("deque", new ArrayDeque<>(List.of(1, 2))),
                Arguments.of("tree map", new TreeMap<>(Map.of("a", 1))),
                Arguments.of("int array", new int[]{1, 2}),
                Arguments.of("char array", new char[]{'h', 'i'}),
                Arguments.of("nested object array", new Object[]{new int[]{1}, List.of("a")}),
                Arguments.of("unmodifiable empty set", Collections.emptySet()),
                Arguments.of("self iterable", new SelfIterable()),
                Arguments.of("endless nesting iterable", new EndlessNesting()),
                Arguments.of("cyclic list", cyclicList),
                Arguments.of("cyclic map", cyclicMap),
                Arguments.of("cyclic array", cyclicArray)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("values")
    void anyValueWithAnySpecNeverOverflowsStack(String description, Object value) {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            for (String spec : SPECS) {
                formatAllWays(description, spec, value);
            }
        });
    }

    private static void formatAllWays(String description, String spec, Object value) {
        try {
            Fmt.format(spec, value);
            Fmt.format(Locale.ROOT, Fmt.compile(spec), value);
            Fmt.format("{v}", Fmt.arg("v", value));
            Fmt.formatTo(new StringWriter(), spec, value);
            Fmt.format(spec, Arrays.asList(value, value));
            Fmt.format("{}", Collections.singletonList(value));
            Fmt.format("{}", (Object) new Object[]{value});
            Fmt.toString(value);
        } catch (FormatException | IllegalArgumentException | UnsupportedOperationException expected) {
            // a spec may legitimately be invalid for a given type; only unbounded recursion is a bug
        } catch (StackOverflowError e) {
            fail("StackOverflowError for " + description + " with spec " + spec);
        }
    }

    @Test
    void pathIsFormattedAsString() {
        Path single = Path.of("a");
        Path multi = Path.of("dir", "sub", "file.txt");
        Path absolute = Path.of("").toAbsolutePath();
        assertEquals("a", Fmt.format("{}", single));
        assertEquals(multi.toString(), Fmt.format("{}", multi));
        assertEquals(absolute.toString(), Fmt.format("{}", absolute));
        assertEquals("", Fmt.format("{}", Path.of("")));
        assertEquals("a", Fmt.toString(single));
        assertEquals("file: a!", Fmt.format("file: {}!", single));
        assertEquals("p=a", Fmt.format("p={p}", Fmt.arg("p", single)));
    }

    @Test
    void pathWithAlignmentAndWidth() {
        Path p = Path.of("a");
        assertEquals("    a", Fmt.format("{:>5}", p));
        assertEquals("a    ", Fmt.format("{:<5}", p));
        assertEquals("**a**", Fmt.format("{:*^5}", p));
    }

    @Test
    void pathsInsideRanges() {
        Path b = Path.of("b", "c");
        assertEquals("[a, " + b + "]", Fmt.format("{}", List.of(Path.of("a"), b)));
        assertEquals("[a, " + b + "]", Fmt.format("{}", (Object) new Path[]{Path.of("a"), b}));
        assertEquals("[a, " + b + "]", Fmt.format("{::}", List.of(Path.of("a"), b)));
        assertEquals("a, " + b, Fmt.format("{:n}", List.of(Path.of("a"), b)));
        Map<Path, Path> map = new LinkedHashMap<>();
        map.put(Path.of("k"), Path.of("v"));
        assertEquals("{k: v}", Fmt.format("{}", map));
    }

    @Test
    void fileUriAndUrlAreFormattedAsString() throws Exception {
        File file = new File("dir/file.txt");
        URI uri = URI.create("https://example.com/a/b?q=1#f");
        URL url = uri.toURL();
        assertEquals(file.toString(), Fmt.format("{}", file));
        assertEquals("https://example.com/a/b?q=1#f", Fmt.format("{}", uri));
        assertEquals("https://example.com/a/b?q=1#f", Fmt.format("{}", url));
        assertEquals("[" + file + ", " + uri + "]", Fmt.format("{}", List.of(file, uri)));
    }

    @Test
    void selfReferencingRangesRenderPlaceholder() {
        List<Object> list = new ArrayList<>();
        list.add(1);
        list.add(list);
        assertEquals("[1, [...]]", Fmt.format("{}", list));
        assertEquals("[1, [...]]", Fmt.format("{::}", list));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("me", map);
        assertEquals("{\"me\": {...}}", Fmt.format("{}", map));

        Object[] array = new Object[2];
        array[0] = "x";
        array[1] = array;
        assertEquals("[\"x\", [...]]", Fmt.format("{}", (Object) array));

        assertEquals("[[...]]", Fmt.format("{}", new SelfIterable()));

        Set<Object> set = new HashSet<>();
        List<Object> holder = new ArrayList<>();
        holder.add(set);
        set.add(holder);
        assertEquals("{[{...}]}", Fmt.format("{}", set));
        assertEquals("{[{...}]}", Fmt.format("{:?}", set));
    }

    @Test
    void debugSpecAppliesToNestedRanges() {
        assertEquals("[[\"a\", 'b'], [1]]", Fmt.format("{:?}", List.of(List.of("a", 'b'), List.of(1))));
        assertEquals("[a]", Fmt.format("{:?}", List.of(Path.of("a"))));
    }

    @Test
    void sameRangeTwiceIsNotACycle() {
        List<Integer> shared = List.of(1, 2);
        assertEquals("[[1, 2], [1, 2]]", Fmt.format("{}", List.of(shared, shared)));
        assertEquals("[1, 2] [1, 2]", Fmt.format("{} {}", shared, shared));
    }

    @Test
    void endlessNestingFailsWithFormatException() {
        assertThrows(FormatException.class, () -> Fmt.format("{}", new EndlessNesting()));
        // the guard state is released after a failure
        assertEquals("[1]", Fmt.format("{}", List.of(1)));
    }
}
