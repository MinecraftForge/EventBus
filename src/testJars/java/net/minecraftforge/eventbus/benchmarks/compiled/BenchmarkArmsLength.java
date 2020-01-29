package net.minecraftforge.eventbus.benchmarks.compiled;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class BenchmarkArmsLength implements Callable<Void>
{
    private static IEventBus staticSubscriberBus;
    private static IEventBus dynamicSubscriberBus;
    private static IEventBus lambdaSubscriberBus;
    private static IEventBus combinedSubscriberBus;

    @Override
    public Void call() throws Exception
    {
        System.out.println("Loading on clsloader: " + this.getClass().getClassLoader().toString());
        System.out.println("Events loading on " + this.getClass().getClassLoader().toString());
        if (!new CancelableEvent().isCancelable())
            throw new RuntimeException("Transformer did not apply!");

        staticSubscriberBus = BusBuilder.builder().build();
        dynamicSubscriberBus = BusBuilder.builder().build();
        lambdaSubscriberBus = BusBuilder.builder().build();
        combinedSubscriberBus = BusBuilder.builder().build();

        staticSubscriberBus.register(SubscriberStatic.class);
        combinedSubscriberBus.register(SubscriberStatic.class);
        dynamicSubscriberBus.register(new SubscriberDynamic());
        combinedSubscriberBus.register(new SubscriberDynamic());
        SubscriberLambda.register(lambdaSubscriberBus);
        SubscriberLambda.register(combinedSubscriberBus);
        return null;
    }

    public static final Consumer<Void> postStatic = BenchmarkArmsLength::postStatic;
    public static final Consumer<Void> postDynamic = BenchmarkArmsLength::postDynamic;
    public static final Consumer<Void> postLambda = BenchmarkArmsLength::postLambda;
    public static final Consumer<Void> postCombined = BenchmarkArmsLength::postCombined;

    public static void postStatic(Void nothing)
    {
        postAll(staticSubscriberBus);
    }

    public static void postDynamic(Void nothing)
    {
        postAll(dynamicSubscriberBus);
    }

    public static void postLambda(Void nothing)
    {
        postAll(lambdaSubscriberBus);
    }

    public static void postCombined(Void nothing)
    {
        postAll(combinedSubscriberBus);
    }

    private static void postAll(IEventBus bus)
    {
        bus.post(new CancelableEvent());
        bus.post(new ResultEvent());
        bus.post(new EventWithData("Foo", 5, true)); //Some example data
    }
}
