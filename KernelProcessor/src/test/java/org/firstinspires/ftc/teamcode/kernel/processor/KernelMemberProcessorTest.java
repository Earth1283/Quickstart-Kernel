package org.firstinspires.ftc.teamcode.kernel.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class KernelMemberProcessorTest {
    private static final String WATCH_STUB = "package org.firstinspires.ftc.teamcode.kernel.telemetry;\n"
            + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n"
            + "@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.FIELD})\n"
            + "public @interface Watch { String value() default \"\"; int order() default 0; }";

    private static final String EVERY_STUB = "package org.firstinspires.ftc.teamcode.kernel.time;\n"
            + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n"
            + "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)\n"
            + "public @interface Every { long value(); boolean duringInit() default false; }";

    private static List<String> errors(String userClass) {
        List<JavaFileObject> sources = Arrays.asList(
                source(KernelMemberProcessor.WATCH, WATCH_STUB),
                source(KernelMemberProcessor.EVERY, EVERY_STUB),
                source("user.Driver", "package user;\n"
                        + "import org.firstinspires.ftc.teamcode.kernel.telemetry.Watch;\n"
                        + "import org.firstinspires.ftc.teamcode.kernel.time.Every;\n"
                        + userClass));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics,
                Arrays.asList("-proc:only"), null, sources);
        task.setProcessors(Collections.singletonList(new KernelMemberProcessor()));
        task.call();
        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) errors.add(d.getMessage(Locale.ROOT));
        }
        return errors;
    }

    private static JavaFileObject source(String className, String code) {
        URI uri = URI.create("string:///" + className.replace('.', '/') + ".java");
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    private static void assertSingleError(String userClass, String expectedFragment) {
        List<String> errors = errors(userClass);
        assertEquals(errors.toString(), 1, errors.size());
        assertTrue(errors.get(0), errors.get(0).contains(expectedFragment));
    }

    @Test
    public void validMembersCompileCleanly() {
        assertEquals(Collections.emptyList(), errors("public class Driver {\n"
                + "  @Watch private double speed;\n"
                + "  @Watch(value = \"lift\", order = -1) String lift;\n"
                + "  @Watch(\"aiming\") boolean aiming() { return true; }\n"
                + "  @Every(250) void poll() {}\n"
                + "  @Every(value = 1000, duringInit = true) protected int check() { return 0; }\n"
                + "}"));
    }

    @Test
    public void staticMembersAreRejected() {
        assertSingleError("public class Driver { @Watch static int shared; }", "must not be static");
        assertSingleError("public class Driver { @Every(100) static void poll() {} }", "must not be static");
    }

    @Test
    public void privateMethodsAreRejected() {
        assertSingleError("public class Driver { @Watch private int hidden() { return 0; } }", "must not be private");
        assertSingleError("public class Driver { @Every(100) private void poll() {} }", "must not be private");
    }

    @Test
    public void methodsTakeNoParameters() {
        assertSingleError("public class Driver { @Watch int value(int x) { return x; } }", "must take no parameters");
        assertSingleError("public class Driver { @Every(100) void poll(int x) {} }", "must take no parameters");
    }

    @Test
    public void watchedMethodMustReturnSomething() {
        assertSingleError("public class Driver { @Watch void nothing() {} }", "must return the value to show");
    }

    @Test
    public void everyNeedsAPositivePeriod() {
        assertSingleError("public class Driver { @Every(0) void poll() {} }", "period must be > 0 ms, got 0");
    }

    @Test
    public void duplicateWatchKeyInOneClassIsRejected() {
        assertSingleError("public class Driver { @Watch(\"lift\") int a; @Watch(\"lift\") int b; }",
                "duplicates another @Watch key");
        assertSingleError("public class Driver { int speed; @Watch(\"speed\") int a; @Watch int speed() { return 0; } }",
                "duplicates another @Watch key");
    }

    @Test
    public void sameWatchKeyInTwoClassesIsFine() {
        assertEquals(Collections.emptyList(), errors("public class Driver { @Watch int speed; }\n"
                + "class OtherTeleOp { @Watch int speed; }"));
    }
}
