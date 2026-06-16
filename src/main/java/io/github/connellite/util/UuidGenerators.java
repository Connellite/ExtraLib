package io.github.connellite.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cryptographically strong {@link UUID} generators for versions 1 through 7 (RFC 4122 and RFC 9562).
 * <p>Time-based generators follow the timestamp, clock-sequence, and node semantics used by
 * <a href="https://github.com/f4b6a3/uuid-creator">uuid-creator</a>: millisecond clock plus an
 * internal 0–9,999 counter to simulate 100-ns resolution (RFC 4122 §4.2.1.2), a stable multicast
 * node id, and a clock sequence that advances when the timestamp repeats.</p>
 */
@UtilityClass
public class UuidGenerators {

    /** RFC 4122 DNS namespace ({@code 6ba7b810-9dad-11d1-80b4-00c04fd430c8}). */
    public static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    /** RFC 4122 URL namespace ({@code 6ba7b811-9dad-11d1-80b4-00c04fd430c8}). */
    public static final UUID NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");

    /** RFC 4122 OID namespace ({@code 6ba7b812-9dad-11d1-80b4-00c04fd430c8}). */
    public static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");

    /** RFC 4122 X.500 DN namespace ({@code 6ba7b814-9dad-11d1-80b4-00c04fd430c8}). */
    public static final UUID NAMESPACE_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

    /** Nil UUID (RFC 9562): {@code 00000000-0000-0000-0000-000000000000} — all bits zero. */
    public static final UUID UUID_NIL = new UUID(0x0000000000000000L, 0x0000000000000000L);

    /** Max UUID (RFC 9562): {@code ffffffff-ffff-ffff-ffff-ffffffffffff} — all bits one. */
    public static final UUID UUID_MAX = new UUID(0xffffffffffffffffL, 0xffffffffffffffffL);

    /** DCE local domain: POSIX UID ({@code 0}). */
    public static final int LOCAL_DOMAIN_PERSON = 0;

    /** DCE local domain: POSIX GID ({@code 1}). */
    public static final int LOCAL_DOMAIN_GROUP = 1;

    /** DCE local domain: organization/site-defined ({@code 2}). */
    public static final int LOCAL_DOMAIN_ORG = 2;

    /** Number of 100-ns clock ticks per millisecond. */
    private static final long TICKS_PER_MILLI = 10_000L;

    /**
     * 100-ns intervals from the UUID epoch (1582-10-15 UTC) to the Unix epoch (1970-01-01 UTC).
     * Equivalent to {@code UuidTime.toGregTimestamp(UuidTime.EPOCH_UNIX)} in uuid-creator.
     */
    private static final long UUID_EPOCH_OFFSET = 0x01B21DD213814000L;

    private static final long VERSION_MASK_V1 = 0x0000000000001000L;
    private static final long VERSION_MASK_V6 = 0x0000000000006000L;
    private static final long VERSION_MASK_V7 = 0x0000000000007000L;
    private static final long VERSION_NIBBLE_MASK = 0x000000000000F000L;
    private static final long VARIANT_MASK = 0x8000000000000000L;
    private static final long VARIANT_OVERFLOW_MASK = 0xc000000000000000L;
    private static final long TIMESTAMP_60_BIT_MASK = 0x0fffffffffffffffL;

    private static final ReentrantLock TIME_BASED_LOCK = new ReentrantLock();
    private static final AtomicInteger VERSION2_DOMAIN_COUNTER = new AtomicInteger();

    private static final class SecureRandomHolder {
        private static final SecureRandom INSTANCE = new SecureRandom();
    }

    /**
     * Random multicast node id generated once (uuid-creator {@code DefaultNodeIdFunction}).
     */
    private static final class NodeIdHolder {
        private static final long NODE_ID = toMulticastRandomNodeId();
    }

    /**
     * Millisecond clock plus 0–9,999 counter (uuid-creator {@code DefaultTimeFunction}).
     */
    private static final class TimeFunctionHolder {
        private static final long ADVANCE_MAX = 1_000L;

        private final Clock clock;
        private long lastTime = -1;
        private long counter = SecureRandomHolder.INSTANCE.nextInt((int) TICKS_PER_MILLI);
        private long counterMax = counter + TICKS_PER_MILLI;

        TimeFunctionHolder() {
            this(Clock.systemUTC());
        }

