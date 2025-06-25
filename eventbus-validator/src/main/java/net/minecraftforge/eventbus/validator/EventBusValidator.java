/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.validator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import java.util.Set;

public final class EventBusValidator extends AbstractValidator {
    private Types types;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (var root : roundEnv.getRootElements()) {
            process(root);
        }

        return false; // allow other processors to run
    }

    private void process(Element element) {
        if (element.getKind() == ElementKind.FIELD) {
            TypeMirror erasedFieldType = types.erasure(element.asType());
            var isStandardEventBus = types.isSameType(erasedFieldType, BusTypes.eventBus);
            if (!(isStandardEventBus || types.isSameType(erasedFieldType, BusTypes.cancellableEventBus)))
                return; // only interested in (Cancellable)EventBus fields

            // Show a warning if the bus field is not final
            if (!element.getModifiers().contains(Modifier.FINAL)) {
                processingEnv.getMessager().printWarning(
                        "EventBus field " + element + " should be final",
                        element
                );
            }

            // Show a warning if the T in field type EventBus<T> is Cancellable (should be CancellableEventBus<T> instead)
            if (isStandardEventBus && element.asType() instanceof DeclaredType declaredType
                    && !declaredType.getTypeArguments().isEmpty()) {
                var genericType = declaredType.getTypeArguments().getFirst();
                if (types.isAssignable(genericType, EventCharacteristics.cancellable)) {
                    processingEnv.getMessager().printWarning(
                            """
                            EventBus field %s should be CancellableEventBus<%s> instead of EventBus<%s> because %s inherits from Cancellable
                            """,
                            element
                    );
                }
            }
        }


        for (var child : element.getEnclosedElements()) {
            process(child);
        }
    }
}
