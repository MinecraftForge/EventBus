package net.minecraftforge.eventbus.test;

import net.minecraftforge.eventbus.api.BusBuilder;
import static org.junit.jupiter.api.Assertions.*;

public class TestNoLoaderBase {
    private void validate(Class<?> clazz) {
        // We expect transformers to not run, so make sure LISTENER_LIST does not exist
        assertFalse(Whitebox.hasField(clazz, "LISTENER_LIST"), "EventSubclassTransformer ran on " + clazz.getName() + ", we wanted to use non-transformed events");
    }

    private BusBuilder builder() {
        return BusBuilder.builder();
    }

    protected void doTest(ITestHandler handler) {
        handler.before(this::validate, this::builder);
        handler.test(this::validate, this::builder);
        handler.after(this::validate, this::builder);
    }
}
