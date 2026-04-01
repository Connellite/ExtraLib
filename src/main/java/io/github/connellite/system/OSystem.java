package io.github.connellite.system;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Snapshot helpers for the operating system, JVM memory, and CPU metrics.
 * <p>
 * CPU load percentages use the HotSpot extension {@link com.sun.management.OperatingSystemMXBean}
 * when available; otherwise those methods return {@code -1}. Values from
 * {@link com.sun.management.OperatingSystemMXBean#getProcessCpuLoad()} and
 * {@link com.sun.management.OperatingSystemMXBean#getCpuLoad()} may be {@code -1.0} at runtime
 * when the JVM cannot measure them yet.
 */
@UtilityClass
public class OSystem {

    private static final Runtime RUNTIME = Runtime.getRuntime();
    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    private static final com.sun.management.OperatingSystemMXBean SUN_OS;

    static {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        SUN_OS = os instanceof com.sun.management.OperatingSystemMXBean sun ? sun : null;
    }

    /**
     * Logical volume roots reported by the JVM ({@link File#listRoots()}).
     */
    public static List<Disk> listDisks() {
        return Arrays.stream(File.listRoots())
                .map(root -> new Disk(
                        root.getAbsolutePath(),
                        root.getName(),
                        root.getTotalSpace(),
                        root.getFreeSpace(),
                        root.getUsableSpace()))
                .toList();
    }

    private static String classifyOsType(String osNameLower) {
        if (osNameLower.contains("win")) {
            return "win";
        }
        if (osNameLower.contains("mac")) {
            return "mac";
        }
        if (osNameLower.contains("nix")) {
            return "nix";
        }
        return null;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    public static boolean isUnix() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("nix") || os.contains("nux");
    }

    /**
     * @return value of the {@code os.version} system property
     */
    public static String getOsVersion() {
        return System.getProperty("os.version");
    }

    public static int getCpuCores() {
        return RUNTIME.availableProcessors();
    }

    /**
     * @return {@code os.name} in lower case
     */
    public static String getOsName() {
        return System.getProperty("os.name", "").toLowerCase();
    }

    /**
     * @return {@code "win"}, {@code "mac"}, {@code "nix"}, or {@code null} if none match
     */
    public static String getOsType() {
        return classifyOsType(getOsName());
    }

    public static long getJvmFreeMemory() {
        return RUNTIME.freeMemory();
    }

    public static long getJvmMaxMemory() {
        return RUNTIME.maxMemory();
    }

    public static long getJvmTotalMemory() {
        return RUNTIME.totalMemory();
    }

    /**
     * @return heap used in megabytes (mebibytes: 1024² bytes), rounded to two decimal places
     */
    public static double getHeapMemoryUsageMb() {
        long used = MEMORY.getHeapMemoryUsage().getUsed();
        return round(used / (1024.0 * 1024.0), 2);
    }

    /**
     * @return non-heap used in megabytes (mebibytes: 1024² bytes), rounded to two decimal places
     */
    public static double getNonHeapMemoryUsageMb() {
        long used = MEMORY.getNonHeapMemoryUsage().getUsed();
        return round(used / (1024.0 * 1024.0), 2);
    }

    /**
     * @return JVM process CPU load as a percentage 0–100, or {@code -1} if unavailable
     */
    public static int getJvmCpuLoadPercent() {
        if (SUN_OS == null) {
            return -1;
        }
        double load = SUN_OS.getProcessCpuLoad();
        if (load < 0) {
            return -1;
        }
        return (int) Math.round(load * 100.0);
    }

    /**
     * @return whole-system CPU load as a percentage 0–100, or {@code -1} if unavailable
     */
    public static int getSystemCpuLoadPercent() {
        if (SUN_OS == null) {
            return -1;
        }
        double load = SUN_OS.getCpuLoad();
        if (load < 0) {
            return -1;
        }
        return (int) Math.round(load * 100.0);
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * One filesystem root as returned by {@link #listDisks()}.
     */
    public record Disk(String path, String name, long totalSpace, long freeSpace, long usableSpace) {
    }
}
