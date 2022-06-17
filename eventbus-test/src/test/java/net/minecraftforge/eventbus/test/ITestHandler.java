package net.minecraftforge.eventbus.test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;

public interface ITestHandler {
    default void before(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) { before(); }
    default void before() {}
    default void after(Consumer<Class<?>> validator, Supplier<BusBuilder> builder) { after(); }
    default void after() {}

    void test(Consumer<Class<?>> validator, Supplier<BusBuilder> builder);
}