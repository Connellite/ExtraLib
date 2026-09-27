package io.github.connellite.util;

import io.github.connellite.logger.Logger;
import io.github.connellite.logger.LoggerFactory;
import io.github.connellite.reflection.ReflectionUtil;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@UtilityClass
public class ProcessRunner {

    /**
     * Executes an OS command, waits for completion, and returns captured standard output.
     */
    public static String runAndWaitFor(String command) throws IOException, InterruptedException {
        return run(command).stdout();
    }

    /**
     * Executes an OS command, waits up to {@code timeout}, and returns captured standard output.
     * On timeout the process is destroyed.
     */
    public static String runAndWaitFor(String command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        return run(command, timeout, unit).stdout();
    }

    /**
     * Runs {@code command} (quoted tokens supported) with no timeout.
     */
    public static ProcessResult run(String command) throws IOException, InterruptedException {
        return run(splitCommand(command));
    }

    /**
     * Runs {@code command} (quoted tokens supported) and waits up to {@code timeout}.
     */
    public static ProcessResult run(String command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        return run(splitCommand(command), timeout, unit);
    }

    /**
     * Runs {@code command} with no timeout.
     */
    public static ProcessResult run(String[] command) throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command");
        return run(List.of(command));
    }

    /**
     * Runs {@code command} and waits up to {@code timeout}.
     */
    public static ProcessResult run(String[] command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command");
        return run(List.of(command), timeout, unit);
    }

    /**
     * Runs {@code command} with no timeout.
     */
    public static ProcessResult run(List<String> command) throws IOException, InterruptedException {
        return run(command, -1, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs {@code command} via {@link ProcessBuilder}. Negative {@code timeout} waits until exit.
     * Both stdout and stderr are drained on extra threads. On timeout: {@link Process#destroy()},
     * then {@link Process#destroyForcibly()} if still alive.
     */
    public static ProcessResult run(List<String> command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (timeout >= 0) {
            Objects.requireNonNull(unit, "unit");
        }
        Process process = new ProcessBuilder(new ArrayList<>(command)).start();
        CompletableFuture<String> stdout = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderr = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));
        boolean finished;
        try {
            if (timeout < 0) {
                process.waitFor();
                finished = true;
            } else {
                finished = process.waitFor(timeout, unit);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        if (!finished) {
            LogHolder.logger.warn("Process killed due to timeout: " + command);
            process.destroy();
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
            }
        }
        return new ProcessResult(process.exitValue(), joinStream(stdout), joinStream(stderr));
    }

    /**
     * Loads {@code jar} in-process and invokes {@code Main-Class#main(String[])}.
     *
     * @deprecated No isolation; prefer {@link #run(List)} with {@code java -jar}.
     */
    @Deprecated
    public static Object runJar(File jar) throws IOException, ReflectiveOperationException {
        return invokeJar(jar, null);
    }

    /**
     * Loads {@code jar} in-process and invokes a no-arg method on {@code Main-Class}.
     *
     * @deprecated No isolation; prefer {@link #run(List)} with {@code java -jar}.
     */
    @Deprecated
    public static Object runJar(File jar, String methodName) throws IOException, ReflectiveOperationException {
        Objects.requireNonNull(methodName, "methodName");
        return invokeJar(jar, methodName);
    }

    /**
     * Loads {@code jar} bytes in-process and invokes {@code Main-Class#main(String[])}.
     *
     * @deprecated No isolation; prefer {@link #run(List)} with {@code java -jar}.
     */
    @Deprecated
    public static Object runJar(byte[] jar) throws IOException, ReflectiveOperationException {
        return runJarBytes(jar, null);
    }

    /**
     * Loads {@code jar} bytes in-process and invokes a no-arg method on {@code Main-Class}.
     *
     * @deprecated No isolation; prefer {@link #run(List)} with {@code java -jar}.
     */
    @Deprecated
    public static Object runJar(byte[] jar, String methodName) throws IOException, ReflectiveOperationException {
        Objects.requireNonNull(methodName, "methodName");
        return runJarBytes(jar, methodName);
    }

    /**
     * Splits a command line on spaces, keeping {@code "..."} as one token (quotes stripped).
     */
    static List<String> splitCommand(String command) {
        Objects.requireNonNull(command, "command");
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if ((c == ' ' || c == '\t') && !inQuotes) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        return parts;
    }

    private static Object runJarBytes(byte[] jar, String methodName) throws IOException, ReflectiveOperationException {
        Objects.requireNonNull(jar, "jar");
        Path tmp = Files.createTempFile("extralib-runjar-", ".jar");
        try {
            Files.write(tmp, jar);
            return invokeJar(tmp.toFile(), methodName);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static Object invokeJar(File jar, String methodName) throws IOException, ReflectiveOperationException {
        Objects.requireNonNull(jar, "jar");
        if (!jar.isFile() || !jar.canRead()) {
            throw new IllegalArgumentException("Unable to read jar file: " + jar);
        }
        String mainClass = readMainClass(jar);
        if (mainClass == null || mainClass.isBlank()) {
            throw new IllegalStateException("Main class not defined in MANIFEST");
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, ProcessRunner.class.getClassLoader())) {
            Class<?> clazz = Class.forName(mainClass, true, loader);
            if (methodName == null) {
                return invokeMain(clazz);
            }
            return invokeNamed(clazz, methodName);
        }
    }

    private static String readMainClass(File jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return null;
            }
            return manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        }
    }

    private static Object invokeMain(Class<?> clazz) throws ReflectiveOperationException {
        Method main = ReflectionUtil.findMethod(clazz, "main", String[].class);
        if (main == null) {
            throw new NoSuchMethodException(clazz.getName() + ".main(String[])");
        }
        Object target = Modifier.isStatic(main.getModifiers()) ? null : ReflectionUtil.getInstance(clazz);
        return ReflectionUtil.invoke(main, target, new Object[]{new String[0]});
    }

    private static Object invokeNamed(Class<?> clazz, String methodName) throws ReflectiveOperationException {
        Method method = ReflectionUtil.findMethod(clazz, methodName);
        if (method == null) {
            throw new NoSuchMethodException(clazz.getName() + "." + methodName + "()");
        }
        Object target = Modifier.isStatic(method.getModifiers()) ? null : ReflectionUtil.getInstance(clazz);
        return ReflectionUtil.invoke(method, target);
    }

    private static String readStream(InputStream in) {
        try (in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) >= 0) {
                out.write(buf, 0, read);
            }
            return out.toString(Charset.defaultCharset());
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private static String joinStream(CompletableFuture<String> future) throws IOException {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IOException(cause);
        }
    }

    /**
     * Outcome of an OS process started by {@link ProcessRunner}.
     *
     * @param exitValue process exit code
     * @param stdout    captured standard output
     * @param stderr    captured standard error
     */
    public record ProcessResult(int exitValue, String stdout, String stderr) {

        /**
         * {@code true} when {@code exitValue} is {@code 0}.
         */
        public boolean success() {
            return exitValue == 0;
        }
    }

    private static class LogHolder {
        private static final Logger logger = LoggerFactory.getLogger(ProcessRunner.class);
    }
}
