/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test.compiletime;

import com.google.testing.compile.Compilation;
import net.minecraftforge.eventbus.test.BulkEventListenerTests;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;

public class SubscribeEventValidatorTests {
    @Language(value = "Java", suffix = "}")
    private static final String SOURCE_PREFIX = CompileTestHelper.SOURCE_PREFIX + "final class Listeners {";

    private static Compilation compile(@Language(value = "Java", prefix = SOURCE_PREFIX, suffix = "}") String sourceCode) {
        return CompileTestHelper.compileWithoutDefaultPrefix(SOURCE_PREFIX + sourceCode + "}");
    }

    /**
     * Tests that compile-time validation of parameter types on methods annotated with @SubscribeEvent works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongParameterType()
     */
    @Test
    public void testSubscribeEventParamTypes() {
        var compilation = compile("@SubscribeEvent void wrongFirstParamType(String notAnEvent) {}");
        assertThat(compilation).hadErrorContaining("must be an event");

        compilation = compile("@SubscribeEvent void correctFirstParam(EventWithData event) {}");
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation of parameter count on methods annotated with @SubscribeEvent works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongParameterCount()
     */
    @Test
    public void testSubscribeEventParamCount() {
        var compilation = compile("@SubscribeEvent void noParameters() {}");
        assertThat(compilation).hadErrorContaining("Invalid number of parameters");

        compilation = compile("@SubscribeEvent void tooManyParameters(int a, int b, int c) {}");
        assertThat(compilation).hadErrorContaining("Invalid number of parameters");
    }

    /**
     * Tests that compile-time validation of the return type on methods annotated with @SubscribeEvent works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongReturnType()
     */
    @Test
    public void testSubscribeEventReturnType() {
        var compilation = compile("@SubscribeEvent int invalidReturnType(EventWithData event) { return 0; }");
        assertThat(compilation).hadErrorContaining("expected void");
    }

    /**
     * Tests that compile-time validation of the return type on methods annotated with @SubscribeEvent for cancellable
     * events and listeners works correctly
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongReturnTypeCancellable()
     */
    @Test
    public void testSubscribeEventReturnTypeCancellable() {
        var compilation = compile("""
            @SubscribeEvent
            boolean invalidReturnType(EventWithData event) { return false; }
        """);
        assertThat(compilation).hadErrorContaining("boolean is only valid for cancellable events");

        compilation = compile("""
            @SubscribeEvent
            boolean neverCancellingEvent(CancelableEvent event) { return false; }
        """);
        assertThat(compilation).hadWarningContaining("consider using a void return type");

        compilation = compile("""
            @SubscribeEvent(alwaysCancelling = true)
            void alwaysCancellingEvent(CancelableEvent event) {}
        """);
        assertThat(compilation).succeededWithoutWarnings();

        compilation = compile("""
            static final java.util.Random RANDOM = new java.util.Random();
        
            @SubscribeEvent
            boolean possiblyCancellingEvent(CancelableEvent event) { return RANDOM.nextBoolean(); }
        """);
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation of the return type on methods annotated with @SubscribeEvent for monitoring
     * listeners works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongReturnTypeMonitoring()
     */
    @Test
    public void testSubscribeEventReturnTypeMonitoring() {
        var compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR)
            boolean possiblyCancellingMonitor(CancelableEvent event) { return false; }
        """);
        assertThat(compilation).hadErrorContaining("Monitoring listeners cannot cancel events");

        compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR, alwaysCancelling = true)
            void alwaysCancellingMonitor(CancelableEvent event) {}
        """);
        assertThat(compilation).hadErrorContaining("Monitoring listeners cannot cancel events");
    }

    /**
     * Tests that compile-time validation of the parameter type on methods annotated with @SubscribeEvent for monitoring
     * listeners works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongParamTypeMonitoring()
     */
    @Test
    public void testSubscribeEventParamTypeMonitoring() {
        var compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR)
            void monitoringListener(CancelableEvent event, int wrong) {}
        """);
        assertThat(compilation).hadErrorContaining("must be a boolean");

        compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR)
            void monitoringListener(CancelableEvent event) {}
        """);
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation of the parameter count on methods annotated with @SubscribeEvent for
     * cancellation-aware monitoring listeners works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongParamCountMonitoring()
     */
    @Test
    public void testSubscribeEventParamCountMonitoring() {
        var compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR)
            void monitoringListener(EventWithData event, boolean wasCancelled) {}
        """);
        assertThat(compilation).hadErrorContaining("Cancellation-aware monitoring listeners are only valid for cancellable events");

        compilation = compile("""
            @SubscribeEvent(priority = Priority.MONITOR)
            void cancellationAwareMonitoringListener(CancelableEvent event, boolean wasCancelled) {}
        """);
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation of the priority on methods annotated with @SubscribeEvent for
     * cancellation-aware monitoring listeners works correctly.
     * @see BulkEventListenerTests#testStrictBulkRegistrationValidationWrongPriorityMonitoring()
     */
    @Test
    public void testSubscribeEventPriorityMonitoring() {
        var compilation = compile("""
            @SubscribeEvent(priority = Priority.LOWEST)
            void cancellationAwareMonitoringListener(CancelableEvent event, boolean wasCancelled) {}
        """);
        assertThat(compilation).hadErrorContaining("must have a priority of MONITOR");
    }
}
