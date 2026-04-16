package io.github.connellite.system;

import io.github.connellite.util.ProcessRunner;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Snapshot helpers for the operating system, JVM memory, and CPU metrics.
 * <p>
 * CPU load percentages use the HotSpot extension {@link com.sun.management.OperatingSystemMXBean}
 * when available; otherwise those methods return {@code -1}. Values from
 * {@link com.sun.management.OperatingSystemMXBean#getProcessCpuLoad()} and
 * {@link com.sun.management.OperatingSystemMXBean#getCpuLoad()} may be {@code -1.0} at runtime
 * when the JVM cannot measure them yet.
 */
@Log
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

    /**
     * Checks whether the running OS appears to be Windows.
     *
     * @return {@code true} when {@code os.name} contains {@code "win"}
     */
    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Checks whether the running OS appears to be macOS.
     *
     * @return {@code true} when {@code os.name} contains {@code "mac"}
     */
    public static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    /**
     * Checks whether the running OS appears to be Unix/Linux.
     *
     * @return {@code true} when {@code os.name} contains {@code "nix"} or {@code "nux"}
     */
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

    /**
     * Returns the number of logical CPU cores visible to the JVM.
     *
     * @return logical processor count
     */
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

    /**
     * Returns currently free JVM heap bytes.
     *
     * @return free heap size in bytes
     */
    public static long getJvmFreeMemory() {
        return RUNTIME.freeMemory();
    }

    /**
     * Returns the maximum heap size configured for the current JVM.
     *
     * @return max heap size in bytes
     */
    public static long getJvmMaxMemory() {
        return RUNTIME.maxMemory();
    }

    /**
     * Returns total heap bytes currently allocated by the JVM.
     *
     * @return allocated heap size in bytes
     */
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

    /**
     * Best-effort CPU model detection for the current OS.
     * <p>
     * Uses OS-specific sources:
     * Linux via {@code /proc/cpuinfo}, Windows via {@code reg query}, and macOS via {@code sysctl}.
     * If detection fails or the OS is unsupported, returns {@code "unknown"}.
     *
     * @return detected processor name, or {@code "unknown"} when unavailable
     */
    public static String tryGetProcessorName() {
        try {
            String osName = getOsName();
            if (isUnix() && new File("/proc/cpuinfo").canRead()) {
                return getLinuxProcessorName();
            } else if (isWindows()) {
                return getWindowsProcessorName();
            } else if (isMac()) {
                return getMacProcessorName();
            } else {
                log.warning("Couldn't determine OS to get processor name! The OS name is " + osName);
                return "unknown";
            }
        } catch (Exception e) {
            log.warning("Couldn't get processor name! " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Much of the code here was copied from the OSHI project. This is simply stripped down to only get the CPU model.
     * <a href="https://github.com/oshi/oshi/">See here</a>
     */
    private static String getLinuxProcessorName() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get("/proc/cpuinfo"), StandardCharsets.UTF_8);
        Pattern whitespaceColonWhitespace = Pattern.compile("\\s+:\\s");
        for (String line : lines) {
            String[] splitLine = whitespaceColonWhitespace.split(line);
            if ("model name".equals(splitLine[0]) || "Processor".equals(splitLine[0])) {
                return splitLine[1];
            }
        }
        log.warning("Couldn't parse processor name!");
        return "unknown";
    }

    /**
     * <a href="https://stackoverflow.com/a/6327663">See here</a>
     */
    private static String getWindowsProcessorName() throws Exception {
        final String cpuNameCmd = "reg query \"HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0\" /v ProcessorNameString";
        final String regstrToken = "REG_SZ";
        String result = ProcessRunner.runAndWaitFor(cpuNameCmd);
        int p = result.indexOf(regstrToken);

        if (p == -1) {
            log.warning("Couldn't parse processor name!");
            return "unknown";
        }

        return result.substring(p + regstrToken.length()).trim();
    }

    /**
     * <a href="https://stackoverflow.com/a/62718963">See here</a>
     */
    private static String getMacProcessorName() throws Exception {
        String result = ProcessRunner.runAndWaitFor("sysctl -n machdep.cpu.brand_string").trim();
        if (!result.isEmpty()) {
            return result;
        } else {
            log.warning("Couldn't parse processor name!");
            return "unknown";
        }
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
