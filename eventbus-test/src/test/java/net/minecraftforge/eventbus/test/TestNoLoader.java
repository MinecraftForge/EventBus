package net.minecraftforge.eventbus.test;

import org.junit.jupiter.api.Test;

import net.minecraftforge.eventbus.test.general.AbstractEventListenerTest;
import net.minecraftforge.eventbus.test.general.DeadlockingEventTest;
import net.minecraftforge.eventbus.test.general.EventBusSubtypeFilterTest;
import net.minecraftforge.eventbus.test.general.EventFiringEventTest;
import net.minecraftforge.eventbus.test.general.EventHandlerExceptionTest;
import net.minecraftforge.eventbus.test.general.GenericListenerTests;
import net.minecraftforge.eventbus.test.general.LambdaHandlerTest;
import net.minecraftforge.eventbus.test.general.NonPublicEventHandler;
import net.minecraftforge.eventbus.test.general.ParallelEventTest;
import net.minecraftforge.eventbus.test.general.ParentHandlersGetInvokedTest;
import net.minecraftforge.eventbus.test.general.ParentHandlersGetInvokedTestDummy;
import net.minecraftforge.eventbus.test.general.ThreadedListenerExceptionTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

public class TestNoLoader extends TestNoLoaderBase {

    @Test
    void eventHandlersCanSubscribeToAbstractEvents() {
        doTest(new AbstractEventListenerTest() {});
    }

    @RepeatedTest(10)
    public void testMultipleThreadsMultipleBus() {
        doTest(new ParallelEventTest.Multiple() {});
    }

    @RepeatedTest(100)
    public void testMultipleThreadsOneBus() {
        doTest(new ParallelEventTest.Single() {});
    }

    @Test
    public void testEventHandlerException() {
        doTest(new EventHandlerExceptionTest() {});
    }

    @Test
    public void testValidType() {
        doTest(new EventBusSubtypeFilterTest.Valid() {});
    }

    @Test
    public void testInvalidType() {
        doTest(new EventBusSubtypeFilterTest.Invalid() {});
    }

    @Test
    public void testInvalidTypeNoDispatch() {
        doTest(new EventBusSubtypeFilterTest.InvalidNoDispatch() {});
    }

    @Test
    void eventHandlersCanFireEvents() {
        doTest(new EventFiringEventTest() {});
    }

    @Test
    public void lambdaBasic() {
        doTest(new LambdaHandlerTest.Basic() {});
    }

    @Test
    public void lambdaSubClass() {
        doTest(new LambdaHandlerTest.SubClassEvent() {});
    }

    @Test
    public void lambdaGenerics() {
        doTest(new LambdaHandlerTest.Generics() {});
    }

    @Disabled
    @RepeatedTest(500)
    public void deadlockTest() {
        doTest(new DeadlockingEventTest() {});
    }

    @Test
    public void parentHandlerGetsInvoked() {
        doTest(new ParentHandlersGetInvokedTest() {});
    }

    @Test
    public void parentHandlerGetsInvokedDummy() {
        doTest(new ParentHandlersGetInvokedTestDummy() {});
    }

    @RepeatedTest(100)
    public void testThreadedEventFiring() {
        doTest(new ThreadedListenerExceptionTest() {});
    }

    @Test
    public void testGenericListener() {
        doTest(new GenericListenerTests.Basic() {});
    }

    @Test
    public void testGenericListenerRegisteredIncorrectly() {
        doTest(new GenericListenerTests.IncorrectRegistration() {});
    }

    @Test
    public void testGenericListenerWildcard() {
        doTest(new GenericListenerTests.Wildcard() {});
    }

    @Disabled //TODO: Do we want to add checks for this? By Default the methods just silent are ignored.
    @Test
    public void testNonPublicEventHandler() {
        doTest(new NonPublicEventHandler(false) {});
    }
}
