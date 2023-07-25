package net.minecraftforge.eventbus;

import static org.objectweb.asm.Type.*;

import org.objectweb.asm.tree.MethodNode;

final class Names {
    static final String SUBSCRIBE_EVENT = "Lnet/minecraftforge/eventbus/api/SubscribeEvent;";
    static final String HAS_RESULT = "Lnet/minecraftforge/eventbus/api/Event$HasResult;";
    static final Method HAS_RESULT_M = new Method("hasResult", getMethodDescriptor(BOOLEAN_TYPE));

    static final String CANCELLABLE = "Lnet/minecraftforge/eventbus/api/Cancelable;";
    static final Method CANCELABLE_M = new Method("isCancelable", getMethodDescriptor(BOOLEAN_TYPE));

    static final String LISTENER_LIST = "Lnet/minecraftforge/eventbus/ListenerList;";
    static final Method LISTENER_LIST_INIT = new Method("<init>", getMethodDescriptor(VOID_TYPE, getType(LISTENER_LIST)));
    static final Method LISTENER_LIST_GET  = new Method("getListenerList", getMethodDescriptor(getType(LISTENER_LIST)));
    static final Method LISTENER_LIST_F = new Method("LISTENER_LIST", LISTENER_LIST);
    static final String LISTENER_LIST_HELPER = "Lnet/minecraftforge/eventbus/api/EventListenerHelper;";
    static final Method LISTENER_LIST_HAS_LISTENERS_M = new Method("hasListeners", getMethodDescriptor(BOOLEAN_TYPE, INT_TYPE));

    static final Method LISTENER_LIST_GET_STATICALLY  = new Method("getListenerListStatically", getMethodDescriptor(getType(LISTENER_LIST)));

    static final String EVENT_BUS = "Lnet/minecraftforge/eventbus/EventBus;";
    static final Method GET_BUS_ID_M = new Method("getBusID", getMethodDescriptor(INT_TYPE));

    static final Method HAS_ANY_LISTENERS_M = new Method("hasAnyListeners", getMethodDescriptor(BOOLEAN_TYPE));

    static final String EVENT = "Lnet/minecraftforge/eventbus/api/Event;";
    static final Method EVENT_HAS_LISTENERS_M = new Method("hasListeners", getMethodDescriptor(BOOLEAN_TYPE, getType(EVENT_BUS)));

    static final Method INIT_M = new Method("<init>", getMethodDescriptor(VOID_TYPE));
    static final Method STATIC_INIT_M = new Method("<clinit>", getMethodDescriptor(VOID_TYPE));

    record Method(String name, String desc) {
        boolean equals(MethodNode node) {
            return this.name.equals(node.name) && this.desc.equals(node.desc);
        }
    }
}
