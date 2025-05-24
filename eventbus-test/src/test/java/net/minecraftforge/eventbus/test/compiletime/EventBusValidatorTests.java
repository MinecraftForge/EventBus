/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test.compiletime;

import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static net.minecraftforge.eventbus.test.compiletime.CompileTestHelper.compile;

public class EventBusValidatorTests {
    @Test
    public void testBusFieldModifiers() {
        var compilation = compile("""
            record RecordTestEvent() implements RecordEvent {
                static EventBus<RecordTestEvent> BUS = EventBus.create(RecordTestEvent.class);
            }
        """);
        assertThat(compilation).hadWarningContaining("should be final");
    }

    @Test
    public void testBusFieldType() {
        var compilation = compile("""
            record CancellableEvent() implements Cancellable, RecordEvent {
                static final EventBus<CancellableEvent> BUS = EventBus.create(CancellableEvent.class);
            }
        """);
        assertThat(compilation).hadWarningContaining("should be CancellableEventBus");
    }
}
