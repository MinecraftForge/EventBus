package net.minecraftforge.eventbus.service;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.eventbus.EventBusEngine;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.EnumSet;

public class ModLauncherService implements ILaunchPluginService {
    @Override
    public String name() {
        return "eventbus";
    }

    @Override
    public boolean processClass(final Phase phase, final ClassNode classNode, final Type classType) {
        return EventBusEngine.INSTANCE.processClass(classNode, classType);
    }

    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NAY = EnumSet.noneOf(Phase.class);
    @Override
    public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
        // we never handle empty classes
        return !isEmpty && EventBusEngine.INSTANCE.handlesClass(classType) ? YAY : NAY;
    }
}
