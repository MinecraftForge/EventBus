package net.minecraftforge.eventbus.benchmarks;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;
import net.minecraftforge.eventbus.api.IEventBus;
import org.powermock.reflect.Whitebox;

import java.util.concurrent.Callable;

public class BenchmarkBootstrap
{
    public static Callable<Void> supplier() throws Exception {
        TransformingClassLoader tcl = (TransformingClassLoader) Whitebox.getField(Launcher.class, "classLoader").get(Launcher.INSTANCE);
        System.out.println("Bootstrapping... tcl=" + tcl);
        tcl.addTargetPackageFilter(s->
        {
            boolean result = !(s.startsWith("net.minecraftforge.eventbus.") && !s.startsWith("net.minecraftforge.eventbus.benchmark"));
            System.out.println("Filter query : " + s + ", result=" + result);
            return result;
        });
        final Class<?> clazz;
        try {
            clazz = Class.forName("net.minecraftforge.eventbus.benchmarks.compiled.BenchmarkArmsLength", true, tcl);
            return (Callable<Void>)clazz.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
