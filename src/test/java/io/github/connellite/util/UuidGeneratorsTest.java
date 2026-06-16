package io.github.connellite.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidGeneratorsTest {

    @Test
    void generateVersion1_hasExpectedVersionVariantAndUniqueness() {
        UUID uuid = UuidGenerators.generateVersion1();
        assertEquals(1, uuid.version());
        assertEquals(2, uuid.variant());

        UUID another = UuidGenerators.generateVersion1();
        assertNotEquals(uuid, another);
    }

    @Test
    void generateVersion2_hasExpectedVersionVariantAndUniqueness() {
        UUID uuid = UuidGenerators.generateVersion2();
        assertEquals(2, uuid.version());
        assertEquals(2, uuid.variant());

        UUID explicit = UuidGenerators.generateVersion2(UuidGenerators.LOCAL_DOMAIN_GROUP, 42L);
        assertEquals(2, explicit.version());
        assertEquals(2, explicit.variant());
        assertNotEquals(uuid, explicit);
    }

    @Test
    void generateVersion2_embedsLocalIdentifierAndDomain() {
        UUID uuid = UuidGenerators.generateVersion2(UuidGenerators.LOCAL_DOMAIN_ORG, 42L);

        assertEquals(42L, uuid.getMostSignificantBits() >>> 32);
        assertEquals(UuidGenerators.LOCAL_DOMAIN_ORG, (uuid.getLeastSignificantBits() >>> 48) & 0xffL);
    }

    @Test
    void generateVersion3_matchesIndependentMd5Assembly() throws NoSuchAlgorithmException {
        UUID expected = expectedNameBasedUuid(
                UuidGenerators.NAMESPACE_DNS,
                "www.example.org",
                "MD5",
                3);
        UUID actual = UuidGenerators.generateVersion3(UuidGenerators.NAMESPACE_DNS, "www.example.org");
        assertEquals(expected, actual);
        assertEquals(3, actual.version());
        assertEquals(2, actual.variant());
    }

    @Test
    void generateVersion3_matchesRfcDnsExample() throws NoSuchAlgorithmException {
        UUID actual = UuidGenerators.generateVersion3(UuidGenerators.NAMESPACE_DNS, "www.example.com");
        assertEquals(UUID.fromString("5df41881-3aed-3515-88a7-2f4a814cf09e"), actual);
    }

    @Test
    void generateVersion4_hasExpectedVersionVariantAndUniqueness() {
        UUID uuid = UuidGenerators.generateVersion4();
        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());

        Set<UUID> generated = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            generated.add(UuidGenerators.generateVersion4());
        }
        assertEquals(32, generated.size());
    }

    @Test
    void generateVersion5_matchesIndependentSha1Assembly() throws NoSuchAlgorithmException {
        UUID expected = expectedNameBasedUuid(
                UuidGenerators.NAMESPACE_DNS,
                "www.example.org",
                "SHA-1",
                5);
        UUID actual = UuidGenerators.generateVersion5(UuidGenerators.NAMESPACE_DNS, "www.example.org");
        assertEquals(expected, actual);
        assertEquals(5, actual.version());
        assertEquals(2, actual.variant());
    }

    @Test
    void generateVersion5_matchesRfcDnsExample() throws NoSuchAlgorithmException {
        UUID actual = UuidGenerators.generateVersion5(UuidGenerators.NAMESPACE_DNS, "www.example.com");
        assertEquals(UUID.fromString("2ed6657d-e927-568b-95e1-2665a8aea6a2"), actual);
    }

    @Test
    void generateVersion6_hasExpectedVersionVariantAndUniqueness() {
        UUID uuid = UuidGenerators.generateVersion6();
        assertEquals(6, uuid.version());
        assertEquals(2, uuid.variant());

        UUID another = UuidGenerators.generateVersion6();
        assertNotEquals(uuid, another);
    }

    @Test
    void version1And6_preserveTimestampClockSequenceAndNodeLayout() throws Exception {
        long timestamp = 0x0123_4567_89ab_cdefL;
        int clockSequence = 0x1234;
        long nodeId = 0x0102_0304_0506L;

        Method createTimeBasedUuid = UuidGenerators.class.getDeclaredMethod(
                "createTimeBasedUuid",
                int.class,
                long.class,
                int.class,
                long.class);
        createTimeBasedUuid.setAccessible(true);

        UUID version1 = (UUID) createTimeBasedUuid.invoke(null, 1, timestamp, clockSequence, nodeId);
        UUID version6 = (UUID) createTimeBasedUuid.invoke(null, 6, timestamp, clockSequence, nodeId);

        assertEquals(timestamp, extractVersion1Timestamp(version1));
        assertEquals(timestamp, extractVersion6Timestamp(version6));
        assertEquals(clockSequence, extractClockSequence(version1));
        assertEquals(clockSequence, extractClockSequence(version6));
        assertEquals(nodeId, extractNodeId(version1));
        assertEquals(nodeId, extractNodeId(version6));
    }

    @Test
    void generateVersion7_hasExpectedVersionVariantTimestampAndUniqueness() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidGenerators.generateVersion7();
        long after = System.currentTimeMillis();

        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());

        long embeddedMillis = uuid.getMostSignificantBits() >>> 16;
        assertTrue(embeddedMillis >= before);
        assertTrue(embeddedMillis <= after);

        UUID another = UuidGenerators.generateVersion7();
        assertNotEquals(uuid, another);
    }

    @Test
    void generateVersion1And7_produceUniqueUuidsInBurst() {
        Set<UUID> version1 = new HashSet<>();
        Set<UUID> version7 = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            version1.add(UuidGenerators.generateVersion1());
            version7.add(UuidGenerators.generateVersion7());
        }
        assertEquals(100, version1.size());
        assertEquals(100, version7.size());
    }

    @Test
    void generateVersion7_isMonotonicInBurst() {
        UUID previous = UuidGenerators.generateVersion7();
        for (int i = 0; i < 100; i++) {
            UUID next = UuidGenerators.generateVersion7();
            assertTrue(previous.compareTo(next) < 0);
            previous = next;
        }
    }

    @Test
    void version7OverflowIncrement_keepsTimestampMonotonicWhenRandBOverflows() throws Exception {
        Class<?> holderClass = Class.forName("io.github.connellite.util.UuidGenerators$Version7Holder");
        Constructor<?> ctor = holderClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object holder = ctor.newInstance();

        Method reset = holderClass.getDeclaredMethod("reset", java.time.Instant.class);
        reset.setAccessible(true);
        Method increment = holderClass.getDeclaredMethod("increment", java.time.Instant.class);
        increment.setAccessible(true);
        Method applyVersionVariant = UuidGenerators.class.getDeclaredMethod("applyVersionVariant", long.class, long.class, int.class);
        applyVersionVariant.setAccessible(true);

        java.time.Instant instant = java.time.Instant.ofEpochMilli(1_700_000_000_000L);
        reset.invoke(holder, instant);

        java.lang.reflect.Field msbField = holderClass.getDeclaredField("msb");
        java.lang.reflect.Field lsbField = holderClass.getDeclaredField("lsb");
        msbField.setAccessible(true);
        lsbField.setAccessible(true);

        long initialMsb = msbField.getLong(holder);
        long upperLsb = lsbField.getLong(holder) & 0xFFFF000000000000L;
        lsbField.setLong(holder, upperLsb | 0xFFFF000000000000L);

        increment.invoke(holder, instant);

        long incrementedMsb = msbField.getLong(holder);
        long incrementedLsb = lsbField.getLong(holder);
        UUID uuidAfterOverflow = (UUID) applyVersionVariant.invoke(null, incrementedMsb, incrementedLsb, 7);

        assertTrue(incrementedMsb >= initialMsb, "MSB must stay monotonic after overflow");
        assertEquals(7, uuidAfterOverflow.version());
        assertEquals(2, uuidAfterOverflow.variant());
    }

    @Test
    void wellKnownNamespaces_matchRfc4122() {
        assertEquals(UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), UuidGenerators.NAMESPACE_DNS);
        assertEquals(UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8"), UuidGenerators.NAMESPACE_URL);
        assertEquals(UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8"), UuidGenerators.NAMESPACE_OID);
        assertEquals(UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8"), UuidGenerators.NAMESPACE_X500);
    }

    @Test
    void generateVersion3And5_areDeterministicForSameInput() throws NoSuchAlgorithmException {
        UUID v3a = UuidGenerators.generateVersion3(UuidGenerators.NAMESPACE_URL, "https://example.org");
        UUID v3b = UuidGenerators.generateVersion3(UuidGenerators.NAMESPACE_URL, "https://example.org");
        assertEquals(v3a, v3b);

        UUID v5a = UuidGenerators.generateVersion5(UuidGenerators.NAMESPACE_OID, new byte[]{1, 2, 3});
        UUID v5b = UuidGenerators.generateVersion5(UuidGenerators.NAMESPACE_OID, new byte[]{1, 2, 3});
        assertEquals(v5a, v5b);
    }

    @Test
    void generatedVersion1NodeIdUsesMulticastRandomNodeBit() {
        UUID uuid = UuidGenerators.generateVersion1();
        assertTrue((extractNodeId(uuid) & 0x0000_0100_0000_0000L) != 0);
    }

    private static UUID expectedNameBasedUuid(UUID namespace, String name, String algorithm, int version)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(uuidToBytes(namespace));
        digest.update(name.getBytes(StandardCharsets.UTF_8));
        byte[] hash = digest.digest();

        long mostSigBits = bytesToLong(hash, 0);
        long leastSigBits = bytesToLong(hash, 8);
        mostSigBits = (mostSigBits & 0xFFFFFFFFFFFF0FFFL) | ((long) version << 12);
        leastSigBits = (leastSigBits & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(mostSigBits, leastSigBits);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (56 - (i * 8)));
            bytes[8 + i] = (byte) (lsb >>> (56 - (i * 8)));
        }
        return bytes;
    }

    private static long bytesToLong(byte[] bytes, int offset) {
        long value = 0L;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (bytes[offset + i] & 0xFFL);
        }
        return value;
    }

    private static long extractVersion1Timestamp(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long timeLow = msb >>> 32;
        long timeMid = (msb >>> 16) & 0xffffL;
        long timeHigh = msb & 0x0fffL;
        return (timeHigh << 48) | (timeMid << 32) | timeLow;
    }

    private static long extractVersion6Timestamp(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long high = (msb & 0xffff_ffff_ffff_0000L) >>> 4;
        long low = msb & 0x0fffL;
        return high | low;
    }

    private static int extractClockSequence(UUID uuid) {
        return (int) ((uuid.getLeastSignificantBits() >>> 48) & 0x3fffL);
    }

    private static long extractNodeId(UUID uuid) {
        return uuid.getLeastSignificantBits() & 0x0000_ffff_ffff_ffffL;
    }
}
