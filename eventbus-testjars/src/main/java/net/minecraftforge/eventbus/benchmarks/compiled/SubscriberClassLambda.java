package net.minecraftforge.eventbus.benchmarks.compiled;

import net.minecraftforge.eventbus.api.IEventBus;

public class SubscriberClassLambda
{

    public static void register(IEventBus bus)
    {
        bus.addListener(CancelableEvent.class, SubscriberClassLambda::onCancelableEvent);
        bus.addListener(ResultEvent.class, SubscriberClassLambda::onResultEvent);
        bus.addListener(EventWithData.class, SubscriberClassLambda::onSimpleEvent);
    }

    public static void onCancelableEvent(CancelableEvent event)
    {

    }

    public static void onResultEvent(ResultEvent event)
    {

    }

    public static void onSimpleEvent(EventWithData event)
    {

    }
}
