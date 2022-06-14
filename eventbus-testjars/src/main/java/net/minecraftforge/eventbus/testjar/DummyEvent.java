package net.minecraftforge.eventbus.testjar;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class DummyEvent extends Event {
    public static class GoodEvent extends DummyEvent {}
    public static class BadEvent extends DummyEvent {}
    @Cancelable
    public static class CancellableEvent extends DummyEvent {}
    @HasResult
    public static class ResultEvent extends DummyEvent {}
}
