package net.minecraftforge.eventbus;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public enum EventBusEngine {
    INSTANCE;

    private final EventSubscriptionTransformer subscriptionTransformer;
    private final EventAccessTransformer accessTransformer;

    EventBusEngine() {
        this.subscriptionTransformer = new EventSubscriptionTransformer();
        this.accessTransformer = new EventAccessTransformer();
    }

    public ClassNode processClass(final ClassNode classNode, final Type classType) {
        subscriptionTransformer.transform(classNode, classType).ifPresent(cn->accessTransformer.transform(cn,classType));
        return classNode;
    }

    public boolean handlesClass(final Type classType) {
        return subscriptionTransformer.handlesType(classType);
    }
}
