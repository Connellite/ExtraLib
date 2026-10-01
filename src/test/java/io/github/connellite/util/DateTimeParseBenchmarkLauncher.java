package io.github.connellite.util;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point to run {@link DateTimeParseBenchmark}).
 *
 */
public final class DateTimeParseBenchmarkLauncher {

    private DateTimeParseBenchmarkLauncher() {}

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DateTimeParseBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
