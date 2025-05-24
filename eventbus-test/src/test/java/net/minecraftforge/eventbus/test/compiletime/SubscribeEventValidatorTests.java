package net.minecraftforge.eventbus.test.compiletime;

import com.google.testing.compile.Compilation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;

public class SubscribeEventValidatorTests {
    @Language(value = "Java", suffix = "}")
    private static final String SOURCE_PREFIX = CompileTestHelper.SOURCE_PREFIX + "final class Listeners {";

    private static Compilation compile(@Language(value = "Java", prefix = SOURCE_PREFIX, suffix = "}") String sourceCode) {
        return CompileTestHelper.compileWithoutDefaultPrefix(SOURCE_PREFIX + sourceCode + "}");
    }

    @Test
    public void testSubscribeEventParamCount() {
        var compilation = compile("@SubscribeEvent void noParameters() {}");
        assertThat(compilation).hadErrorContaining("Invalid number of parameters");

        compilation = compile("@SubscribeEvent void tooManyParameters(int a, int b, int c) {}");
        assertThat(compilation).hadErrorContaining("Invalid number of parameters");
    }

    @Test
    public void testSubscribeEventParamTypes() {
        var compilation = compile("@SubscribeEvent void wrongFirstParamType(String notAnEvent) {}");
        assertThat(compilation).hadErrorContaining("must be an event");

        compilation = compile("@SubscribeEvent void correctFirstParam(EventWithData event) {}");
        assertThat(compilation).succeededWithoutWarnings();

        compilation = compile("@SubscribeEvent void wrongSecondParamType(EventWithData event, String notABoolean) {}");
        assertThat(compilation).hadErrorContaining("must be a boolean");

        compilation = compile("@SubscribeEvent void wrongParamTypes(EventWithData event, boolean wasCancelled) {}");
        assertThat(compilation).hadErrorContaining("only valid for cancellable events");
    }

    @Test
    public void testSubscribeEventReturnType() {
        var compilation = compile("@SubscribeEvent int invalidReturnType(EventWithData event) { return 0; }");
        assertThat(compilation).hadErrorContaining("expected void");

        compilation = compile("@SubscribeEvent boolean neverCancellingEvent(CancelableEvent event) { return false; }");
        assertThat(compilation).hadWarningContaining("consider using a void return type");
    }

    // Todo: The rest of the tests
}
