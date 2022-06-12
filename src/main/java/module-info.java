open module net.minecraftforge.eventbus {
    uses net.minecraftforge.eventbus.IEventBusEngine;
    requires cpw.mods.modlauncher;

    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j;
    requires static org.jetbrains.annotations;
    requires typetools;

    exports net.minecraftforge.eventbus;
    exports net.minecraftforge.eventbus.api;
    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService with net.minecraftforge.eventbus.service.ModLauncherService;
    provides net.minecraftforge.eventbus.IEventBusEngine with net.minecraftforge.eventbus.EventBusEngine;
}