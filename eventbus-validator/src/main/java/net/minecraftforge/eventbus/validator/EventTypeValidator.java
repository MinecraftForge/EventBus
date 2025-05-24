/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.validator;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

public final class EventTypeValidator extends AbstractValidator {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var typeUtils = processingEnv.getTypeUtils();
        for (Element root : roundEnv.getRootElements()) {
            var rootKind = root.getKind();
            if (rootKind != ElementKind.CLASS && rootKind != ElementKind.INTERFACE && rootKind != ElementKind.RECORD)
                continue;

            var rootType = root.asType();

            // Check that RecordEvent is only implemented by record classes
            if (typeUtils.isAssignable(rootType, EventTypes.recordEvent) && rootKind != ElementKind.RECORD) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Event type " + rootType + " implements RecordEvent but is not a record class",
                        root
                );
            }

            // Check that MonitorAware is only implemented on classes that extend MutableEvent
            if (typeUtils.isAssignable(rootType, EventCharacteristics.monitorAware) && !typeUtils.isAssignable(rootType, EventTypes.mutableEvent)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Event type " + rootType + " implements MonitorAware but is not a mutable event",
                        root
                );
            }

            // Check that InheritableEvent is not directly implemented by classes that are not inheritable
            var rootElement = (TypeElement) root;
            if (typeUtils.isAssignable(rootType, EventTypes.inheritableEvent) // instanceof InheritableEvent
                    && (rootKind == ElementKind.RECORD || rootElement.getModifiers().contains(Modifier.FINAL)) // record or final
                    && (rootKind == ElementKind.RECORD || rootElement.getSuperclass().toString().equals(Object.class.getCanonicalName()))) { // record or no extends clause

                var rootInterfaces = rootElement.getInterfaces(); // only one interface which is exactly InheritableEvent
                if (rootInterfaces.size() == 1 && typeUtils.isSameType(rootInterfaces.getFirst(), EventTypes.inheritableEvent)) {
                    var errorMsg = "Event type " + rootType + " directly implements InheritableEvent but is not inheritable";
                    var solution = rootKind == ElementKind.RECORD ? "implement RecordEvent instead" : "extend MutableEvent instead";
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            errorMsg + " - " + solution,
                            root
                    );
                }
            }
        }

        return false; // allow other processors to run
    }
}
