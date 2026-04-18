package io.github.connellite.cloner;

import io.github.connellite.exception.CloningException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectionCloningTest {

    @Test
    void clone_null_returnsNull() {
        assertNull(ReflectionCloning.clone(null));
    }

    @Test
    void shallowClone_null_returnsNull() {
        assertNull(ReflectionCloning.shallowClone(null));
    }

    @Test
    void clone_simplePojo_deepCopiesFields() {
        Node n = new Node();
        n.value = 42;
        n.name = "x";

        Node copy = ReflectionCloning.clone(n);

        assertNotSame(n, copy);
        assertEquals(42, copy.value);
        assertEquals("x", copy.name);
        assertSame(n.name, copy.name);
    }

    @Test
    void clone_nestedObject_copiesGraph() {
        Node inner = new Node();
        inner.value = 1;
        Holder h = new Holder();
        h.child = inner;

        Holder copy = ReflectionCloning.clone(h);

        assertNotSame(h, copy);
        assertNotSame(h.child, copy.child);
        assertEquals(1, copy.child.value);
    }

    @Test
    void shallowClone_nestedObject_sharesChildReference() {
        Node inner = new Node();
        inner.value = 7;
        Holder h = new Holder();
        h.child = inner;

        Holder copy = ReflectionCloning.shallowClone(h);

        assertNotSame(h, copy);
        assertSame(h.child, copy.child);
    }

    @Test
    void clone_cycle_preservesStructure() {
        Cyclic a = new Cyclic();
        Cyclic b = new Cyclic();
        a.other = b;
        b.other = a;

        Cyclic a2 = ReflectionCloning.clone(a);

        assertNotSame(a, a2);
        assertNotSame(b, a2.other);
        assertSame(a2, a2.other.other);
    }

    @Test
    void clone_array_deepCopiesElements() {
        Node[] arr = {new Node()};
        arr[0].value = 99;

        Node[] copy = ReflectionCloning.clone(arr);

        assertNotSame(arr, copy);
        assertEquals(1, copy.length);
        assertNotSame(arr[0], copy[0]);
        assertEquals(99, copy[0].value);
    }

    @Test
    void clone_ignoredWrapper_returnsSameInstance() {
        Integer i = 404;
        assertSame(i, ReflectionCloning.clone(i));
    }

    @Test
    void clone_uuidField_reusesSameInstance() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        WithUuid w = new WithUuid();
        w.id = id;
        WithUuid copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.id, copy.id);
        assertEquals(id, copy.id);
    }

    @Test
    void clone_bigDecimalAndBigInteger_reused() {
        WithNumbers w = new WithNumbers();
        w.dec = new BigDecimal("123.45");
        w.bint = new BigInteger("999000999");
        WithNumbers copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.dec, copy.dec);
        assertSame(w.bint, copy.bint);
    }

    @Test
    void clone_uriField_reused() {
        URI uri = URI.create("https://example.org/path?x=1");
        WithUri w = new WithUri();
        w.uri = uri;
        WithUri copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.uri, copy.uri);
    }

    @Test
    void clone_urlField_reused() throws MalformedURLException {
        URL url = URI.create("https://example.net/").toURL();
        WithUrl w = new WithUrl();
        w.url = url;
        WithUrl copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.url, copy.url);
    }

    @Test
    void clone_patternField_reused() {
        Pattern p = Pattern.compile("[a-z]+", Pattern.CASE_INSENSITIVE);
        WithPattern w = new WithPattern();
        w.pattern = p;
        WithPattern copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.pattern, copy.pattern);
    }

    @Test
    void clone_longDoubleBooleanWrappers_reused() {
        WithBoxed w = new WithBoxed();
        w.l = Long.MIN_VALUE;
        w.d = Double.valueOf("1.5");
        w.flag = Boolean.FALSE;
        WithBoxed copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.l, copy.l);
        assertSame(w.d, copy.d);
        assertSame(w.flag, copy.flag);
    }

    @Test
    void clone_classField_reusesSameClassObject() {
        WithClass w = new WithClass();
        w.type = String.class;
        WithClass copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.type, copy.type);
    }

    @Test
    void clone_enumField_sameConstant() {
        WithEnum w = new WithEnum();
        w.season = Season.WINTER;
        WithEnum copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(Season.WINTER, copy.season);
    }

    @Test
    void clone_primitivesOnPojo_valuesCopied() {
        Primitives p = new Primitives();
        p.a = -7;
        p.b = 3.25f;
        p.c = true;
        Primitives copy = ReflectionCloning.clone(p);
        assertNotSame(p, copy);
        assertEquals(-7, copy.a);
        assertEquals(3.25f, copy.b, 0.0001f);
        assertTrue(copy.c);
    }

    @Test
    void clone_objectArray_mixedIgnoredAndNested() {
        Node n = new Node();
        n.value = 5;
        UUID u = UUID.randomUUID();
        Object[] arr = {"skip", n, u};
        Object[] copy = ReflectionCloning.clone(arr);
        assertNotSame(arr, copy);
        assertEquals(3, copy.length);
        assertSame(arr[0], copy[0]);
        assertNotSame(arr[1], copy[1]);
        assertEquals(5, ((Node) copy[1]).value);
        assertSame(arr[2], copy[2]);
    }

    @Test
    void clone_charAndByteWrappers_reused() {
        WithChars w = new WithChars();
        w.ch = 'Z';
        w.b = (byte) -12;
        WithChars copy = ReflectionCloning.clone(w);
        assertNotSame(w, copy);
        assertSame(w.ch, copy.ch);
        assertSame(w.b, copy.b);
    }

    @Test
    void clone_integerArrayField_copiesArrayKeepsSameIntegerInstances() {
        IntegerBucket b = new IntegerBucket();
        b.values = new Integer[]{1, 2, 3};
        IntegerBucket copy = ReflectionCloning.clone(b);
        assertNotSame(b, copy);
        assertNotSame(b.values, copy.values);
        assertArrayEquals(new Integer[]{1, 2, 3}, copy.values);
        assertSame(b.values[0], copy.values[0]);
    }

    @Test
    void clone_noNoArgConstructor_throwsCloningException() {
        assertThrows(CloningException.class, () -> ReflectionCloning.clone(new NoDefaultCtor(1)));
    }

    static class Node {
        int value;
        String name;

        Node() {
        }
    }

    static class Holder {
        Node child;

        Holder() {
        }
    }

    static class Cyclic {
        Cyclic other;

        Cyclic() {
        }
    }

    record NoDefaultCtor(@SuppressWarnings("unused") int x) {
        NoDefaultCtor(int x) {
            this.x = x;
        }
    }

    static class IntegerBucket {
        Integer[] values;

        IntegerBucket() {
        }
    }

    static class WithUuid {
        UUID id;

        WithUuid() {
        }
    }

    static class WithNumbers {
        BigDecimal dec;
        BigInteger bint;

        WithNumbers() {
        }
    }

    static class WithUri {
        URI uri;

        WithUri() {
        }
    }

    static class WithUrl {
        URL url;

        WithUrl() {
        }
    }

    static class WithPattern {
        Pattern pattern;

        WithPattern() {
        }
    }

    static class WithBoxed {
        Long l;
        Double d;
        Boolean flag;

        WithBoxed() {
        }
    }

    static class WithClass {
        Class<?> type;

        WithClass() {
        }
    }

    enum Season {
        WINTER,
        SUMMER
    }

    static class WithEnum {
        Season season;

        WithEnum() {
        }
    }

    static class Primitives {
        int a;
        float b;
        boolean c;
        private static final int i = 1;

        Primitives() {
        }
    }

    static class WithChars {
        Character ch;
        Byte b;
        private static final String s = "test";

        WithChars() {
        }
    }
}