        TimeFunctionHolder(Clock clock) {
            this.clock = clock;
        }

        long gregorianTimestamp() {
            counter++;

            long time = clock.millis();

            if (ADVANCE_MAX > Math.abs(lastTime - time)) {
                time = Math.max(lastTime, time);
            }

            if (time == lastTime) {
                if (counter >= counterMax) {
                    time++;
                    counter = counter % TICKS_PER_MILLI;
                    counterMax = counter + TICKS_PER_MILLI;
                }
            } else {
                counter = counter % TICKS_PER_MILLI;
                counterMax = counter + TICKS_PER_MILLI;
            }

            lastTime = time;
            long unixTimestamp = (time * TICKS_PER_MILLI) + counter;
            return (unixTimestamp + UUID_EPOCH_OFFSET) & TIMESTAMP_60_BIT_MASK;
        }
    }

    /**
     * Clock sequence that advances when the Gregorian timestamp repeats
     * (uuid-creator {@code DefaultClockSeqFunction}, without the cross-factory pool).
     */
    private static final class ClockSequenceHolder {
        private long lastTimestamp = -1;
        private int sequence = SecureRandomHolder.INSTANCE.nextInt() & 0x3FFF;

        int next(long gregorianTimestamp) {
            if (gregorianTimestamp > lastTimestamp) {
                lastTimestamp = gregorianTimestamp;
                return sequence;
            }
            lastTimestamp = gregorianTimestamp;
            sequence = (sequence + 1) & 0x3FFF;
            return sequence;
        }
    }

    /**
     * UUIDv7 state machine (uuid-creator {@code TimeOrderedEpochFactory.DefaultFunction}).
     */
    private static final class Version7Holder {
        private static final long ADVANCE_MAX = 1_000L;
        private static final long UPPER_16_BITS = 0xffff000000000000L;
        private static final long UPPER_48_BITS = 0xffffffffffff0000L;

        private final Clock clock;
        private final boolean microsecondPrecision;
        private long msb;
        private long lsb;

        Version7Holder() {
            this(Clock.systemUTC(), detectMicrosecondPrecision(Clock.systemUTC()));
        }

        Version7Holder(Clock clock, boolean microsecondPrecision) {
            this.clock = clock;
            this.microsecondPrecision = microsecondPrecision;
            reset(clock.instant());
        }

        synchronized UUID next() {
            Instant now = clock.instant();
            long lastTime = msb >>> 16;
            long time = now.toEpochMilli();

            if (ADVANCE_MAX > Math.abs(lastTime - time)) {
                time = Math.max(lastTime, time);
            }

            if (time == lastTime) {
                increment(now);
            } else {
                reset(now);
            }

            return applyVersionVariant(msb, lsb, 7);
        }

        private void reset(Instant instant) {
            msb = instant.toEpochMilli() << 16;
            lsb = SecureRandomHolder.INSTANCE.nextLong();
            if (microsecondPrecision) {
                injectMicroseconds(instant);
            } else {
                msb = (msb & UPPER_48_BITS) | (SecureRandomHolder.INSTANCE.nextLong() & 0x0FFFL);
            }
        }

        private void increment(Instant instant) {
            if (microsecondPrecision) {
                injectMicroseconds(instant);
            }

            lsb = (lsb & UPPER_16_BITS);
            lsb = (lsb | VARIANT_OVERFLOW_MASK) + (1L << 48);

            if (lsb == 0L) {
                msb = (msb | VERSION_NIBBLE_MASK) + 1L;
            }

            lsb = (lsb & UPPER_16_BITS) | randomLower48Bits();
        }

        private void injectMicroseconds(Instant instant) {
            long randA = ((instant.getNano() % 1_000_000L) << 12) / 1_000_000L;
            long prev = msb & ~VERSION_NIBBLE_MASK;
            long next = (msb & UPPER_48_BITS) | (randA & 0x0FFFL);
            msb = Math.max(next, prev);
        }

        private static boolean detectMicrosecondPrecision(Clock clock) {
            int best = 0;
            for (int i = 0; i < 3; i++) {
                int nanos = clock.instant().getNano();
                best = Math.max(best, nanos % 1_000_000 == 0 ? 1 : 2);
            }
            return best == 2;
        }
    }

