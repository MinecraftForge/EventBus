package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.Event;

public class DeadlockingEventArmsLength {
    public static class GrandParentEvent extends Event {}
    public static class ParentEvent extends GrandParentEvent {}
    public static class ChildEvent extends ParentEvent {}
    public static class Child2Event extends ParentEvent {}
    public static class DummyEvent extends Event {}
}
