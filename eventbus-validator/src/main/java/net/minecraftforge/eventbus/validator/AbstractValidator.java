/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.validator;

import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.event.characteristic.MonitorAware;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

abstract sealed class AbstractValidator implements Processor
        permits EventBusValidator, EventTypeValidator, SubscribeEventValidator {
    protected ProcessingEnvironment processingEnv;

    protected static final class EventTypes {
        private EventTypes() {}

        protected static TypeMirror mutableEvent;
        protected static TypeMirror recordEvent;
        protected static TypeMirror inheritableEvent;
    }

    protected static final class EventCharacteristics {
        private EventCharacteristics() {}

        protected static TypeMirror cancellable;
        protected static TypeMirror monitorAware;
    }

    protected static final class BusTypes {
        private BusTypes() {}

        protected static TypeMirror eventBus;
        protected static TypeMirror cancellableEventBus;
    }

    protected AbstractValidator() {}

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        var elements = processingEnv.getElementUtils();
        var types = processingEnv.getTypeUtils();
        EventTypes.inheritableEvent = elements.getTypeElement(InheritableEvent.class.getCanonicalName()).asType();
        EventTypes.recordEvent = elements.getTypeElement(RecordEvent.class.getCanonicalName()).asType();
        EventTypes.mutableEvent = elements.getTypeElement(MutableEvent.class.getCanonicalName()).asType();
        EventCharacteristics.cancellable = elements.getTypeElement(Cancellable.class.getCanonicalName()).asType();
        EventCharacteristics.monitorAware = elements.getTypeElement(MonitorAware.class.getCanonicalName()).asType();
        BusTypes.eventBus = types.erasure(elements.getTypeElement(EventBus.class.getCanonicalName()).asType());
        BusTypes.cancellableEventBus = types.erasure(elements.getTypeElement(CancellableEventBus.class.getCanonicalName()).asType());
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return List.of();
    }
}
