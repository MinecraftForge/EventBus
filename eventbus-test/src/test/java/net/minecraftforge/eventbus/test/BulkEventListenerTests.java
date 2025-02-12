/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.eventbus.internal.BusGroupImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BulkEventListenerTests {
    public record TestEvent() implements RecordEvent {
        static final EventBus<TestEvent> BUS = EventBus.create(TestEvent.class);
    }

    public record CancellableTestEvent() implements Cancellable, RecordEvent {
        static final CancellableEventBus<CancellableTestEvent> BUS = CancellableEventBus.create(CancellableTestEvent.class);
    }

    private static final MethodHandle REGISTER_STRICT_METHOD;
    static {
        try {
            var clazz = Class.forName("net.minecraftforge.eventbus.internal.EventListenerFactory");
            var method = clazz.getDeclaredMethod("registerStrict", BusGroupImpl.class, MethodHandles.Lookup.class, Class.class, Object.class);
            method.setAccessible(true);
            REGISTER_STRICT_METHOD = MethodHandles.lookup().unreflect(method);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    private static void registerStrict(MethodHandles.Lookup callerLookup, Class<?> listenerClass, @Nullable Object instance) {
        try {
            REGISTER_STRICT_METHOD.invoke((BusGroupImpl) BusGroup.DEFAULT, callerLookup, listenerClass, instance);
        } catch (Throwable e) {
            sneak(e);
        }
    }
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneak(Throwable e) throws E {
        throw (E) e;
    }

    public static class StaticListeners {
        static volatile boolean privateCalled;
        static volatile boolean protectedCalled;
        static volatile boolean packageCalled;
        static volatile boolean publicCalled;

        @SubscribeEvent
        private static void privateListener(TestEvent event) {
            privateCalled = true;
        }

        @SubscribeEvent
        protected static void protectedListener(TestEvent event) {
            protectedCalled = true;
        }

        @SubscribeEvent
        static void packageListener(TestEvent event) {
            packageCalled = true;
        }

        @SubscribeEvent
        public static void publicListener(TestEvent event) {
            publicCalled = true;
        }

        static void reset() {
            privateCalled = false;
            protectedCalled = false;
            packageCalled = false;
            publicCalled = false;
        }
    }

    /**
     * Tests that bulk registered static listeners of different access modifiers are called when the event is posted.
     */
    @Test
    public void testBulkRegisteredStaticListenersAreCalled() {
        StaticListeners.reset();

        AtomicReference<Collection<EventListener>> listeners = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> listeners.set(BusGroup.DEFAULT.register(MethodHandles.lookup(), StaticListeners.class)),
                "Failed to bulk register static listeners"
        );
        Assertions.assertNotNull(listeners.get(), "Bulk registered listeners should not be null");
        Assertions.assertFalse(listeners.get().isEmpty(), "Bulk registered listeners should not be empty");
        Assertions.assertEquals(4, listeners.get().size(), "There should be four listeners registered");

        Assertions.assertFalse(StaticListeners.privateCalled, "Private listener should not have been called yet");
        Assertions.assertFalse(StaticListeners.protectedCalled, "Protected listener should not have been called yet");
        Assertions.assertFalse(StaticListeners.packageCalled, "Package listener should not have been called yet");
        Assertions.assertFalse(StaticListeners.publicCalled, "Public listener should not have been called yet");

        TestEvent.BUS.post(new TestEvent());

        Assertions.assertTrue(StaticListeners.privateCalled, "Private listener should have been called");
        Assertions.assertTrue(StaticListeners.protectedCalled, "Protected listener should have been called");
        Assertions.assertTrue(StaticListeners.packageCalled, "Package listener should have been called");
        Assertions.assertTrue(StaticListeners.publicCalled, "Public listener should have been called");
    }

    public static class InstanceListeners {
        volatile boolean privateCalled;
        volatile boolean protectedCalled;
        volatile boolean packageCalled;
        volatile boolean publicCalled;

        @SubscribeEvent
        private void privateListener(TestEvent event) {
            privateCalled = true;
        }

        @SubscribeEvent
        protected void protectedListener(TestEvent event) {
            protectedCalled = true;
        }

        @SubscribeEvent
        void packageListener(TestEvent event) {
            packageCalled = true;
        }

        @SubscribeEvent
        public void publicListener(TestEvent event) {
            publicCalled = true;
        }
    }

    /**
     * Tests that bulk registered instance listeners of different access modifiers are called when the event is posted.
     */
    @Test
    public void testBulkRegisteredInstanceListenersAreCalled() {
        var instanceListeners = new InstanceListeners();
        AtomicReference<Collection<EventListener>> listeners = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> listeners.set(BusGroup.DEFAULT.register(MethodHandles.lookup(), instanceListeners)),
                "Failed to bulk register instance listeners"
        );
        Assertions.assertNotNull(listeners.get(), "Bulk registered listeners should not be null");
        Assertions.assertFalse(listeners.get().isEmpty(), "Bulk registered listeners should not be empty");
        Assertions.assertEquals(4, listeners.get().size(), "There should be four listeners registered");

        Assertions.assertFalse(instanceListeners.privateCalled, "Private listener should not have been called yet");
        Assertions.assertFalse(instanceListeners.protectedCalled, "Protected listener should not have been called yet");
        Assertions.assertFalse(instanceListeners.packageCalled, "Package listener should not have been called yet");
        Assertions.assertFalse(instanceListeners.publicCalled, "Public listener should not have been called yet");

        TestEvent.BUS.post(new TestEvent());

        Assertions.assertTrue(instanceListeners.privateCalled, "Private listener should have been called");
        Assertions.assertTrue(instanceListeners.protectedCalled, "Protected listener should have been called");
        Assertions.assertTrue(instanceListeners.packageCalled, "Package listener should have been called");
        Assertions.assertTrue(instanceListeners.publicCalled, "Public listener should have been called");
    }

    public static class MixedListeners {
        volatile boolean instanceCalled;
        static volatile boolean staticCalled;

        @SubscribeEvent
        public void instanceListener(TestEvent event) {
            instanceCalled = true;
        }

        @SubscribeEvent
        public static void staticListener(TestEvent event) {
            staticCalled = true;
        }

        static void reset() {
            staticCalled = false;
        }
    }

    /**
     * Tests that bulk registered listeners of both static and instance types are called when the event is posted.
     * <p>When registering an instance class, it should register the static methods inside it as well.</p>
     */
    @Test
    public void testBulkRegisteredMixedListenersAreCalled() {
        MixedListeners.reset();

        var mixedListeners = new MixedListeners();
        AtomicReference<Collection<EventListener>> listeners = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> listeners.set(BusGroup.DEFAULT.register(MethodHandles.lookup(), mixedListeners)),
                "Failed to bulk register mixed listeners"
        );
        Assertions.assertNotNull(listeners.get(), "Bulk registered listeners should not be null");
        Assertions.assertFalse(listeners.get().isEmpty(), "Bulk registered listeners should not be empty");
        Assertions.assertEquals(2, listeners.get().size(), "There should be two listeners registered");

        Assertions.assertFalse(mixedListeners.instanceCalled, "Instance listener should not have been called yet");
        Assertions.assertFalse(MixedListeners.staticCalled, "Static listener should not have been called yet");

        TestEvent.BUS.post(new TestEvent());

        Assertions.assertTrue(mixedListeners.instanceCalled, "Instance listener should have been called");
        Assertions.assertTrue(MixedListeners.staticCalled, "Static listener should have been called");
    }

    public interface InterfaceStaticListeners {
        AtomicBoolean privateCalled = new AtomicBoolean();
        AtomicBoolean publicCalled = new AtomicBoolean();

        @SubscribeEvent
        private static void privateListener(TestEvent event) {
            privateCalled.set(true);
        }

        @SubscribeEvent
        static void publicListener(TestEvent event) {
            publicCalled.set(true);
        }

        static void reset() {
            privateCalled.set(false);
            publicCalled.set(false);
        }
    }

    /**
     * Tests that bulk registered static listeners inside interfaces are called when the event is posted.
     */
    @Test
    public void testBulkRegisteredInterfaceStaticListenersAreCalled() {
        InterfaceStaticListeners.reset();

        AtomicReference<Collection<EventListener>> listeners = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> listeners.set(BusGroup.DEFAULT.register(MethodHandles.lookup(), InterfaceStaticListeners.class)),
                "Failed to bulk register static listeners in interface"
        );
        Assertions.assertNotNull(listeners.get(), "Bulk registered listeners should not be null");
        Assertions.assertFalse(listeners.get().isEmpty(), "Bulk registered listeners should not be empty");
        Assertions.assertEquals(2, listeners.get().size(), "There should be two listeners registered");

        Assertions.assertFalse(InterfaceStaticListeners.privateCalled.get(), "Private listener should not have been called yet");
        Assertions.assertFalse(InterfaceStaticListeners.publicCalled.get(), "Public listener should not have been called yet");

        TestEvent.BUS.post(new TestEvent());

        Assertions.assertTrue(InterfaceStaticListeners.privateCalled.get(), "Private listener should have been called");
        Assertions.assertTrue(InterfaceStaticListeners.publicCalled.get(), "Public listener should have been called");
    }

    public static class VariantListeners {
        static volatile boolean cancellableCalled;
        static volatile boolean alwaysCancellingCalled;
        static volatile boolean monitoringCalled;
        static volatile boolean cancellationAwareMonitoringCalled;
        static volatile boolean lowPriorityCalled;
        static volatile boolean highPriorityCalled;

        @SubscribeEvent
        public static boolean cancellableListener(CancellableTestEvent event) {
            cancellableCalled = true;
            return false;
        }

        @SubscribeEvent(alwaysCancelling = true, priority = -100)
        public static void alwaysCancellingListener(CancellableTestEvent event) {
            alwaysCancellingCalled = true;
        }

        @SubscribeEvent(priority = Priority.MONITOR)
        public static void monitoringListener(TestEvent event) {
            monitoringCalled = true;
        }

        @SubscribeEvent
        public static void cancellationAwareMonitoringListener(CancellableTestEvent event, boolean wasCancelled) {
            cancellationAwareMonitoringCalled = true;
        }

        @SubscribeEvent(priority = Priority.LOW)
        public static void lowPriorityListener(TestEvent event) {
            lowPriorityCalled = true;
        }

        @SubscribeEvent(priority = Priority.HIGH)
        public static void highPriorityListener(TestEvent event) {
            highPriorityCalled = true;
        }

        @SubscribeEvent(priority = Priority.LOWEST)
        public static void neverCalledListener(CancellableTestEvent event) {
            Assertions.fail("This listener should never be called because the alwaysCancellingListener should've cancelled the event before me");
        }

        static void reset() {
            cancellableCalled = false;
            alwaysCancellingCalled = false;
            monitoringCalled = false;
            cancellationAwareMonitoringCalled = false;
            lowPriorityCalled = false;
            highPriorityCalled = false;
        }
    }

    /**
     * Tests that cancellable, monitoring and priority variants of listeners are registered and called correctly.
     */
    @Test
    public void testBulkRegisteredVarietyListeners() {
        VariantListeners.reset();

        AtomicReference<Collection<EventListener>> listeners = new AtomicReference<>();
        Assertions.assertDoesNotThrow(
                () -> listeners.set(BusGroup.DEFAULT.register(MethodHandles.lookup(), VariantListeners.class)),
                "Failed to bulk register variant listeners"
        );
        Assertions.assertNotNull(listeners.get(), "Bulk registered listeners should not be null");
        Assertions.assertFalse(listeners.get().isEmpty(), "Bulk registered listeners should not be empty");
        Assertions.assertEquals(7, listeners.get().size(), "There should be six listeners registered");
        Assertions.assertTrue(
                listeners.get().stream().anyMatch(EventListener::alwaysCancelling),
                "There should be an always cancelling listener registered"
        );
        Assertions.assertTrue(
                listeners.get().stream().anyMatch(listener -> listener.priority() == Priority.LOW),
                "There should be a low priority listener registered"
        );
        Assertions.assertTrue(
                listeners.get().stream().anyMatch(listener -> listener.priority() == Priority.HIGH),
                "There should be a high priority listener registered"
        );
        Assertions.assertEquals(
                2,
                listeners.get().stream().filter(listener -> listener.priority() == Priority.MONITOR).count(),
                "There should be two monitoring listeners registered"
        );

        Assertions.assertFalse(VariantListeners.cancellableCalled, "Cancellable listener should not have been called yet");
        Assertions.assertFalse(VariantListeners.alwaysCancellingCalled, "Always cancelling listener should not have been called yet");
        Assertions.assertFalse(VariantListeners.monitoringCalled, "Monitoring listener should not have been called yet");
        Assertions.assertFalse(VariantListeners.cancellationAwareMonitoringCalled, "Cancellation aware monitoring listener should not have been called yet");
        Assertions.assertFalse(VariantListeners.lowPriorityCalled, "Low priority listener should not have been called yet");
        Assertions.assertFalse(VariantListeners.highPriorityCalled, "High priority listener should not have been called yet");

        TestEvent.BUS.post(new TestEvent());
        CancellableTestEvent.BUS.post(new CancellableTestEvent());

        Assertions.assertTrue(VariantListeners.cancellableCalled, "Cancellable listener should have been called");
        Assertions.assertTrue(VariantListeners.alwaysCancellingCalled, "Always cancelling listener should have been called");
        Assertions.assertTrue(VariantListeners.monitoringCalled, "Monitoring listener should have been called");
        Assertions.assertTrue(VariantListeners.cancellationAwareMonitoringCalled, "Cancellation aware monitoring listener should have been called");
        Assertions.assertTrue(VariantListeners.lowPriorityCalled, "Low priority listener should have been called");
        Assertions.assertTrue(VariantListeners.highPriorityCalled, "High priority listener should have been called");
    }

    public static class EncapsulatedListeners {
        @SubscribeEvent
        private static void encapsulatedListener(TestEvent event) {
            Assertions.fail("Encapsulated listener should not be called");
        }

        @SubscribeEvent
        private void encapsulatedInstanceListener(TestEvent event) {
            Assertions.fail("Encapsulated instance listener should not be called");
        }
    }

    /**
     * Tests that encapsulation is respected when requested during bulk registration.
     */
    @Test
    public void testBulkRegistrationEncapsulation() {
        Assertions.assertThrows(
                Exception.class,
                () -> BusGroup.DEFAULT.register(MethodHandles.publicLookup(), new EncapsulatedListeners()),
                "Private listeners should not be accessible with a public lookup"
        );
    }

    /**
     * Tests that strict bulk registration on a class with no methods throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationNoMethods() {
        final class NoMethods {}

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), NoMethods.class, null),
                "Attempting to bulk register a class with no methods should throw an exception"
        );

        final class NoListenerMethods {
            static void utilityMethod(int x, int y) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), NoListenerMethods.class, null),
                "Attempting to bulk register a class with no listener methods should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with instance event listener methods without an instance throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationInstanceMethodsWithoutInstance() {
        final class InstanceMethods {
            @SubscribeEvent
            void instanceMethod(TestEvent event) {}

            @SubscribeEvent
            static void staticMethod(TestEvent event) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), InstanceMethods.class, null),
                "Attempting to bulk register a class with instance methods without an instance should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with event listener methods missing the @SubscribeEvent annotation throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationMissingSubscribeEvent() {
        final class MissingSubscribeEvent {
            void noAnnotation(TestEvent event) {}
            static void noAnnotationStatic(TestEvent event) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), MissingSubscribeEvent.class, new MissingSubscribeEvent()),
                "Attempting to bulk register a class containing event handlers without the @SubscribeEvent annotation should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with event listener methods with the wrong parameter type throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongParameterType() {
        final class WrongParameterType {
            @SubscribeEvent
            void wrongParameterType(boolean notAnEvent) {}

            @SubscribeEvent
            static void wrongParameterTypeStatic(boolean notAnEvent) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongParameterType.class, new WrongParameterType()),
                "Attempting to bulk register a class containing @SubscribeEvent with the wrong parameter type should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with event listener methods with the wrong parameter count throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongParameterCount() {
        final class ZeroParameters {
            @SubscribeEvent
            void wrongParameterCount() {}

            @SubscribeEvent
            static void wrongParameterCountStatic() {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), ZeroParameters.class, new ZeroParameters()),
                "Attempting to bulk register a class containing @SubscribeEvent with zero parameters should throw an exception"
        );

        final class TooManyParameters {
            @SubscribeEvent
            void wrongParameterCount(TestEvent event, int a, int b) {}

            @SubscribeEvent
            static void wrongParameterCountStatic(TestEvent event, int a, int b) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), TooManyParameters.class, new TooManyParameters()),
                "Attempting to bulk register a class containing @SubscribeEvent with too many parameters should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with event listener methods with the wrong return type throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongReturnType() {
        final class WrongReturnType {
            @SubscribeEvent
            int wrongReturnType(TestEvent event) {
                return 0;
            }

            @SubscribeEvent
            static String wrongReturnTypeStatic(TestEvent event) {
                return "";
            }
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongReturnType.class, new WrongReturnType()),
                "Attempting to bulk register a class containing @SubscribeEvent with the wrong return type should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with event listener methods with the wrong return type throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongReturnTypeCancellable() {
        final class WrongReturnTypeCancellable {
            @SubscribeEvent
            boolean wrongReturnType(TestEvent event) {
                return false;
            }

            @SubscribeEvent
            static boolean wrongReturnTypeStatic(TestEvent event) {
                return false;
            }
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongReturnTypeCancellable.class, new WrongReturnTypeCancellable()),
                "Attempting to bulk register a class containing a possibly cancelling listener on a non-cancellable event should throw an exception"
        );

        final class WrongReturnTypeAlwaysCancelling {
            @SubscribeEvent(alwaysCancelling = true)
            boolean wrongReturnType(CancellableTestEvent event) {
                return false;
            }

            @SubscribeEvent(alwaysCancelling = true)
            static boolean wrongReturnTypeStatic(CancellableTestEvent event) {
                return false;
            }
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongReturnTypeAlwaysCancelling.class, new WrongReturnTypeAlwaysCancelling()),
                "Attempting to bulk register a class containing a always cancelling listener with a non-void return type should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with monitoring event listener methods with the wrong return type throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongReturnTypeMonitoring() {
        final class PossiblyCancellingMonitorListeners {
            @SubscribeEvent(priority = Priority.MONITOR)
            boolean possiblyCancellingMonitor(CancellableTestEvent event) {
                return false;
            }

            @SubscribeEvent(priority = Priority.MONITOR)
            static boolean wrongReturnTypeStatic(CancellableTestEvent event) {
                return false;
            }
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), PossiblyCancellingMonitorListeners.class, new PossiblyCancellingMonitorListeners()),
                "Attempting to bulk register a class containing a possibly cancelling monitoring listener should throw an exception"
        );

        final class AlwaysCancellingMonitoringListeners {
            @SubscribeEvent(alwaysCancelling = true, priority = Priority.MONITOR)
            void alwaysCancellingMonitor(CancellableTestEvent event) {}

            @SubscribeEvent(alwaysCancelling = true, priority = Priority.MONITOR)
            static void alwaysCancellingMonitorStatic(CancellableTestEvent event) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), AlwaysCancellingMonitoringListeners.class, new AlwaysCancellingMonitoringListeners()),
                "Attempting to bulk register a class containing an always cancelling monitoring listener should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with monitoring event listener methods with the wrong parameter type throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongParamTypeMonitoring() {
        final class WrongParamTypeCancelAwareMonitoring {
            @SubscribeEvent(priority = Priority.MONITOR)
            void wrongParamType(CancellableTestEvent event, int wrong) {}

            @SubscribeEvent(priority = Priority.MONITOR)
            static void wrongParamTypeStatic(CancellableTestEvent event, String wrong) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongParamTypeCancelAwareMonitoring.class, new WrongParamTypeCancelAwareMonitoring()),
                "Attempting to bulk register a class containing a cancellation-aware monitoring listener with the wrong parameter type should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registration on a class with cancellation-aware monitoring event listener methods on non-cancellable events throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongParamCountMonitoring() {
        final class WrongParamCountMonitoring {
            @SubscribeEvent(priority = Priority.MONITOR)
            void wrongParamCount(TestEvent event, boolean wrong) {}

            @SubscribeEvent(priority = Priority.MONITOR)
            static void wrongParamCountStatic(TestEvent event, boolean wrong) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongParamCountMonitoring.class, new WrongParamCountMonitoring()),
                "Attempting to bulk register a class containing a cancellation-aware monitoring listener for a non-cancellable event should throw an exception"
        );
    }

    /**
     * Tests that strict bulk registeration on a class with a non-monitoring priority on a cancellation-aware monitoring event listener method throws an exception.
     */
    @Test
    public void testStrictBulkRegistrationValidationWrongPriorityMonitoring() {
        final class WrongPriorityMonitoring {
            @SubscribeEvent(priority = Priority.HIGHEST)
            void wrongPriority(CancellableTestEvent event, boolean wasCancelled) {}

            @SubscribeEvent(priority = Priority.LOWEST)
            static void wrongPriorityStatic(CancellableTestEvent event, boolean wasCancelled) {}
        }

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> registerStrict(MethodHandles.lookup(), WrongPriorityMonitoring.class, new WrongPriorityMonitoring()),
                "Attempting to bulk register a class containing a cancellation-aware monitoring listener with a non-monitoring priority should throw an exception"
        );
    }
}
