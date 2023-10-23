open module net.minecraftforge.eventbus.jmh {
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;

    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires net.minecraftforge.eventbus;
    requires jopt.simple;
    requires jmh.core;

    requires static org.jetbrains.annotations;
    requires static net.minecraftforge.eventbus.testjars;

    provides cpw.mods.modlauncher.api.ITransformationService with
        net.minecraftforge.eventbus.benchmarks.MockTransformerService;
}