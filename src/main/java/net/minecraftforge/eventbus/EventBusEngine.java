package net.minecraftforge.eventbus;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public final class EventBusEngine implements IEventBusEngine {
    private final EventSubclassTransformer eventTransformer;
    private final EventAccessTransformer accessTransformer;

    public EventBusEngine() {
        LogManager.getLogger().debug(LogMarkers.EVENTBUS, "Loading EventBus transformers");
        this.eventTransformer = new EventSubclassTransformer();
        this.accessTransformer = new EventAccessTransformer();
    }

    @Override
    public int processClass(final ClassNode classNode, final Type classType) {
        if (ASMEventHandler.hasPendingWrapperClass(classType.getClassName())) {
            ASMEventHandler.processWrapperClass(classType.getClassName(), classNode);
            LogManager.getLogger().debug(LogMarkers.EVENTBUS, "Built transformed event wrapper class {}", classType.getClassName());
            return ClassWriter.COMPUTE_FRAMES;
        }
        final int evtXformFlags = eventTransformer.transform(classNode, classType).isPresent() ? ClassWriter.COMPUTE_FRAMES : 0x0;
        final int axXformFlags = accessTransformer.transform(classNode, classType) ? 0x100 : 0;
        return evtXformFlags | axXformFlags;
    }

    @Override
    public boolean handlesClass(final Type classType) {
        final String name = classType.getClassName();
        return !(name.equals("net.minecraftforge.eventbus.api.Event") ||
                name.startsWith("net.minecraft.") ||
                name.indexOf('.') == -1);
    }

    @Override
    public boolean findASMEventDispatcher(final Type classType) {
        return ASMEventHandler.hasPendingWrapperClass(classType.getClassName());
    }
}
