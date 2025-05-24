/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.validator;

import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

public final class SubscribeEventValidator extends AbstractValidator {
    private Types types;
    private TypeMirror eventType;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(SubscribeEvent.class.getCanonicalName());
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        var elements = processingEnv.getElementUtils();
        eventType = elements.getTypeElement("net.minecraftforge.eventbus.internal.Event").asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(SubscribeEvent.class)) {
            if (e.getKind() == ElementKind.METHOD) {
                validate((ExecutableElement) e);
            }
        }

        return false; // allow other processors to run
    }

    private void validate(ExecutableElement method) {
        int paramCount = method.getParameters().size();

        if (paramCount == 0 || paramCount > 2) {
            error(method, "Invalid number of parameters: " + paramCount + " (expected 1 or 2)");
            return;
        }

        var firstParam = method.getParameters().getFirst();
        var firstParamType = firstParam.asType();
        if (!types.isAssignable(firstParamType, eventType))
            error(method, "First parameter of a @SubscribeEvent method must be an event");

        var returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.VOID && returnType.getKind() != TypeKind.BOOLEAN)
            error(method, "Invalid return type: " + returnType + " (expected void or boolean)");

        var firstParamExtendsCancellable = types.isAssignable(firstParamType, EventCharacteristics.cancellable);
        var subscribeEventAnnotation = method.getAnnotation(SubscribeEvent.class);
        var isMonitoringListener = subscribeEventAnnotation.priority() == Priority.MONITOR;

        if (isMonitoringListener && (returnType.getKind() == TypeKind.BOOLEAN || subscribeEventAnnotation.alwaysCancelling()))
            error(method, "Monitoring listeners cannot cancel events");

        if (paramCount == 2) {
            if (!firstParamExtendsCancellable)
                error(method, "Cancellation-aware monitoring listeners are only valid for cancellable events");

            var secondParamType = method.getParameters().getLast().asType();
            if (secondParamType.getKind() != TypeKind.BOOLEAN)
                error(method, "Second parameter of a cancellation-aware monitoring listener must be a boolean");

            if (subscribeEventAnnotation.priority() != Priority.MONITOR)
                error(method, "Cancellation-aware monitoring listeners must have a priority of MONITOR");
        }

        if (!firstParamExtendsCancellable) {
            if (subscribeEventAnnotation.alwaysCancelling())
                error(method, "Always cancelling listeners are only valid for cancellable events");

            if (returnType.getKind() == TypeKind.BOOLEAN)
                error(method, "Return type boolean is only valid for cancellable events");
        }
    }

    private void error(ExecutableElement method, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, method);
    }
}
