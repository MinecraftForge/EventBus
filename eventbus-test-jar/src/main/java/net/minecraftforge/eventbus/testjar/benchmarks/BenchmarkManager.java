package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BenchmarkManager {
    private BenchmarkManager() {}

    private static final MethodType RETURNS_CONSUMER = MethodType.methodType(Consumer.class);
    private static final MethodType CONSUMER_FI_TYPE = MethodType.methodType(void.class, Object.class);

    public static void validate(boolean shouldBeTransformed) {
        try {
            CancelableEvent.class.getDeclaredField("LISTENER_LIST");
            if (!shouldBeTransformed)
                throw new RuntimeException("LISTENER_LIST field exists!");
        } catch (Exception e) {
            if (shouldBeTransformed)
                throw new RuntimeException("Transformer did not apply!", e);
        }
    }

    public static Consumer<Blackhole> getPostingBenchmark(String name, int multiplier) {
        return switch (name) {
            case "modLauncherMixed" -> ModLauncherBenchmarks.Post.Factory.MIXED.create().apply(multiplier);
            case "modLauncherDynamic" -> ModLauncherBenchmarks.Post.Factory.DYNAMIC.create().apply(multiplier);
            case "modLauncherLambda" -> ModLauncherBenchmarks.Post.Factory.LAMBDA.create().apply(multiplier);
            case "modLauncherStatic" -> ModLauncherBenchmarks.Post.Factory.STATIC.create().apply(multiplier);

            case "noLoaderMixed" -> NoLoaderBenchmarks.Post.Factory.MIXED.create().apply(multiplier);
            case "noLoaderDynamic" -> NoLoaderBenchmarks.Post.Factory.DYNAMIC.create().apply(multiplier);
            case "noLoaderLambda" -> NoLoaderBenchmarks.Post.Factory.LAMBDA.create().apply(multiplier);
            case "noLoaderStatic" -> NoLoaderBenchmarks.Post.Factory.STATIC.create().apply(multiplier);

            default -> throw new IllegalArgumentException("Unknown benchmark: " + name);
        };
    }

    public static Runnable[] getRegistrationBenchmark(String name) {
        return switch (name) {
            case "modLauncherDynamic" -> new Runnable[] {
                    ModLauncherBenchmarks.Register.Dynamic::setupIteration,
                    ModLauncherBenchmarks.Register.Dynamic::run
            };
            case "modLauncherLambda" -> new Runnable[] {
                    ModLauncherBenchmarks.Register.Lambda::setupIteration,
                    ModLauncherBenchmarks.Register.Lambda::run
            };
            case "modLauncherStatic" -> new Runnable[] {
                    ModLauncherBenchmarks.Register.Static::setupIteration,
                    ModLauncherBenchmarks.Register.Static::run
            };

            case "noLoaderDynamic" -> new Runnable[] {
                    NoLoaderBenchmarks.Register.Dynamic::setupIteration,
                    NoLoaderBenchmarks.Register.Dynamic::run
            };
            case "noLoaderLambda" -> new Runnable[] {
                    NoLoaderBenchmarks.Register.Lambda::setupIteration,
                    NoLoaderBenchmarks.Register.Lambda::run
            };
            case "noLoaderStatic" -> new Runnable[] {
                    NoLoaderBenchmarks.Register.Static::setupIteration,
                    NoLoaderBenchmarks.Register.Static::run
            };

            default -> throw new IllegalArgumentException("Unknown benchmark: " + name);
        };
    }

    public static Consumer<Blackhole> setupPostingBenchmark(MethodHandles.Lookup lookup, Class<?> cls, int multiplier, Supplier<Consumer<IEventBus>> registrar) {
        try {
            // Register the requested multiplier of listeners
            lookup.findStatic(cls, "setup", MethodType.methodType(void.class, int.class, Supplier.class))
                    .invokeExact(multiplier, registrar);

            // Find the static `post(Blackhole)` method and return it as a `Consumer<Blackhole>` using LambdaMetaFactory
            var postMethod = lookup.findStatic(cls, "post", MethodType.methodType(void.class, Blackhole.class));
            var lmf = LambdaMetafactory.metafactory(lookup, "accept", RETURNS_CONSUMER, CONSUMER_FI_TYPE, postMethod, postMethod.type());

            @SuppressWarnings("unchecked")
            var consumer = (Consumer<Blackhole>) lmf.getTarget().invokeExact();
            return consumer;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
