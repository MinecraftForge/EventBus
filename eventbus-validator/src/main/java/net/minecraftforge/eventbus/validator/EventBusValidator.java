/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.eventbus.validator;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

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
    private Trees trees;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        trees = Trees.instance(processingEnv);
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
            var isCancellableEventBus = false;
            if (!(isStandardEventBus || (isCancellableEventBus = types.isSameType(erasedFieldType, BusTypes.cancellableEventBus))))
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

            // Show a warning if the field initialiser casts the result of EventBus.create(...) to CancellableEventBus
            if (isCancellableEventBus) {
                checkForCastedEventBusCreate(element);
            }
        }


        for (var child : element.getEnclosedElements()) {
            process(child);
        }
    }

    private void checkForCastedEventBusCreate(Element fieldElement) {
        TreePath path = trees.getPath(fieldElement);
        if (path == null || !(path.getLeaf() instanceof VariableTree variableTree)) return;

        // Check if initialiser is a cast expression and the expression being cast is a method invocation
        if (!(variableTree.getInitializer() instanceof TypeCastTree cast
                && cast.getExpression() instanceof MethodInvocationTree methodInvocation)) return;

        // Check if it's a call to EventBus.create(...)
        if (!(methodInvocation.getMethodSelect() instanceof MemberSelectTree memberSelectTree
                && memberSelectTree.getExpression() instanceof IdentifierTree identifierTree
                && identifierTree.getName().contentEquals("EventBus")
                && memberSelectTree.getIdentifier().contentEquals("create"))) return;

        // And that its result is being cast to CancellableEventBus...
        // Note: Can't use types#getTypeMirror here because it returns null for TypeCastTree when wrapped in TreePath in
        //       this context, so we have to manually resolve the type element from the tree instead.
        if (trees.getElement(new TreePath(new TreePath(path, cast), cast.getType())) instanceof TypeElement typeElement) {
            if (types.isSameType(types.erasure(typeElement.asType()), BusTypes.cancellableEventBus)) {
                processingEnv.getMessager().printWarning(
                        "CancellableEventBus field " + fieldElement.getEnclosingElement().getSimpleName() + '.' + fieldElement.getSimpleName() +
                                " should call CancellableEventBus#create(Class) directly instead of casting the result from EventBus#create(Class)",
                        fieldElement
                );
            }
        }
    }
}
