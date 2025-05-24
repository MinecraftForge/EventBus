# EventBus

A flexible, high-performance, thread-safe subscriber-publisher framework designed with modern Java in mind.

## Overview
The core functionality of EventBus is to provide a simple and efficient way to handle events in a decoupled manner.

Each event may have one or more buses associated with it, which are responsible for managing listeners and dispatching
instances of the event object to them. To maximise performance, the underlying implementation is tailored on the fly
based on the event's type, characteristics, inheritance chain and the number and type of listeners registered to the bus.

## Quickstart guide
First, add the Forge Maven repository and the EventBus dependency to your project:
```gradle
repositories {
    maven {
        name = "Forge"
        url = "https://maven.minecraftforge.net"
    }
}

dependencies {
    implementation "net.minecraftforge:eventbus:<version>"
    annotationProcessor "net.minecraftforge:eventbus-validator:<version>"
}
```

You can find the list of available versions [here][Versions].

Now you can start using EventBus in your project. Simple usage example:
```java
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.bus.EventBus;

// Define an event and a bus for it
record PlayerLoggedInEvent(String username) implements RecordEvent {
    public static final EventBus<PlayerLoggedInEvent> BUS = EventBus.create(PlayerLoggedInEvent.class);
}

// Register an event listener
PlayerLoggedInEvent.BUS.addListener(event -> System.out.println("Player logged in: " + event.username()));

// Post an event to the registered listeners
PlayerLoggedInEvent.BUS.post(new PlayerLoggedInEvent("Paint_Ninja"));
```

Browse the `net.minecraftforge.eventbus.api` package and read the Javadocs for more information. For real-world
examples, check out Forge's extensive use of EventBus [here][Forge usages].

## Nullability
The entirety of EventBus' API is `@NullMarked` and compliant with the [jSpecify specification](https://jspecify.dev/) -
this means that everything is non-null by default unless otherwise specified.

Attempting to pass a `null` value to a method param that isn't explicitly marked as `@Nullable` is an unsupported
operation and won't be considered a breaking change if a future version throws an exception in such cases when it didn't
before.

## Validation
To improve startup performance, EventBus 7 relies heavily on dev-time static analysis and compile-time validation to
ensure correct API usage, reducing the need for runtime checks. This also helps catch potential issues early on and
provides more informative error messages that suggest solutions.

It is highly recommended to use the `eventbus-validator` annotation processor during development to enable enhanced
validation.

### Runtime checks
EventBus performs limited validation at runtime and assumes API usage is mostly correct. Incorrect usage of the API may
lead to unexpected behaviour and breaking changes when updating EventBus due to reliance on internal implementation
details.

For use-cases where you need to debug issues in production or are unable to use the annotation processor, you can enable
strict runtime checks by setting the `eventbus.api.strictRuntimeChecks` system property to `true` when launching your
application. This will enable more exhaustive runtime checks for most API usage, including bulk registration of
listeners and EventBus creation, but not field declarations.

Alternatively, you can selectively enable strict runtime checks for specific subsets of the API by using the
`eventbus.api.strictRegistrationChecks` and `eventbus.api.strictBusCreationChecks` system properties for bulk
registration and EventBus creation, respectively. This may be useful if you only intend on adding listeners to an
existing event made by another library, where you don't need strict checks for the creation of its associated EventBus
due to it being outside your control.

## Contributing
One of the main goals of EventBus is performance. As such, any changes should be benchmarked with the `jmh` Gradle task
to help ensure that there are no unintended performance regressions. If you are unsure how to do this or would like
to discuss your ideas before submitting a PR, feel free to reach out to us on the [Forge Discord].

[Versions]: https://files.minecraftforge.net/net/minecraftforge/eventbus/
[Forge usages]: https://github.com/MinecraftForge/MinecraftForge/tree/1.21.x/src/main/java/net/minecraftforge/event
[Forge Discord]: https://discord.minecraftforge.net/
