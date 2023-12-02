/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.benchmarks;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.infra.BenchmarkParams;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.unsafe.UnsafeHacks;

public class BenchmarkBase {
    private static final String MANAGER = "net.minecraftforge.eventbus.testjar.benchmarks.BenchmarkManager";
    private static final String[] TEST_LIBS = {
        "net.minecraftforge.unsafe.UnsafeHacks",
        "org.objectweb.asm.ClassReader",
        "org.objectweb.asm.commons.ClassRemapper",
        "org.objectweb.asm.tree.ClassNode"
    };


    private static final String EVENT = "net.minecraftforge.eventbus.api.Event";
    private static final String[] EB_LIBS = {
        "org.apache.logging.log4j.LogManager",
        "org.apache.logging.log4j.core.Core",
        "net.jodah.typetools.TypeResolver"
    };

    private static final String[] ML_LIBS = {
        "cpw.mods.modlauncher.Launcher",
        "net.minecraftforge.securemodules.SecureModuleClassLoader",
        "joptsimple.ArgumentList"
    };

    private Runnable setupIteration;
    private Runnable run;

    protected void setupTransformed(BenchmarkParams pars, boolean modloader) {
        try {
            var jar = findTestJar();

            System.setProperty("test.harness.game", jar.getAbsolutePath() + "," + getPath(getClass().getName()));
            System.setProperty("test.harness.callable", TestCallback.class.getName());

            var cfg = ModuleLayer.boot().configuration().resolveAndBind(ModuleFinder.of(jar.toPath()), ModuleFinder.ofSystem(), List.of("net.minecraftforge.eventbus.testjars"));
            var cl = new SecureModuleClassLoader("MODULE-CLASSLOADER", cfg, List.of(ModuleLayer.boot()));

            Thread.currentThread().setContextClassLoader(cl);

            var launcher = Class.forName(Launcher.class.getName(), true, cl);
            var ctr = launcher.getDeclaredConstructor();
            UnsafeHacks.setAccessible(ctr);
            var inst = ctr.newInstance();
            var main = launcher.getDeclaredMethod("run", String[].class);
            UnsafeHacks.setAccessible(main);
            main.invoke(inst, (Object)new String[]{"--launchTarget", "testharness"});
            var tcl = (ClassLoader)UnsafeHacks.getField(launcher.getDeclaredField("classLoader"), inst);
            var name = pars.getBenchmark().substring(pars.getBenchmark().lastIndexOf('.') + 1);
            setup(tcl, name, true, modloader, false);
        } catch (Exception e) {
            sneak(e);
        }
    }

    public static class TestCallback {
        public static ServiceRunner supplier() {
            return ServiceRunner.NOOP;
        }
    }

    protected void setupWrapped(BenchmarkParams pars) {
        var wrapper = findWrapperJar();
        if (wrapper != null)
            setupNormal(pars, true, findWrapperJar());
    }

    protected void setupNormal(BenchmarkParams pars) {
        setupNormal(pars, false, getPaths(EVENT).iterator().next());
    }

    private void setupNormal(BenchmarkParams pars, boolean wrapped, URL eventbus) {
        try {
            var urls = new ArrayList<URL>();
            urls.addAll(getPaths(getClass().getName()));
            urls.add(findTestJar().toURI().toURL());
            urls.addAll(getPaths(TEST_LIBS));
            urls.add(eventbus);
            urls.addAll(getPaths(EB_LIBS));
            urls.addAll(getPaths(ML_LIBS));

            var cl = new URLClassLoader("URL-CLASSLOADER", urls.toArray(URL[]::new), null);
            Thread.currentThread().setContextClassLoader(cl);

            var name = pars.getBenchmark().substring(pars.getBenchmark().lastIndexOf('.') + 1);
            setup(cl, name, false, false, wrapped);
        } catch (Exception e) {
            sneak(e);
        }
    }


    private void setup(ClassLoader cl, String name, boolean shouldTransform, boolean modloader, boolean neo) throws Exception {
        Class<?> cls = Class.forName(MANAGER, false, cl);
        var inst = cls.getConstructor(String.class, boolean.class, boolean.class, boolean.class)
                      .newInstance(name, shouldTransform, modloader, neo);

        setupIteration = get(cls, inst, "setupIteration");
        run = get(cls, inst, "run");
    }

    protected void setupIteration() {
        if (setupIteration != null)
            setupIteration.run();
    }

    protected void run() {
        if (run != null);
            run.run();
    }

    private Runnable get(Class<?> cls, Object inst, String name) throws Exception {
        return (Runnable)cls.getField(name).get(inst);
    }

    private File findTestJar() throws IOException {
        var jar = new File("../eventbus-test-jar/build/libs/eventbus-test-jar.jar").getCanonicalFile();
        if (!jar.exists())
            throw new RuntimeException("Could not find test jar at: " + jar);
        return jar;
    }

    private URL findWrapperJar() {
        try {
            var jar = new File("../eventbus-wrapper/build/libs/eventbus-wrapper-all.jar").getCanonicalFile();
            if (!jar.exists()) return null;
            return jar.toURI().toURL();
        } catch (Exception e) {
            return sneak(e);
        }
    }

    private List<URL> getPaths(String... resources) {
        var ret = new ArrayList<URL>();
        for (var lib : resources) {
            try {
                ret.add(getPath(lib).toURI().toURL());
            } catch (Exception e) {
                sneak(e);
            }
        }
        return ret;
    }

    private File getPath(String resource) {
        resource = resource.replace('.', '/') + ".class";
        var url = getClass().getClassLoader().getResource(resource);
        var str = url.toString();
        int len = resource.length();
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            str = url.getFile();
            len += 2;
        }
        str = str.substring(0, str.length() - len);
        var path = Paths.get(URI.create(str));
        return path.toFile();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
