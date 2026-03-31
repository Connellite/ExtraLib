package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class ProcessRunner {
    public static String runAndWaitFor(String command) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();

        return getProcessOutput(p);
    }

    public static String runAndWaitFor(String command, long timeout, TimeUnit unit)
            throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor(timeout, unit);

        return getProcessOutput(p);
    }

    private static String getProcessOutput(Process process) throws IOException {
        try (InputStream in = process.getInputStream()) {
            int readLen;
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[32];
            while ((readLen = in.read(buf, 0, buf.length)) >= 0) {
                b.write(buf, 0, readLen);
            }
            return b.toString();
        }
    }
}