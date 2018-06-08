package net.minecraftforge.eventbus;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import static net.minecraftforge.eventbus.Logging.EVENTBUS;

public enum EventBusEngine {
    INSTANCE;

    private final EventSubscriptionTransformer subscriptionTransformer;
    private final EventAccessTransformer accessTransformer;

    EventBusEngine() {
        LogManager.getLogger("EVENTBUS").debug(EVENTBUS, "Loading EventBus transformer");
        this.subscriptionTransformer = new EventSubscriptionTransformer();
        this.accessTransformer = new EventAccessTransformer();
    }

    public ClassNode processClass(final ClassNode classNode, final Type classType) {
        subscriptionTransformer.transform(classNode, classType);
        accessTransformer.transform(classNode,classType);
        return classNode;
    }

    public boolean handlesClass(final Type classType) {
        final String name = classType.getClassName();
        return !(name.equals("net.minecraftforge.eventbus.api.Event") ||
                name.startsWith("net.minecraft.") ||
                name.indexOf('.') == -1);
    }
}
