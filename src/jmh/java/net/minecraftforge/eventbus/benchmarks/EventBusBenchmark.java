package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.function.Consumer;

@State(Scope.Benchmark)
public class EventBusBenchmark
{
    private Consumer<Void> postStatic;
    private Consumer<Void> postDynamic;
    private Consumer<Void> postLambda;
    private Consumer<Void> postCombined;

    @SuppressWarnings("unchecked")
    @Setup
    public void setup() throws Exception
    {
        //Forks have an incorrect working dir set, so use the absolute path to correct
        String basePath = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent().getParent().toAbsolutePath().toString();
        System.out.println(FileSystems.getDefault().getPath(basePath + "/classes/java/testJars").toAbsolutePath());
        System.setProperty("test.harness", basePath + "/classes/java/testJars," + basePath + "/classes/java/main");
        System.setProperty("test.harness.callable", "net.minecraftforge.eventbus.benchmarks.BenchmarkBootstrap");
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");

        TransformingClassLoader tcl = (TransformingClassLoader) Whitebox.getField(Launcher.class, "classLoader").get(Launcher.INSTANCE);
        Class<?> cls = Class.forName("net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength", false, tcl);
        postStatic = (Consumer<Void>) cls.getDeclaredField("postStatic").get(null);
        postDynamic = (Consumer<Void>) cls.getDeclaredField("postDynamic").get(null);
        postLambda = (Consumer<Void>) cls.getDeclaredField("postLambda").get(null);
        postCombined = (Consumer<Void>) cls.getDeclaredField("postCombined").get(null);
    }

    @Benchmark
    public int testDynamic() throws Exception
    {
        postDynamic.accept(null);
        return 0;
    }

    @Benchmark
    public int testLambda() throws Exception
    {
        postLambda.accept(null);
        return 0;
    }

    @Benchmark
    public int testStatic() throws Exception
    {
        postStatic.accept(null);
        return 0;
    }

}
