package io.github.connellite.format;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JMH microbenchmarks: {@link Fmt} vs {@link String#format} vs {@link MessageFormat}.
 *
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 1, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class FmtFormatBenchmark {

    private static final Locale US = Locale.US;
    private static final String NAME = "Alice";
    private static final int ID = 42;
    private static final String HELLO = "hello";

    private CompiledFormat userIdFmt;
    private MessageFormat userIdMessageFormat;
    private CompiledFormat singlePlaceholder;

    @Setup
    public void setup() {
        userIdFmt = Fmt.compile("User {} has id {:d}");
        userIdMessageFormat = new MessageFormat("User {0} has id {1}", US);
        singlePlaceholder = Fmt.compile("{}");
    }

    @Benchmark
    public String fmt_userId_parsePatternEachCall() {
        return Fmt.format(US, "User {} has id {:d}", NAME, ID);
    }

    @Benchmark
    public String fmt_userId_compiledReuse() {
        return Fmt.format(US, userIdFmt, NAME, ID);
    }

    @Benchmark
    public String stringFormat_userId() {
        return String.format(US, "User %s has id %d", NAME, ID);
    }

    @Benchmark
    public String messageFormat_userId() {
        return userIdMessageFormat.format(new Object[] {NAME, ID});
    }

    @Benchmark
    public String fmt_single_placeholder_parseEach() {
        return Fmt.format("{}", HELLO);
    }

    @Benchmark
    public String fmt_single_placeholder_compiled() {
        return Fmt.format(singlePlaceholder, HELLO);
    }

    @Benchmark
    public String stringFormat_single() {
        return String.format("%s", HELLO);
    }

    @Benchmark
    public String messageFormat_single() {
        return MessageFormat.format("{0}", HELLO);
    }
}
