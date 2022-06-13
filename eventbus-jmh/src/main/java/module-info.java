open module net.minecraftforge.eventbus.jmh {
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;

    requires org.junit.jupiter.api;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires net.minecraftforge.eventbus;
    requires joptsimple;
    requires jmh.core;
    requires cpw.mods.bootstraplauncher;

    requires static org.jetbrains.annotations;
    requires static net.minecraftforge.eventbus.testjars;

    exports net.minecraftforge.eventbus.benchmarks;

    provides cpw.mods.modlauncher.api.ILaunchHandlerService with net.minecraftforge.eventbus.benchmarks.MockLauncherHandlerService;
    provides cpw.mods.modlauncher.api.ITransformationService with net.minecraftforge.eventbus.benchmarks.MockTransformerService;

}