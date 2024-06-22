/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.eventbus.testjar.benchmarks;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.testjar.events.CancelableEvent;

public class BenchmarkManager {
    private final Supplier<IEventBus> bus;
    private final Benchmark benchmark;
    public final Runnable setupIteration;
    public final Runnable run;
    private static Function<String, String> renamer;

    public BenchmarkManager(String benchmark, boolean shouldTransform, boolean ml, boolean wrapped) {
        this.validate(shouldTransform);
        if (ml) bus = () -> BusBuilder.builder().useModLauncher().build();
        else    bus = () -> BusBuilder.builder().build();
        renamer = getRenamer(wrapped);
        this.benchmark = getBenchmark(benchmark);
        this.benchmark.setup(bus);
        setupIteration = this.benchmark::setupIteration;
        run = this.benchmark::run;
    }

    private static Function<String, String> getRenamer(boolean wrapped) {
        if (!wrapped)
            return null;
        try {
            var mtd = IEventBus.class.getDeclaredMethod("rename", String.class);
            return name -> {
                try {
                    return (String)mtd.invoke(null, name);
                } catch (Exception e) {
                    return sneak(e);
                }
            };
        } catch (Exception e) {
            return sneak(e);
        }
    }

    private static Benchmark getBenchmark(String name) {
        switch (name) {
            case "registerDynamic": return new Register.Dynamic();
            case "registerLambda":  return new Register.Lambda();
            case "registerStatic":  return new Register.Static();

            case "postMixed":        return new Post.Mixed.Single();
            case "postMixedDozen":   return new Post.Mixed.Dozen();
            case "postMixedHundred": return new Post.Mixed.Hundred();

            case "postDynamic":        return new Post.Dynamic.Single();
            case "postDynamicDozen":   return new Post.Dynamic.Dozen();
            case "postDynamicHundred": return new Post.Dynamic.Hundred();

            case "postLambda":        return new Post.Lambda.Single();
            case "postLambdaDozen":   return new Post.Lambda.Dozen();
            case "postLambdaHundred": return new Post.Lambda.Hundred();

            case "postStatic":        return new Post.Static.Single();
            case "postStaticDozen":   return new Post.Static.Dozen();
            case "postStaticHundred": return new Post.Static.Hundred();

            case "postMixedRecord":        return new PostRecord.Mixed.Single();
            case "postMixedDozenRecord":   return new PostRecord.Mixed.Dozen();
            case "postMixedHundredRecord": return new PostRecord.Mixed.Hundred();

            case "postDynamicRecord":        return new PostRecord.Dynamic.Single();
            case "postDynamicDozenRecord":   return new PostRecord.Dynamic.Dozen();
            case "postDynamicHundredRecord": return new PostRecord.Dynamic.Hundred();

            case "postLambdaRecord":        return new PostRecord.Lambda.Single();
            case "postLambdaDozenRecord":   return new PostRecord.Lambda.Dozen();
            case "postLambdaHundredRecord": return new PostRecord.Lambda.Hundred();

            case "postStaticRecord":        return new PostRecord.Static.Single();
            case "postStaticDozenRecord":   return new PostRecord.Static.Dozen();
            case "postStaticHundredRecord": return new PostRecord.Static.Hundred();

        }
        throw new IllegalArgumentException("Invalid benchmark: " + name);
    }

    private void validate(boolean shouldTransform) {
        try {
            CancelableEvent.class.getDeclaredField("LISTENER_LIST");
            if (!shouldTransform)
                throw new RuntimeException("LISTENER_LIST field exists!");
        } catch (Exception e) {
            if (shouldTransform)
                throw new RuntimeException("Transformer did not apply!", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }

    public static String rename(String name) {
        return renamer == null ? name : renamer.apply(name);
    }
}
