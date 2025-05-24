/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test.compiletime;

import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static net.minecraftforge.eventbus.test.compiletime.CompileTestHelper.compile;

public class EventTypeValidatorTests {
    /**
     * Tests that compile-time validation throws an error for non-record classes that implement RecordEvent.
     */
    @Test
    public void testRecordEventValidation() {
        var compilation = compile("final class ClassTestEvent implements RecordEvent {}");
        assertThat(compilation).hadErrorContaining("implements RecordEvent but is not a record class");

        compilation = compile("record RecordTestEvent() implements RecordEvent {}");
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation throws an error for MonitorAware on classes that do not extend MutableEvent.
     */
    @Test
    public void testMonitorAwareValidation() {
        var compilation = compile("record RecordTestEvent() implements RecordEvent, MonitorAware {}");
        assertThat(compilation).hadErrorContaining("implements MonitorAware but is not a mutable event");

        compilation = compile("final class ClassTestEvent extends MutableEvent implements MonitorAware {}");
        assertThat(compilation).succeededWithoutWarnings();
    }

    /**
     * Tests that compile-time validation throws an error for InheritableEvent on classes that are not inheritable.
     */
    @Test
    public void testInheritableEventValidation() {
        var compilation = compile("final class ClassTestEvent implements InheritableEvent {}");
        assertThat(compilation).hadErrorContaining("directly implements InheritableEvent but is not inheritable - extend MutableEvent instead");

        compilation = compile("record RecordTestEvent() implements InheritableEvent {}");
        assertThat(compilation).hadErrorContaining("directly implements InheritableEvent but is not inheritable - implement RecordEvent instead");

        compilation = compile("final class ClassTestEvent extends MutableEvent {}");
        assertThat(compilation).succeededWithoutWarnings();
    }
}