    private static final TimeFunctionHolder TIME_FUNCTION = new TimeFunctionHolder();
    private static final ClockSequenceHolder CLOCK_SEQUENCE = new ClockSequenceHolder();
    private static final Version7Holder VERSION7 = new Version7Holder();

    /**
     * Version 1: time-based UUID (RFC 4122 §4.2).
     *
     * @return a new version-1 UUID
     */
    public static UUID generateVersion1() {
        return generateTimeBased(1);
    }

    /**
     * Version 2: DCE Security time-based UUID (RFC 4122 §4.1.2).
     * <p>Builds a version-1 UUID, then embeds the local domain and identifier
     * (uuid-creator {@code DceSecurityFactory}).</p>
     *
     * @return a new version-2 UUID with a random 32-bit local identifier and {@link #LOCAL_DOMAIN_PERSON}
     */
    public static UUID generateVersion2() {
        long localId = SecureRandomHolder.INSTANCE.nextInt();
        return generateVersion2(LOCAL_DOMAIN_PERSON, localId);
    }

    /**
     * Version 2: DCE Security time-based UUID (RFC 4122 §4.1.2).
     *
     * @param localDomain      DCE local domain ({@code 0} = person/UID, {@code 1} = group/GID)
     * @param localIdentifier  32-bit POSIX UID or GID
     * @return a new version-2 UUID
     */
    public static UUID generateVersion2(int localDomain, long localIdentifier) {
        TIME_BASED_LOCK.lock();
        try {
            long gregorianTimestamp = TIME_FUNCTION.gregorianTimestamp();
            int clockSeq = CLOCK_SEQUENCE.next(gregorianTimestamp);
            UUID version1 = createTimeBasedUuid(1, gregorianTimestamp, clockSeq, NodeIdHolder.NODE_ID);
            long msb = embedLocalIdentifier(version1.getMostSignificantBits(), (int) localIdentifier);
            long lsb = embedLocalDomain(version1.getLeastSignificantBits(), localDomain, VERSION2_DOMAIN_COUNTER.incrementAndGet());
            return applyVersionVariant(msb, lsb, 2);
        } finally {
            TIME_BASED_LOCK.unlock();
        }
    }

    /**
     * Version 3: name-based UUID using MD5 (RFC 4122 §4.3).
     *
     * @param namespace well-known or application namespace UUID; must not be {@code null}
     * @param name      name bytes within {@code namespace}; must not be {@code null}
     * @return deterministic version-3 UUID for the given namespace and name
     * @throws NoSuchAlgorithmException if MD5 is not available
     */
    public static UUID generateVersion3(@NonNull UUID namespace, @NonNull byte[] name) throws NoSuchAlgorithmException {
        return nameBasedUuid(namespace, name, "MD5", 3);
    }

