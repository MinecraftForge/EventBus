package net.minecraftforge.eventbus.test;

import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformingClassLoader;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.service.ModLauncherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.uncheck;

public class EventListenerRaceTest
{
    private TransformingClassLoader classLoader;

    private Class[] setup() { //stolen from benchmark
        String packageName = "net/minecraftforge/eventbus/testjar/";
        String[] toTransform = new String[]{"StaticInitTestEvent", "StaticInitTestEvent$SubEvent", "TestSubscriber"};
        Class<?>[] classes = new Class[toTransform.length];

        //Setup class transformers
        final TransformStore transformStore = new TransformStore();
        final LaunchPluginHandler lph = new LaunchPluginHandler();
        classLoader = new TransformingClassLoader(transformStore, lph);
        ClassTransformer classTransformer = uncheck(()-> Whitebox.invokeConstructor(ClassTransformer.class, new Class[] { transformStore.getClass(),  lph.getClass(), TransformingClassLoader.class }, new Object[] { transformStore, lph, null}));
        Method transform = Whitebox.getMethod(classTransformer.getClass(), "transform", byte[].class, String.class);
        LaunchPluginHandler pluginHandler = Whitebox.getInternalState(classTransformer, "pluginHandler");
        Map<String, ILaunchPluginService> plugins = Whitebox.getInternalState(pluginHandler, "plugins");
        ModLauncherService service = new ModLauncherService();
        plugins.put(service.name(), service); //Inject it

        //Setup class loader injects
        Method defineClass = Whitebox.getMethod(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class);
        //Setup event and subscriber classes
        for (int i = 0; i < toTransform.length; i++)
        {
            String className = toTransform[i];
            className = packageName + className;
            byte[] classBytes;
            try (InputStream is = getClass().getResourceAsStream("/" + className + ".class"))
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1];
                while (is.read(buf) >= 0)
                {
                    bos.write(buf);
                }
                classBytes = bos.toByteArray();
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            String clsWithDot = className.replace('/', '.');
            try
            {
                classBytes = (byte[]) transform.invoke(classTransformer, classBytes, clsWithDot);
                classes[i] = (Class) defineClass.invoke(classLoader, clsWithDot, classBytes, 0, classBytes.length);
            } catch (ReflectiveOperationException e)
            {
                throw new RuntimeException(e);
            }
        }
        return classes;
    }

    @RepeatedTest(200)
    public void test()
    {
        try
        {
            Assertions.assertTimeoutPreemptively(Duration.ofMillis(1000), this::runTest);
        }
        finally
        {
            classLoader = null; //make sure the classloader is nuked for the next run
            if (t1 != null)
                t1.stop();
            if (t2 != null)
                t2.stop();
        }
    }

    private Throwable t1Error = null;
    private Throwable t2Error = null;
    private Thread t1;
    private Thread t2;
    private void runTest() throws ClassNotFoundException, InterruptedException
    {
        classLoader = null;
        Class<?>[] classes = setup();
        IEventBus bus = new BusBuilder().build();
        t1 = new Thread(() ->
        {
            randomSleep();
            System.out.println("registering");
            bus.register(classes[2]);
            System.out.println("registered");
        });
        t2 = new Thread(() ->
        {
            randomSleep();
            System.out.println("constructing");
            try
            {
                Assertions.assertEquals(((Event) Whitebox.invokeConstructor(classes[1])).getListenerList(), EventListenerHelper.getListenerList(classes[1]));
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            System.out.println("constructed");
        });
        t1.setUncaughtExceptionHandler((t, e) -> t1Error = e);
        t2.setUncaughtExceptionHandler((t, e) -> t2Error = e);
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        if (t1Error != null)
            Assertions.fail("Error in register thread(t1)", t1Error);
        if (t2Error != null)
            Assertions.fail("Error in post thread(t2)", t2Error);
    }

    private static void randomSleep()
    {
        try
        {
            Thread.sleep(0, new Random().nextInt(10000));
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
