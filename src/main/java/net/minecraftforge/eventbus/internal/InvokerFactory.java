/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.internal;

import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.ObjBooleanBiConsumer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds static methods for creating specialised invokers from a list of listeners based on the event's characteristics
 * and the listeners' capabilities, to maximise performance.
 * <p>For instance, if an event implements {@link Cancellable} but none of the listeners are
 * capable of cancelling the event, we can unwrap them from their predicates, skip checking for cancelled and treat it
 * as if the event isn't cancellable.</p>
 */
final class InvokerFactory {
    private InvokerFactory() {}

    /**
     * Threshold for the number of listeners at which the optimisation to unwrap non-cancellable listeners is applied.
     * <p><u>When:</u></p>
     * <ol>
     *     <li>the number of listeners is less than or equal to this threshold</li>
     *     <li>the event is cancellable</li>
     *     <li>all listeners never cancel the event</li>
     * </ol>
     * <p><u>Then do:</u></p>
     * <ol>
     *     <li>Unwrap the individual listeners from their predicates back to consumers</li>
     *     <li>Wrap in a single consumer that always returns false</li>
     * </ol>
     * <p><u>Notes:</u></p>
     * <ul>
     *     <li>Setting to 0 will disable this optimisation.</li>
     *     <li>Setting too high can counter-intuitively slow down the event bus.</li>
     *     <li>Default aligns with the max sized manually unrolled loop for consumers, which seems to be a safe bet looking at
     *     the benchmarks, but needs more testing.</li>
     * </ul>
     */
    private static final int UNWRAP_CANCELLABLE_THRESHOLD = Integer.getInteger("eventbus.experimental.unwrapCancellableThreshold", 4);

    static <T extends Event> Consumer<T> createMonitoringInvoker(
            Class<T> eventType,
            int eventCharacteristics,
            List<EventListener> listeners,
            List<EventListener> monitoringListeners
    ) {
        Consumer<T> invoker = createInvoker(listeners);
        if (monitoringListeners.isEmpty())
            return invoker;

        List<ObjBooleanBiConsumer<T>> unwrappedMonitors = InvokerFactoryUtils.unwrapMonitors(monitoringListeners);

        if (Constants.isMonitorAware(eventCharacteristics)) {
            if (!MutableEvent.class.isAssignableFrom(eventType))
                throw new UnsupportedOperationException("This version of EventBus only supports " +
                        "EventCharacteristics.MonitorAware on MutableEvent");

            // If there's only one monitoring listener, invoke it directly without setting up an iterator/loop
            if (monitoringListeners.size() == 1) {
                var firstMonitor = unwrappedMonitors.getFirst();
                return event -> {
                    invoker.accept(event);
                    var mutableEvent = (MutableEventInternals) event;
                    mutableEvent.isMonitoring = true;
                    firstMonitor.accept(event, false);
                    mutableEvent.isMonitoring = false;
                };
            } else {
                @SuppressWarnings("unchecked")
                ObjBooleanBiConsumer<T>[] unwrappedMonitorsArray = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
                return event -> {
                    invoker.accept(event);
                    var mutableEvent = (MutableEventInternals) event;
                    mutableEvent.isMonitoring = true;
                    for (var monitor : unwrappedMonitorsArray) {
                        monitor.accept(event, false);
                    }
                    mutableEvent.isMonitoring = false;
                };
            }
        }

        // same as above but without notifying the event that it's being monitored
        if (monitoringListeners.size() == 1) {
            var firstMonitor = unwrappedMonitors.getFirst();
            return event -> {
                invoker.accept(event);
                firstMonitor.accept(event, false);
            };
        } else {
            @SuppressWarnings("unchecked")
            ObjBooleanBiConsumer<T>[] unwrappedMonitorsArray = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
            return event -> {
                invoker.accept(event);
                for (var monitor : unwrappedMonitorsArray) {
                    monitor.accept(event, false);
                }
            };
        }
    }

