package net.minecraftforge.eventbus;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.function.Supplier;

public final class EventBusEngine implements IEventBusEngine {
    static EventBusEngine INSTANCE;
    private final EventSubclassTransformer eventTransformer;
    private final EventAccessTransformer accessTransformer;
    final String EVENT_CLASS = "net.minecraftforge.eventbus.api.Event";
    private Supplier<ClassLoader> clazzLoaderSupplier = ()->
            Thread.currentThread().getContextClassLoader() != null ? Thread.currentThread().getContextClassLoader() : EventBusEngine.INSTANCE.getClassLoader();

    public EventBusEngine() {
        LogManager.getLogger().debug(LogMarkers.EVENTBUS, "Loading EventBus transformers");
        this.eventTransformer = new EventSubclassTransformer(this);
        this.accessTransformer = new EventAccessTransformer(this);
        INSTANCE = this;
    }

    @Override
    public int processClass(final ClassNode classNode, final Type classType) {
        if (ModLauncherFactory.hasPendingWrapperClass(classType.getClassName())) {
            ModLauncherFactory.processWrapperClass(classType.getClassName(), classNode);
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
        return !(name.startsWith("net.minecraft.") || name.indexOf('.') == -1);
    }

    @Override
    public boolean findASMEventDispatcher(final Type classType) {
        return ModLauncherFactory.hasPendingWrapperClass(classType.getClassName());
    }

    @Override
    public void acceptClassLoaderSupplier(final Supplier<ClassLoader> classLoaderSupplier) {
        this.clazzLoaderSupplier = classLoaderSupplier;
    }

    ClassLoader getClassLoader() {
        return this.clazzLoaderSupplier.get();
    }
}
