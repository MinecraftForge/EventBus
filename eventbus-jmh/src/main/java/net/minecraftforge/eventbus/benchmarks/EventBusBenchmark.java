package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.bootstraplauncher.BootstrapLauncher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class EventBusBenchmark
{
    private Runnable postStatic;
    private Runnable postDynamic;
    private Runnable postLambda;
    private Runnable postCombined;

    @Setup
    public void setup() throws Exception
    {
        //Forks have an incorrect working dir set, so use the absolute path to correct
        System.setProperty("test.harness.game", MockTransformerService.getTestJarsPath() + "," + MockTransformerService.getBasePath());
//        System.setProperty("test.harness.plugin", basePath + "/classes/java/main");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.benchmarks.EventBusBenchmark$TestCallback");
        System.setProperty("ignoreList", "");
        BootstrapLauncher.main("--version", "1.0", "--launchTarget", "testharness");

        Class<?> cls = Class.forName("net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength", false, Thread.currentThread().getContextClassLoader());
        postStatic = (Runnable) cls.getField("postStatic").get(null);
        postDynamic = (Runnable) cls.getField("postDynamic").get(null);
        postLambda = (Runnable) cls.getField("postLambda").get(null);
        postCombined = (Runnable) cls.getField("postCombined").get(null);
    }

    public static class TestCallback {
        public static ServiceRunner supplier() {
            return () -> BenchmarkArmsLength.supplier().run();
        }
    }
    @Benchmark
    public int testDynamic()
    {
        postDynamic.run();
        return 0;
    }

    @Benchmark
    public int testLambda()
    {
        postLambda.run();
        return 0;
    }

    @Benchmark
    public int testStatic()
    {
        postStatic.run();
        return 0;
    }

}
