package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength;

import java.util.function.Consumer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class EventBusBenchmark {
    private static final String ARMS_LENGTH = "net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength";

    private Object ModLauncher;
    private Object ClassLoader;
    private Consumer<Object> postStatic;
    private Consumer<Object> postDynamic;
    private Consumer<Object> postLambda;
    private Consumer<Object> postCombined;

    @SuppressWarnings("unchecked")
    @Setup
    public void setup() throws Exception {
        System.setProperty("test.harness.game", MockTransformerService.getTestJarsPath() + "," + MockTransformerService.getBasePath());
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.benchmarks.EventBusBenchmark$TestCallback");
        System.setProperty("ignoreList", "");
        BootstrapLauncher.main("--version", "1.0", "--launchTarget", "testharness");

        Class<?> cls = Class.forName(ARMS_LENGTH, false, Thread.currentThread().getContextClassLoader());
        ModLauncher  = cls.getField("ModLauncher").get(null);
        ClassLoader  = cls.getField("ClassLoader").get(null);
        postStatic   = (Consumer<Object>)cls.getField("postStatic").get(null);
        postDynamic  = (Consumer<Object>)cls.getField("postDynamic").get(null);
        postLambda   = (Consumer<Object>)cls.getField("postLambda").get(null);
        postCombined = (Consumer<Object>)cls.getField("postCombined").get(null);
    }

    public static class TestCallback {
        public static ServiceRunner supplier() {
            return () -> BenchmarkArmsLength.supplier().run();
        }
    }

    // ModLauncher ASM Factory
    @Benchmark
    public int testModLauncherDynamic() throws Throwable {
        postDynamic.accept(ModLauncher);
        return 0;
    }

    @Benchmark
    public int testModLauncherLambda() throws Throwable {
        postLambda.accept(ModLauncher);
        return 0;
    }

    @Benchmark
    public int testModLauncherStatic() throws Throwable {
        postStatic.accept(ModLauncher);
        return 0;
    }

    @Benchmark
    public int testModLauncherCombined() throws Throwable {
        postCombined.accept(ModLauncher);
        return 0;
    }

    // ClassLoader ASM Factory
    @Benchmark
    public int testClassLoaderDynamic() throws Throwable {
        postDynamic.accept(ClassLoader);
        return 0;
    }

    @Benchmark
    public int testClassLoaderLambda() throws Throwable {
        postLambda.accept(ClassLoader);
        return 0;
    }

    @Benchmark
    public int testClassLoaderStatic() throws Throwable {
        postStatic.accept(ClassLoader);
        return 0;
    }

    @Benchmark
    public int testClassLoaderCombined() throws Throwable {
        postCombined.accept(ClassLoader);
        return 0;
    }
}
