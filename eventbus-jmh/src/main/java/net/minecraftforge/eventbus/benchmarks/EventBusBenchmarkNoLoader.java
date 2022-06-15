package net.minecraftforge.eventbus.benchmarks;

import net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class EventBusBenchmarkNoLoader {
    // No Runtime Patching
    @Benchmark
    public int testNoLoaderDynamic() {
        BenchmarkArmsLength.postDynamic(BenchmarkArmsLength.NoLoader);
        return 0;
    }

    @Benchmark
    public int testNoLoaderLambda() {
        BenchmarkArmsLength.postLambda(BenchmarkArmsLength.NoLoader);
        return 0;
    }

    @Benchmark
    public int testNoLoaderStatic() {
        BenchmarkArmsLength.postStatic(BenchmarkArmsLength.NoLoader);
        return 0;
    }

    @Benchmark
    public int testNoLoaderCombined() {
        BenchmarkArmsLength.postCombined(BenchmarkArmsLength.NoLoader);
        return 0;
    }
}
