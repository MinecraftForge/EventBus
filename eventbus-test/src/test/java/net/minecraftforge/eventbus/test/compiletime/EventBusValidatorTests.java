/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test.compiletime;

import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static net.minecraftforge.eventbus.test.compiletime.CompileTestHelper.compile;

public class EventBusValidatorTests {
    /**
     * Tests that compile-time validation emits a warning for EventBus fields that are not final.
     */
    @Test
    public void testBusFieldModifiers() {
        var compilation = compile("""
            record RecordTestEvent() implements RecordEvent {
                static EventBus<RecordTestEvent> BUS = EventBus.create(RecordTestEvent.class);
            }
        """);
        assertThat(compilation).hadWarningContaining("should be final");
    }

    /**
     * Tests that compile-time validation emits a warning for EventBus fields that are not of the correct type.
     */
    @Test
    public void testBusFieldType() {
        var compilation = compile("""
            record CancellableEvent() implements Cancellable, RecordEvent {
                static final EventBus<CancellableEvent> BUS = EventBus.create(CancellableEvent.class);
            }
        """);
        assertThat(compilation).hadWarningContaining("should be CancellableEventBus");
    }

    /**
     * Tests that the compile-time validation emits a warning when calling EventBus#create(Class) and casting the result
     * to CancellableEventBus, as this relies on internal implementation details that aren't guaranteed to hold true in
     * future patch updates. The correct approach is to call CancellableEventBus#create(Class) directly.
     */
    @Test
    public void testBusFieldNotCasted() {
        var compilation = compile("""
            record CancellableEvent() implements Cancellable, RecordEvent {
                static final CancellableEventBus<CancellableEvent> BUS = (CancellableEventBus<CancellableEvent>) EventBus.create(CancellableEvent.class);
            }
        """);
        assertThat(compilation).hadWarningContaining("should call CancellableEventBus#create(Class) directly");

        compilation = compile("""
            record CancellableEvent() implements Cancellable, RecordEvent {
                static final CancellableEventBus<CancellableEvent> BUS = CancellableEventBus.create(CancellableEvent.class);
            }
        """);
        assertThat(compilation).succeeded();
    }
}
