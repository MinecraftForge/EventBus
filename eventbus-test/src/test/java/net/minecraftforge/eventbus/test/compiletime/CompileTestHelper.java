/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.test.compiletime;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.testjar.events.EventWithData;
import net.minecraftforge.eventbus.validator.EventBusValidator;
import net.minecraftforge.eventbus.validator.EventTypeValidator;
import net.minecraftforge.eventbus.validator.SubscribeEventValidator;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

final class CompileTestHelper {
    private CompileTestHelper() {}

    private static final List<File> CLASSPATH;

    @Language("Java")
    static final String SOURCE_PREFIX = """
            import net.minecraftforge.eventbus.api.bus.*;
            import net.minecraftforge.eventbus.api.event.*;
            import net.minecraftforge.eventbus.api.event.characteristic.*;
            import net.minecraftforge.eventbus.api.listener.*;
            
            import net.minecraftforge.eventbus.testjar.events.*;
            
            import org.jspecify.annotations.NullMarked;
            import org.jspecify.annotations.Nullable;
            
            @NullMarked
            """;

    static {
        try {
            CLASSPATH = List.of(
                    Path.of(EventBus.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile(),
                    Path.of(EventWithData.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile(),
                    Path.of(NullMarked.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static Compilation compile(@Language(value = "Java", prefix = SOURCE_PREFIX) String sourceCode) {
        return compileWithoutDefaultPrefix(SOURCE_PREFIX + sourceCode);
    }

    static Compilation compileWithoutDefaultPrefix(@Language(value = "Java") String sourceCode) {
        return Compiler.javac()
                .withProcessors(new EventBusValidator(), new EventTypeValidator(), new SubscribeEventValidator())
                .withClasspath(CLASSPATH)
                .compile(JavaFileObjects.forSourceString("", sourceCode));
    }
}
