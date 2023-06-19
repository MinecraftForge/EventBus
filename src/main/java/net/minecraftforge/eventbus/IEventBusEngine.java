package net.minecraftforge.eventbus;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.function.Supplier;

public interface IEventBusEngine {
    int processClass(ClassNode classNode, Type classType);

    boolean handlesClass(Type classType);

    boolean findASMEventDispatcher(Type classType);
    void acceptClassLoaderSupplier(Supplier<ClassLoader> classLoaderSupplier);
}
