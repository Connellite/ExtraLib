package io.github.connellite.format;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point to run {@link FmtFormatBenchmark}).
 *
 */
public final class FmtFormatBenchmarkLauncher {

    private FmtFormatBenchmarkLauncher() {}

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FmtFormatBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
