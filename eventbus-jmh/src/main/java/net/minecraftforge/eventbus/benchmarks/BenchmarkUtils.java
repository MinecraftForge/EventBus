/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.unsafe.UnsafeHacks;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.util.List;
import java.util.function.Consumer;

public final class BenchmarkUtils {
    private BenchmarkUtils() {}

    private static final String MANAGER = "net.minecraftforge.eventbus.testjar.benchmarks.BenchmarkManager";

    public static void setupTransformedEnvironment() {
        try {
            var jar = findTestJar();

            setupBaseEnvironment();

            System.setProperty("test.harness.game", jar.getAbsolutePath()); // Jars to put in the game layer
            System.setProperty("test.harness.callable", BenchmarkUtils.TestCallback.class.getName());

            // Reflectively call `new Launcher().run("--launchTarget", "testharness")` to skip
            // some logging in Launcher#main(String[])
            var launcher = Class.forName(Launcher.class.getName());
            var ctr = launcher.getDeclaredConstructor();
            UnsafeHacks.setAccessible(ctr);
            var inst = ctr.newInstance();
            var main = launcher.getDeclaredMethod("run", String[].class);
            UnsafeHacks.setAccessible(main);
            main.invoke(inst, (Object) new String[] { "--launchTarget", "testharness" });

            validateEnvironment(true);
        } catch (Exception e) {
            sneak(e);
        }
    }

    public static void setupNormalEnvironment() {
        setupBaseEnvironment();
        validateEnvironment(false);
    }

    // Boilerplate that ModLauncher's test harness requires
    public static final class TestCallback {
        // This is reflectively called by ModLauncher's test harness launch target. The required method name and type
        // are hardcoded, and its containing class is defined by the system property "test.harness.callable".
        public static ServiceRunner supplier() {
            return ServiceRunner.NOOP;
        }
    }

    @SuppressWarnings("unchecked")
    public static Consumer<Blackhole> getPostingBenchmark(String name, int multiplier) {
        try {
            var benchmarkManager = getBenchmarkManager();
            return (Consumer<Blackhole>) benchmarkManager
                    .getDeclaredMethod("getPostingBenchmark", String.class, int.class)
                    .invoke(null, name, multiplier);
        } catch (Exception e) {
            sneak(e);
            throw new AssertionError("Unreachable");
        }
    }

    public static Runnable[] getRegistrationBenchmark(String name) {
        try {
            var benchmarkManager = getBenchmarkManager();
            return (Runnable[]) benchmarkManager
                    .getDeclaredMethod("getRegistrationBenchmark", String.class)
                    .invoke(null, name);
        } catch (Exception e) {
            sneak(e);
            throw new AssertionError("Unreachable");
        }
    }

    private static void setupBaseEnvironment() {
        try {
            var jar = findTestJar();
            var cfg = ModuleLayer.boot().configuration().resolveAndBind(ModuleFinder.of(jar.toPath()), ModuleFinder.ofSystem(), List.of("net.minecraftforge.eventbus.testjars"));
            var cl = new SecureModuleClassLoader("MODULE-CLASSLOADER", null, cfg, List.of(ModuleLayer.boot()));
            Thread.currentThread().setContextClassLoader(cl);
        } catch (Exception e) {
            sneak(e);
            throw new AssertionError("Unreachable");
        }
    }

    private static File findTestJar() throws IOException {
        var jar = new File("../eventbus-test-jar/build/libs/eventbus-test-jar.jar").getCanonicalFile();
        if (!jar.exists())
            throw new RuntimeException("Could not find test jar at: " + jar);

        return jar;
    }

    private static Class<?> getBenchmarkManager() throws Exception {
        return Class.forName(MANAGER, false, Thread.currentThread().getContextClassLoader());
    }

    private static void validateEnvironment(boolean shouldBeTransformed) {
        try {
            getBenchmarkManager()
                    .getDeclaredMethod("validate", boolean.class)
                    .invoke(null, shouldBeTransformed);
        } catch (Exception e) {
            sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneak(Throwable e) throws E {
        throw (E) e;
    }
}
