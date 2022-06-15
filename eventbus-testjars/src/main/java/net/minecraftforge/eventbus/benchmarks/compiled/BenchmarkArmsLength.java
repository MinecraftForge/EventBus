package net.minecraftforge.eventbus.benchmarks.compiled;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;

public class BenchmarkArmsLength
{
    private static IEventBus staticSubscriberBus;
    private static IEventBus dynamicSubscriberBus;
    private static IEventBus lambdaSubscriberBus;
    private static IEventBus combinedSubscriberBus;

    public static Runnable supplier()
    {
        return () -> {
            try {
                Event.class.getDeclaredField("LISTENER_LIST");
            } catch (Exception e) {
                throw new RuntimeException("Transformer did not apply!", e);
            }

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
        };
    }

    public static final Runnable postStatic = BenchmarkArmsLength::postStatic;
    public static final Runnable postDynamic = BenchmarkArmsLength::postDynamic;
    public static final Runnable postLambda = BenchmarkArmsLength::postLambda;
    public static final Runnable postCombined = BenchmarkArmsLength::postCombined;

    public static void postStatic()
    {
        postAll(staticSubscriberBus);
    }

    public static void postDynamic()
    {
        postAll(dynamicSubscriberBus);
    }

    public static void postLambda()
    {
        postAll(lambdaSubscriberBus);
    }

    public static void postCombined()
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
