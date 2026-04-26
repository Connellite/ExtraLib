package io.github.connellite.reflection;

import io.github.connellite.reflection.annotation.MapField;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive tests for {@link ObjectFieldMapMapper} and {@link SimpleMapBeanMapper}, including
 * round-trips, cross-type class/record mapping, and negative cases. Keys produced by
 * {@link ObjectFieldMapMapper} are aligned with what {@link SimpleMapBeanMapper} expects
 * ({@link MapField#key()} when set).
 */
class MapBeanReflectionTest {

    @Nested
    class ObjectFieldMapMapperTests {

        @Test
        void mapsDeclaredInstanceFields() {
            SimplePojo pojo = new SimplePojo();
            pojo.id = 7;
            pojo.name = "n";

            Map<String, Object> map = ObjectFieldMapMapper.map(pojo);
            assertEquals(7, map.get("id"));
            assertEquals("n", map.get("name"));
            assertEquals(2, map.size());
        }

        @Test
        void returnsUnmodifiableMap() {
            Map<String, Object> map = ObjectFieldMapMapper.map(new SimplePojo());
            assertThrows(UnsupportedOperationException.class, () -> map.put("x", 1));
        }

        @Test
        void respectsMapFieldKeyAndIgnore() {
            AnnotatedPojo p = new AnnotatedPojo();
            p.login = "user";
            p.secret = "hidden";

            Map<String, Object> map = ObjectFieldMapMapper.map(p);
            assertEquals("user", map.get("userLogin"));
            assertFalse(map.containsKey("secret"));
            assertFalse(map.containsKey("login"));
            assertEquals(1, map.size());
        }

        @Test
        void blankMapFieldKeyUsesFieldName() {
            BlankKeyPojo p = new BlankKeyPojo();
            p.value = 99;
            Map<String, Object> map = ObjectFieldMapMapper.map(p);
            assertEquals(99, map.get("value"));
            assertEquals(1, map.size());
        }

        @Test
        void emitsSuperclassFieldsBeforeSubclass() {
            ChildPojo c = new ChildPojo();
            c.base = 1;
            c.child = 2;

            Map<String, Object> map = ObjectFieldMapMapper.map(c);
            assertEquals(List.of("base", "child"), new ArrayList<>(map.keySet()));
        }

        @Test
        void mapsHierarchyForSubclassInstance() {
            PojoChild c = new PojoChild();
            c.a = 10;
            c.b = 20;
            Map<String, Object> map = ObjectFieldMapMapper.map(c);
            assertEquals(10, map.get("a"));
            assertEquals(20, map.get("b"));
            assertEquals(List.of("a", "b"), new ArrayList<>(map.keySet()));
        }

        @Test
        void emptyPojoYieldsEmptyMap() {
            Map<String, Object> map = ObjectFieldMapMapper.map(new EmptyPojo());
            assertTrue(map.isEmpty());
        }

        @Test
        void recordComponentsMappedWithDefaultKeys() {
            PointRecord p = new PointRecord(1, 2);
            Map<String, Object> map = ObjectFieldMapMapper.map(p);
            assertEquals(1, map.get("x"));
            assertEquals(2, map.get("y"));
        }

        @Test
        void recordWithMapFieldKeyUsesAliasInMap() {
            KeyedRecord r = new KeyedRecord(5, "z");
            Map<String, Object> map = ObjectFieldMapMapper.map(r);
            assertEquals(5, map.get("kx"));
            assertEquals("z", map.get("label"));
            assertFalse(map.containsKey("id"));
        }

        @Test
        void recordWithIgnoredComponentOmitsKey() {
            RecordWithIgnored r = new RecordWithIgnored("skip", 3);
            Map<String, Object> map = ObjectFieldMapMapper.map(r);
            assertFalse(map.containsKey("ignored"));
            assertEquals(3, map.get("id"));
            assertEquals(1, map.size());
        }

        @Test
        void duplicateMapFieldKeysLastWriteWins() {
            DupKeyPojo p = new DupKeyPojo();
            p.first = 1;
            p.second = 2;
            Map<String, Object> map = ObjectFieldMapMapper.map(p);
            assertEquals(1, map.size());
            assertEquals(2, map.get("same"));
        }

        @Test
        void nullSourceThrowsNpe() {
            assertThrows(NullPointerException.class, () -> ObjectFieldMapMapper.map(null));
        }
    }

    @Nested
    class SimpleMapBeanMapperTests {

        @Test
        void buildsPojoFromMap() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", 5);
            row.put("name", "row");

            SimplePojo out = mapper.mapRow(row);
            assertEquals(5, out.id);
            assertEquals("row", out.name);
        }

        @Test
        void coercesNumberToPrimitiveInt() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            Map<String, Object> row = Map.of("id", Long.valueOf(9L), "name", "x");

            SimplePojo out = mapper.mapRow(row);
            assertEquals(9, out.id);
            assertEquals("x", out.name);
        }

        @Test
        void missingKeyForReferenceTypeYieldsNull() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", 1);

            SimplePojo out = mapper.mapRow(row);
            assertEquals(1, out.id);
            assertNull(out.name);
        }

        @Test
        void mapFieldKeyOnPojoField() {
            SimpleMapBeanMapper<KeyedPojo> mapper = new SimpleMapBeanMapper<>(KeyedPojo.class);
            KeyedPojo out = mapper.mapRow(Map.of("kx", 4, "label", "L"));
            assertEquals(4, out.id);
            assertEquals("L", out.label);
        }

        @Test
        void mapFieldIgnoreOnPojoSkipsBinding() {
            SimpleMapBeanMapper<AnnotatedPojo> mapper = new SimpleMapBeanMapper<>(AnnotatedPojo.class);
            Map<String, Object> row = Map.of("userLogin", "u");
            AnnotatedPojo out = mapper.mapRow(row);
            assertEquals("u", out.login);
            assertNull(out.secret);
        }

        @Test
        void mapsSubclassWithInheritedNonFinalFields() {
            SimpleMapBeanMapper<PojoChild> mapper = new SimpleMapBeanMapper<>(PojoChild.class);
            PojoChild out = mapper.mapRow(Map.of("a", 100, "b", 200));
            assertEquals(100, out.a);
            assertEquals(200, out.b);
        }

        @Test
        void buildsRecordFromMap() {
            SimpleMapBeanMapper<PointRecord> mapper = new SimpleMapBeanMapper<>(PointRecord.class);
            PointRecord p = mapper.mapRow(Map.of("x", 3, "y", 4));
            assertEquals(3, p.x());
            assertEquals(4, p.y());
        }

        @Test
        void usesMapFieldKeyOnRecordComponents() {
            SimpleMapBeanMapper<KeyedRecord> mapper = new SimpleMapBeanMapper<>(KeyedRecord.class);
            Map<String, Object> row = Map.of("kx", 100, "label", "ok");
            KeyedRecord r = mapper.mapRow(row);
            assertEquals(100, r.id());
            assertEquals("ok", r.label());
        }

        @Test
        void ignoredRecordComponentAcceptsMissingKey() {
            SimpleMapBeanMapper<RecordWithIgnored> mapper = new SimpleMapBeanMapper<>(RecordWithIgnored.class);
            Map<String, Object> row = Map.of("id", 1);
            RecordWithIgnored r = mapper.mapRow(row);
            assertEquals(1, r.id());
            assertNotNull(r);
        }

        @Test
        void coercesEnumFromName() {
            SimpleMapBeanMapper<EnumHolderPojo> mapper = new SimpleMapBeanMapper<>(EnumHolderPojo.class);
            EnumHolderPojo out = mapper.mapRow(Map.of("shade", "LIGHT"));
            assertEquals(Shade.LIGHT, out.shade);
        }

        @Test
        void coercesBooleanFromString() {
            SimpleMapBeanMapper<BoolPojo> mapper = new SimpleMapBeanMapper<>(BoolPojo.class);
            BoolPojo out = mapper.mapRow(Map.of("flag", "true"));
            assertTrue(out.flag);
        }

        @Test
        void coercesUuidFromString() {
            UUID u = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            SimpleMapBeanMapper<UuidPojo> mapper = new SimpleMapBeanMapper<>(UuidPojo.class);
            UuidPojo out = mapper.mapRow(Map.of("id", u.toString()));
            assertEquals(u, out.id);
        }

        @Test
        void nonInstantiableWrapperClassThrowsIllegalState() {
            SimpleMapBeanMapper<Integer> mapper = new SimpleMapBeanMapper<>(Integer.class);
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> mapper.mapRow(Map.of("value", 1)));
            assertTrue(ex.getMessage().contains("Cannot instantiate java.lang.Integer"), ex.getMessage());
        }

        @Test
        void nullRowThrowsNpe() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            assertThrows(NullPointerException.class, () -> mapper.mapRow(null));
        }

        @Test
        void constructorNullBeanClassThrowsNpe() {
            assertThrows(NullPointerException.class, () -> new SimpleMapBeanMapper<>(null));
        }

        @Test
        void invalidIntStringThrowsIllegalArgument() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            Map<String, Object> row = Map.of("id", "not-an-int", "name", "x");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mapper.mapRow(row));
            // NumberUtils.parseNumber may return null for bad input; then primitive-null guard throws without cause.
            assertTrue(
                    ex.getCause() != null
                            || "Cannot map null to primitive field 'id'".equals(ex.getMessage()),
                    () -> "message=" + ex.getMessage() + ", cause=" + ex.getCause());
        }

        @Test
        void nullForPrimitivePojoFieldThrowsWithFieldName() {
            SimpleMapBeanMapper<SimplePojo> mapper = new SimpleMapBeanMapper<>(SimplePojo.class);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", null);
            row.put("name", "ok");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mapper.mapRow(row));
            assertEquals("Cannot map null to primitive field 'id'", ex.getMessage());
        }

        @Test
        void nullForPrimitiveRecordComponentThrowsWithComponentName() {
            SimpleMapBeanMapper<PointRecord> mapper = new SimpleMapBeanMapper<>(PointRecord.class);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("x", 1);
            row.put("y", null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> mapper.mapRow(row));
            assertEquals("Cannot map null to primitive record component 'y'", ex.getMessage());
        }
    }

    @Nested
    class RoundTripTests {

        @Test
        void record_withoutAnnotations_objectFieldMapThenSimpleMapBeanMapper() {
            PointRecord original = new PointRecord(11, 22);

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            assertEquals(11, row.get("x"));
            assertEquals(22, row.get("y"));

            PointRecord restored = new SimpleMapBeanMapper<>(PointRecord.class).mapRow(row);
            assertEquals(original, restored);
        }

        @Test
        void record_withMapFieldKey_roundTripsWhenKeysMatch() {
            KeyedRecord original = new KeyedRecord(42, "hi");

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            assertEquals(42, row.get("kx"));
            assertEquals("hi", row.get("label"));

            KeyedRecord restored = new SimpleMapBeanMapper<>(KeyedRecord.class).mapRow(row);
            assertEquals(original, restored);
        }

        @Test
        void record_withIgnoredComponent_roundTrips() {
            RecordWithIgnored original = new RecordWithIgnored("noise", 7);
            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            assertEquals(7, row.get("id"));
            RecordWithIgnored restored = new SimpleMapBeanMapper<>(RecordWithIgnored.class).mapRow(row);
            assertEquals(original.id(), restored.id());
            assertNull(restored.ignored());
        }

        @Test
        void class_withAnnotations_roundTrips() {
            AnnotatedPojo original = new AnnotatedPojo();
            original.login = "alpha";
            original.secret = "do-not-map";

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            assertEquals("alpha", row.get("userLogin"));
            assertFalse(row.containsKey("secret"));

            AnnotatedPojo restored = new SimpleMapBeanMapper<>(AnnotatedPojo.class).mapRow(row);
            assertEquals("alpha", restored.login);
            assertNull(restored.secret);
        }

        @Test
        void class_withoutAnnotations_roundTrips() {
            SimplePojo original = new SimplePojo();
            original.id = 77;
            original.name = "plain";

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            SimplePojo restored = new SimpleMapBeanMapper<>(SimplePojo.class).mapRow(row);

            assertEquals(original.id, restored.id);
            assertEquals(original.name, restored.name);
        }

        @Test
        void pojoWithMapFieldKey_roundTrips() {
            KeyedPojo original = new KeyedPojo();
            original.id = 33;
            original.label = "L";

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            KeyedPojo restored = new SimpleMapBeanMapper<>(KeyedPojo.class).mapRow(row);
            assertEquals(original.id, restored.id);
            assertEquals(original.label, restored.label);
        }
    }

    @Nested
    class ClassToRecordCrossTests {

        @Test
        void class_mapsToRecord_whenKeysMatch() {
            PointPojo original = new PointPojo();
            original.x = 3;
            original.y = 9;

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            PointRecord restored = new SimpleMapBeanMapper<>(PointRecord.class).mapRow(row);

            assertEquals(original.x, restored.x());
            assertEquals(original.y, restored.y());
        }

        @Test
        void annotatedClass_mapsToAnnotatedRecord_whenKeysMatch() {
            KeyedPojo original = new KeyedPojo();
            original.id = 15;
            original.label = "keyed";

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            KeyedRecord restored = new SimpleMapBeanMapper<>(KeyedRecord.class).mapRow(row);

            assertEquals(original.id, restored.id());
            assertEquals(original.label, restored.label());
        }

        @Test
        void throwsWhenRequiredRecordKeyIsMissing() {
            MissingYPojo original = new MissingYPojo();
            original.x = 10;

            Map<String, Object> row = ObjectFieldMapMapper.map(original);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SimpleMapBeanMapper<>(PointRecord.class).mapRow(row));
            assertEquals("Cannot map null to primitive record component 'y'", ex.getMessage());
        }

        @Test
        void throwsWhenAnnotatedRecordKeyMissing() {
            WrongKeyPojo original = new WrongKeyPojo();
            original.id = 1;
            original.label = "x";

            Map<String, Object> row = ObjectFieldMapMapper.map(original);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SimpleMapBeanMapper<>(KeyedRecord.class).mapRow(row));
            assertEquals("Cannot map null to primitive record component 'id'", ex.getMessage());
        }
    }

    @Nested
    class RecordToClassCrossTests {

        @Test
        void record_mapsToClass_whenKeysMatch() {
            PointRecord original = new PointRecord(5, 8);

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            PointPojo restored = new SimpleMapBeanMapper<>(PointPojo.class).mapRow(row);

            assertEquals(original.x(), restored.x);
            assertEquals(original.y(), restored.y);
        }

        @Test
        void annotatedRecord_mapsToAnnotatedClass_whenKeysMatch() {
            KeyedRecord original = new KeyedRecord(21, "round");

            Map<String, Object> row = ObjectFieldMapMapper.map(original);
            KeyedPojo restored = new SimpleMapBeanMapper<>(KeyedPojo.class).mapRow(row);

            assertEquals(original.id(), restored.id);
            assertEquals(original.label(), restored.label);
        }

        @Test
        void throwsWhenRequiredClassKeyIsMissing() {
            XOnlyRecord original = new XOnlyRecord(33);

            Map<String, Object> row = ObjectFieldMapMapper.map(original);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> new SimpleMapBeanMapper<>(PointPojo.class).mapRow(row));
            assertEquals("Cannot map null to primitive field 'y'", ex.getMessage());
        }
    }

    // --- shared fixtures ---

    enum Shade {
        LIGHT, DARK
    }

    static final class SimplePojo {
        int id;
        String name;
    }

    static final class AnnotatedPojo {
        @MapField(key = "userLogin")
        String login;

        @MapField(ignore = true)
        String secret;
    }

    static final class BlankKeyPojo {
        @MapField(key = "")
        int value;
    }

    static final class PointPojo {
        int x;
        int y;
    }

    static final class KeyedPojo {
        @MapField(key = "kx")
        int id;
        String label;
    }

    static class PojoParent {
        int a;
    }

    static final class PojoChild extends PojoParent {
        int b;
    }

    static class BasePojo {
        int base;
    }

    static final class ChildPojo extends BasePojo {
        int child;
    }

    static final class EmptyPojo {
    }

    static final class DupKeyPojo {
        @MapField(key = "same")
        int first;
        @MapField(key = "same")
        int second;
    }

    static final class EnumHolderPojo {
        Shade shade;
    }

    static final class BoolPojo {
        boolean flag;
    }

    static final class UuidPojo {
        UUID id;
    }

    static final class WrongKeyPojo {
        int id;
        String label;
    }

    record PointRecord(int x, int y) {
    }

    record KeyedRecord(@MapField(key = "kx") int id, String label) {
    }

    record RecordWithIgnored(@MapField(ignore = true) String ignored, int id) {
    }

    static final class MissingYPojo {
        int x;
    }

    record XOnlyRecord(int x) {
    }
}
