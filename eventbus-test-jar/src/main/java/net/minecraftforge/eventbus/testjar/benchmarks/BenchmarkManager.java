package net.minecraftforge.eventbus.testjar.benchmarks;

import net.minecraftforge.eventbus.testjar.events.CancelableEvent;
import org.openjdk.jmh.infra.Blackhole;

import java.util.function.Consumer;

public final class BenchmarkManager {
    private BenchmarkManager() {}

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
            case "modLauncherMixed" -> switch (multiplier) {
                case 1 -> ModLauncherBenchmarks.Post.Mixed.Single::post;
                case 12 -> ModLauncherBenchmarks.Post.Mixed.Dozen::post;
                case 100 -> ModLauncherBenchmarks.Post.Mixed.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "modLauncherDynamic" -> switch (multiplier) {
                case 1 -> ModLauncherBenchmarks.Post.Dynamic.Single::post;
                case 12 -> ModLauncherBenchmarks.Post.Dynamic.Dozen::post;
                case 100 -> ModLauncherBenchmarks.Post.Dynamic.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "modLauncherLambda" -> switch (multiplier) {
                case 1 -> ModLauncherBenchmarks.Post.Lambda.Single::post;
                case 12 -> ModLauncherBenchmarks.Post.Lambda.Dozen::post;
                case 100 -> ModLauncherBenchmarks.Post.Lambda.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "modLauncherStatic" -> switch (multiplier) {
                case 1 -> ModLauncherBenchmarks.Post.Static.Single::post;
                case 12 -> ModLauncherBenchmarks.Post.Static.Dozen::post;
                case 100 -> ModLauncherBenchmarks.Post.Static.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };

            case "noLoaderMixed" -> switch (multiplier) {
                case 1 -> NoLoaderBenchmarks.Post.Mixed.Single::post;
                case 12 -> NoLoaderBenchmarks.Post.Mixed.Dozen::post;
                case 100 -> NoLoaderBenchmarks.Post.Mixed.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "noLoaderDynamic" -> switch (multiplier) {
                case 1 -> NoLoaderBenchmarks.Post.Dynamic.Single::post;
                case 12 -> NoLoaderBenchmarks.Post.Dynamic.Dozen::post;
                case 100 -> NoLoaderBenchmarks.Post.Dynamic.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "noLoaderLambda" -> switch (multiplier) {
                case 1 -> NoLoaderBenchmarks.Post.Lambda.Single::post;
                case 12 -> NoLoaderBenchmarks.Post.Lambda.Dozen::post;
                case 100 -> NoLoaderBenchmarks.Post.Lambda.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };
            case "noLoaderStatic" -> switch (multiplier) {
                case 1 -> NoLoaderBenchmarks.Post.Static.Single::post;
                case 12 -> NoLoaderBenchmarks.Post.Static.Dozen::post;
                case 100 -> NoLoaderBenchmarks.Post.Static.Hundred::post;
                default -> throw unsupportedMultiplier(multiplier);
            };

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

    private static UnsupportedOperationException unsupportedMultiplier(int multiplier) {
        return new UnsupportedOperationException("Unsupported multiplier: " + multiplier + ". Supported: 1, 12, 100");
    }
}