    static <T extends Event & Cancellable> Predicate<T> createCancellableMonitoringInvoker(
            Class<T> eventType,
            int eventCharacteristics,
            List<EventListener> listeners,
            List<EventListener> monitoringListeners
    ) {
        Predicate<T> cancellableInvoker = createCancellableInvoker(listeners);
        if (monitoringListeners.isEmpty())
            return cancellableInvoker;

        List<ObjBooleanBiConsumer<T>> unwrappedMonitors = InvokerFactoryUtils.unwrapMonitors(monitoringListeners);

        if (Constants.isMonitorAware(eventCharacteristics)) {
            if (!MutableEvent.class.isAssignableFrom(eventType))
                throw new UnsupportedOperationException("This version of EventBus only supports " +
                        "EventCharacteristics.MonitorAware on MutableEvent");

            // If there's only one monitoring listener, invoke it directly without setting up an iterator/loop
            if (monitoringListeners.size() == 1) {
                var firstMonitor = unwrappedMonitors.getFirst();
                return event -> {
                    boolean cancelled = cancellableInvoker.test(event);
                    var mutableEvent = (MutableEventInternals) event;
                    mutableEvent.isMonitoring = true;
                    firstMonitor.accept(event, cancelled);
                    mutableEvent.isMonitoring = false;
                    return cancelled;
                };
            } else {
                @SuppressWarnings("unchecked")
                ObjBooleanBiConsumer<T>[] unwrappedMonitorsArray = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
                return event -> {
                    boolean cancelled = cancellableInvoker.test(event);
                    var mutableEvent = (MutableEventInternals) event;
                    mutableEvent.isMonitoring = true;
                    for (var monitor : unwrappedMonitorsArray) {
                        monitor.accept(event, cancelled);
                    }
                    mutableEvent.isMonitoring = false;
                    return cancelled;
                };
            }
        }

        // same as above but without notifying the event that it's being monitored
        if (monitoringListeners.size() == 1) {
            var firstMonitor = unwrappedMonitors.getFirst();
            return event -> {
                boolean cancelled = cancellableInvoker.test(event);
                firstMonitor.accept(event, cancelled);
                return cancelled;
            };
        } else {
            @SuppressWarnings("unchecked")
            ObjBooleanBiConsumer<T>[] unwrappedMonitorsArray = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
            return event -> {
                boolean cancelled = cancellableInvoker.test(event);
                for (var monitor : unwrappedMonitorsArray) {
                    monitor.accept(event, cancelled);
                }
                return cancelled;
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Consumer<T> createInvoker(List<EventListener> listeners) {
        return createInvokerFromUnwrapped((List<Consumer<T>>) (List) InvokerFactoryUtils.unwrapConsumers(listeners));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event & Cancellable> Predicate<T> createCancellableInvoker(List<EventListener> listeners) {
        // If none of the listeners are able to cancel the event, we can remove the overhead of checking for cancellation entirely
        // by treating it like a non-cancellable event.
        if (listeners.size() <= UNWRAP_CANCELLABLE_THRESHOLD
                && listeners.stream().allMatch(EventListenerImpl.WrappedConsumerListener.class::isInstance)) {
            // We can't do this because the JVM seems to hate wrapping a lambda inside another lambda of a different type (4-5x slower)...
            // Microsoft\jdk-21.0.4.7-hotspot
//            Consumer<T> invoker = createInvoker(listeners);
//            return event -> {
//                invoker.accept(event);
//                return false;
//            };

            // ...so annoyingly, we need to duplicate the code of createInvoker() here, but with a different primitive return type (boolean instead of void)
            // Maybe JEP 402 can save us from this workaround in the future? https://openjdk.java.net/jeps/402

            return createCancellableInvokerFromUnwrappedNoChecks(
                    (List<Consumer<T>>) (List) InvokerFactoryUtils.unwrapAlwaysCancellingConsumers(listeners),
                    listeners.stream()
                            .map(EventListenerImpl.WrappedConsumerListener.class::cast)
                            .anyMatch(EventListenerImpl.WrappedConsumerListener::alwaysCancelling)
            );
        }

        return createCancellableInvokerFromUnwrapped((List<Predicate<T>>) (List) InvokerFactoryUtils.unwrapPredicates(listeners));
    }

    private static <T extends Event> Consumer<T> createInvokerFromUnwrapped(List<Consumer<T>> listeners) {
        return switch (listeners.size()) {
            case 0 -> Constants.getNoOpConsumer(); // No-op
            case 1 -> listeners.getFirst(); // Direct call
            case 2 -> {
                var first = listeners.getFirst();
                var second = listeners.getLast();
                yield first.andThen(second);
            }

            case 3 -> {
                var first = listeners.getFirst(); // 0
                var second = listeners.get(1);
                var third = listeners.getLast(); // 2
                yield event -> {
                    first.accept(event);
                    second.accept(event);
                    third.accept(event);
                };
            }
            case 4 -> {
                var first = listeners.getFirst(); // 0
                var second = listeners.get(1);
                var third = listeners.get(2);
                var fourth = listeners.getLast(); // 3
                yield event -> {
                    first.accept(event);
                    second.accept(event);
                    third.accept(event);
                    fourth.accept(event);
                };
            }

            default -> {
                @SuppressWarnings("unchecked")
                Consumer<T>[] listenersArray = listeners.toArray(new Consumer[0]);
                yield event -> {
                    for (Consumer<T> listener : listenersArray) {
                        listener.accept(event);
                    }
                };
            }
        };
    }

    /**
     * Same as {@link #createInvokerFromUnwrapped(List)} but returns a {@link Predicate} instead of a {@link Consumer}.
     * <p>See the code comments inside {@link #createCancellableInvoker(List)} for an explainer as to why this exists.</p>
     * <p>Also see {@link EventListenerImpl.WrappedConsumerListener#wrap(boolean, Consumer)} for an explainer as to why capturing the return value is avoided.</p>
     */
    private static <T extends Event & Cancellable> Predicate<T> createCancellableInvokerFromUnwrappedNoChecks(List<Consumer<T>> listeners, boolean alwaysCancelling) {
        if (alwaysCancelling) {
            return switch (listeners.size()) {
                case 0 -> Constants.getNoOpPredicate(true);
                case 1 -> {
                    var first = listeners.getFirst();
                    yield event -> {
                        first.accept(event);
                        return true;
                    };
                }
                case 2 -> {
                    var first = listeners.getFirst();
                    var second = listeners.getLast();
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        return true;
                    };
                }

                case 3 -> {
                    var first = listeners.getFirst(); // 0
                    var second = listeners.get(1);
                    var third = listeners.getLast(); // 2
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        third.accept(event);
                        return true;
                    };
                }
                case 4 -> {
                    var first = listeners.getFirst(); // 0
                    var second = listeners.get(1);
                    var third = listeners.get(2);
                    var fourth = listeners.getLast(); // 3
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        third.accept(event);
                        fourth.accept(event);
                        return true;
                    };
                }

                default -> {
                    @SuppressWarnings("unchecked")
                    Consumer<T>[] listenersArray = listeners.toArray(new Consumer[0]);
                    yield event -> {
                        for (Consumer<T> listener : listenersArray) {
                            listener.accept(event);
                        }
                        return true;
                    };
                }
            };
        } else {
            return switch (listeners.size()) {
                case 0 -> Constants.getNoOpPredicate(false);
                case 1 -> {
                    var first = listeners.getFirst();
                    yield event -> {
                        first.accept(event);
                        return false;
                    };
                }
                case 2 -> {
                    var first = listeners.getFirst();
                    var second = listeners.getLast();
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        return false;
                    };
                }

                case 3 -> {
                    var first = listeners.getFirst(); // 0
                    var second = listeners.get(1);
                    var third = listeners.getLast(); // 2
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        third.accept(event);
                        return false;
                    };
                }
                case 4 -> {
                    var first = listeners.getFirst(); // 0
                    var second = listeners.get(1);
                    var third = listeners.get(2);
                    var fourth = listeners.getLast(); // 3
                    yield event -> {
                        first.accept(event);
                        second.accept(event);
                        third.accept(event);
                        fourth.accept(event);
                        return false;
                    };
                }

                default -> {
                    @SuppressWarnings("unchecked")
                    Consumer<T>[] listenersArray = listeners.toArray(new Consumer[0]);
                    yield event -> {
                        for (Consumer<T> listener : listenersArray) {
                            listener.accept(event);
                        }
                        return false;
                    };
                }
            };
        }
    }

    private static <T extends Event & Cancellable> Predicate<T> createCancellableInvokerFromUnwrapped(List<Predicate<T>> listeners) {
        return switch (listeners.size()) {
            case 0 -> Constants.getNoOpPredicate(false);
            case 1 -> listeners.getFirst(); // Direct call
            case 2 -> {
                var first = listeners.getFirst();
                var second = listeners.getLast();
                yield first.or(second);
            }

            case 3 -> {
                var first = listeners.getFirst(); // 0
                var second = listeners.get(1);
                var third = listeners.getLast(); // 2
                yield event -> first.test(event) || second.test(event) || third.test(event);
            }

            default -> {
                @SuppressWarnings("unchecked")
                Predicate<T>[] listenersArray = listeners.toArray(new Predicate[0]);
                yield event -> {
                    for (Predicate<T> listener : listenersArray) {
                        if (listener.test(event))
                            return true;
                    }
                    return false;
                };
            }
        };
    }
}