    /**
     * Version 3: name-based UUID using MD5 (RFC 4122 §4.3).
     *
     * @param namespace well-known or application namespace UUID; must not be {@code null}
     * @param name      UTF-8 name within {@code namespace}; must not be {@code null}
     * @return deterministic version-3 UUID for the given namespace and name
     * @throws NoSuchAlgorithmException if MD5 is not available
     */
    public static UUID generateVersion3(@NonNull UUID namespace, @NonNull String name) throws NoSuchAlgorithmException {
        return generateVersion3(namespace, name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Version 4: random UUID (RFC 4122 §4.4).
     *
     * @return a new version-4 UUID
     */
    public static UUID generateVersion4() {
        long mostSigBits = SecureRandomHolder.INSTANCE.nextLong();
        long leastSigBits = SecureRandomHolder.INSTANCE.nextLong();
        return applyVersionVariant(mostSigBits, leastSigBits, 4);
    }

    /**
     * Version 5: name-based UUID using SHA-1 (RFC 4122 §4.5).
     *
     * @param namespace well-known or application namespace UUID; must not be {@code null}
     * @param name      name bytes within {@code namespace}; must not be {@code null}
     * @return deterministic version-5 UUID for the given namespace and name
     * @throws NoSuchAlgorithmException if SHA-1 is not available
     */
    public static UUID generateVersion5(@NonNull UUID namespace, @NonNull byte[] name) throws NoSuchAlgorithmException {
        return nameBasedUuid(namespace, name, "SHA-1", 5);
    }

    /**
     * Version 5: name-based UUID using SHA-1 (RFC 4122 §4.5).
     *
     * @param namespace well-known or application namespace UUID; must not be {@code null}
     * @param name      UTF-8 name within {@code namespace}; must not be {@code null}
     * @return deterministic version-5 UUID for the given namespace and name
     * @throws NoSuchAlgorithmException if SHA-1 is not available
     */
    public static UUID generateVersion5(@NonNull UUID namespace, @NonNull String name) throws NoSuchAlgorithmException {
        return generateVersion5(namespace, name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Version 6: time-ordered UUID (RFC 9562 §5.6).
     *
     * @return a new version-6 UUID
     */
    public static UUID generateVersion6() {
        return generateTimeBased(6);
    }

    /**
     * Version 7: Unix-epoch time-ordered UUID (RFC 9562 §5.7).
     * <p>Uses millisecond {@code unix_ts_ms}, injects microseconds into {@code rand_a} when the
     * system clock allows, and increments {@code rand_b} when the millisecond repeats
     * (uuid-creator {@code TimeOrderedEpochFactory}).</p>
     *
     * @return a new version-7 UUID
     */
    public static UUID generateVersion7() {
        return VERSION7.next();
    }

    private static UUID generateTimeBased(int version) {
        TIME_BASED_LOCK.lock();
        try {
            long gregorianTimestamp = TIME_FUNCTION.gregorianTimestamp();
            int clockSeq = CLOCK_SEQUENCE.next(gregorianTimestamp);
            return createTimeBasedUuid(version, gregorianTimestamp, clockSeq, NodeIdHolder.NODE_ID);
        } finally {
            TIME_BASED_LOCK.unlock();
        }
    }

    private static UUID createTimeBasedUuid(int version, long gregorianTimestamp, int clockSequence, long nodeId) {
        long msb = version == 6
                ? formatMostSignificantBitsVersion6(gregorianTimestamp)
                : formatMostSignificantBitsVersion1(gregorianTimestamp);
        long lsb = formatLeastSignificantBits(clockSequence, nodeId);
        return applyVersionVariant(msb, lsb, version);
    }

    private static long formatMostSignificantBitsVersion1(long timestamp) {
        return ((timestamp & 0x0fff_0000_00000000L) >>> 48)
                | ((timestamp & 0x0000_ffff_00000000L) >>> 16)
                | ((timestamp & 0x0000_0000_ffffffffL) << 32)
                | VERSION_MASK_V1;
    }

    private static long formatMostSignificantBitsVersion6(long timestamp) {
        return ((timestamp & 0x0ffffffffffff000L) << 4)
                | (timestamp & 0x0000000000000fffL)
                | VERSION_MASK_V6;
    }

    private static long formatLeastSignificantBits(long clockSequence, long nodeId) {
        return ((((clockSequence << 48) | (nodeId & 0x0000ffffffffffffL)) & 0x3fffffffffffffffL) | VARIANT_MASK);
    }

    private static long embedLocalIdentifier(long msb, int localIdentifier) {
        return (msb & 0x00000000ffffffffL) | ((localIdentifier & 0xffffffffL) << 32);
    }

    private static long embedLocalDomain(long lsb, int localDomain, int counter) {
        return (lsb & 0x0000ffffffffffffL)
                | ((localDomain & 0xffL) << 48)
                | ((counter & 0xffL) << 56);
    }

    private static long toMulticastRandomNodeId() {
        long nodeId = SecureRandomHolder.INSTANCE.nextLong() & 0x0000ffffffffffffL;
        return nodeId | 0x0000010000000000L;
    }

    private static long randomLower48Bits() {
        return SecureRandomHolder.INSTANCE.nextLong() & 0x0000ffffffffffffL;
    }

    private static UUID applyVersionVariant(long msb, long lsb, int version) {
        long versionMask = (long) version << 12;
        long msbOut = (msb & 0xffffffffffff0fffL) | versionMask;
        long lsbOut = (lsb & 0x3fffffffffffffffL) | VARIANT_MASK;
        return new UUID(msbOut, lsbOut);
    }

    private static UUID nameBasedUuid(UUID namespace, byte[] name, String algorithm, int version)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(uuidToBytes(namespace));
        digest.update(name);
        byte[] hash = digest.digest();
        ByteBuffer buffer = ByteBuffer.wrap(hash);
        return applyVersionVariant(buffer.getLong(), buffer.getLong(), version);
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
