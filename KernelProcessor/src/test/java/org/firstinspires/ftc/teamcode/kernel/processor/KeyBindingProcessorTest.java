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

public class KeyBindingProcessorTest {
    private static final String ANNOTATIONS = "org.firstinspires.ftc.teamcode.kernel.input.annotations";

    private static final String KEY_STUB = "package org.firstinspires.ftc.teamcode.kernel.input;\n"
            + "public enum Key { A, B, X, Y, LEFT_BUMPER, RIGHT_BUMPER, LEFT_TRIGGER, RIGHT_TRIGGER }";

    private static String annotationStub(String name, String extraAttributes) {
        return "package " + ANNOTATIONS + ";\n"
                + "import org.firstinspires.ftc.teamcode.kernel.input.Key;\n"
                + "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n"
                + "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)\n"
                + "public @interface " + name + " { Key value(); int gamepad() default 1; Key[] with() default {};"
                + " double threshold() default 0.5; " + extraAttributes + " }";
    }

    private static List<Diagnostic<? extends JavaFileObject>> compile(String userClass) {
        List<JavaFileObject> sources = new ArrayList<>(Arrays.asList(
                source("org.firstinspires.ftc.teamcode.kernel.input.Key", KEY_STUB),
                source(ANNOTATIONS + ".OnPress", annotationStub("OnPress", "")),
                source(ANNOTATIONS + ".OnRelease", annotationStub("OnRelease", "")),
                source(ANNOTATIONS + ".WhileHeld", annotationStub("WhileHeld", "")),
                source(ANNOTATIONS + ".OnToggle", annotationStub("OnToggle", "")),
                source(ANNOTATIONS + ".OnLongPress", annotationStub("OnLongPress", "long ms() default 500;")),
                source(ANNOTATIONS + ".OnDoubleTap", annotationStub("OnDoubleTap", "long windowMs() default 300;")),
                source("user.Driver", "package user;\n"
                        + "import org.firstinspires.ftc.teamcode.kernel.input.Key;\n"
                        + "import " + ANNOTATIONS + ".*;\n"
                        + userClass)));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, null, diagnostics,
                Arrays.asList("-proc:only"), null, sources);
        task.setProcessors(Collections.singletonList(new KeyBindingProcessor()));
        task.call();
        return diagnostics.getDiagnostics();
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

    private static List<String> messages(List<Diagnostic<? extends JavaFileObject>> diagnostics, Diagnostic.Kind kind) {
        List<String> messages = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            if (d.getKind() == kind) messages.add(d.getMessage(Locale.ROOT));
        }
        return messages;
    }

    private static void assertSingleError(String userClass, String expectedFragment) {
        List<String> errors = messages(compile(userClass), Diagnostic.Kind.ERROR);
        assertEquals(errors.toString(), 1, errors.size());
        assertTrue(errors.get(0), errors.get(0).contains(expectedFragment));
    }

    @Test
    public void validBindingsCompileCleanly() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("public class Driver {\n"
                + "  @OnPress(Key.A) void a() {}\n"
                + "  @OnPress(value = Key.A, gamepad = 2) void a2() {}\n"
                + "  @OnPress(value = Key.A, with = Key.LEFT_BUMPER) void chord() {}\n"
                + "  @OnRelease(Key.A) void released() {}\n"
                + "  @WhileHeld(Key.RIGHT_BUMPER) public void held() {}\n"
                + "  @OnToggle(Key.X) void toggled(boolean on) {}\n"
                + "  @OnLongPress(value = Key.Y, ms = 600) protected void longPress() {}\n"
                + "  @OnDoubleTap(Key.B) void doubleTap() {}\n"
                + "  @OnPress(value = Key.RIGHT_TRIGGER, threshold = 0.8) void trigger() {}\n"
                + "}");
        assertEquals(Collections.emptyList(), messages(diagnostics, Diagnostic.Kind.ERROR));
        assertEquals(Collections.emptyList(), messages(diagnostics, Diagnostic.Kind.WARNING));
    }

    @Test
    public void staticMethodIsRejected() {
        assertSingleError("public class Driver { @OnPress(Key.A) static void a() {} }", "must not be static");
    }

    @Test
    public void privateMethodIsRejected() {
        assertSingleError("public class Driver { @OnPress(Key.A) private void a() {} }", "must not be private");
    }

    @Test
    public void parametersAreRejectedOutsideToggle() {
        assertSingleError("public class Driver { @OnPress(Key.A) void a(int x) {} }", "must take no parameters");
    }

    @Test
    public void toggleRequiresOneBoolean() {
        assertSingleError("public class Driver { @OnToggle(Key.A) void a() {} }", "exactly one boolean");
        assertSingleError("public class Driver { @OnToggle(Key.A) void a(int on) {} }", "exactly one boolean");
    }

    @Test
    public void gamepadMustBeOneOrTwo() {
        assertSingleError("public class Driver { @OnPress(value = Key.A, gamepad = 3) void a() {} }", "gamepad must be 1 or 2");
    }

    @Test
    public void thresholdOnlyOnTriggers() {
        assertSingleError("public class Driver { @OnPress(value = Key.A, threshold = 0.3) void a() {} }",
                "threshold only applies to LEFT_TRIGGER and RIGHT_TRIGGER");
    }

    @Test
    public void thresholdMustBeInRange() {
        assertSingleError("public class Driver { @OnPress(value = Key.LEFT_TRIGGER, threshold = 1.5) void a() {} }",
                "threshold must be in (0, 1]");
    }

    @Test
    public void keyCannotBeItsOwnModifier() {
        assertSingleError("public class Driver { @OnPress(value = Key.A, with = Key.A) void a() {} }",
                "can't be its own modifier");
    }

    @Test
    public void longPressAndDoubleTapNeedPositiveDurations() {
        assertSingleError("public class Driver { @OnLongPress(value = Key.A, ms = 0) void a() {} }", "ms must be > 0");
        assertSingleError("public class Driver { @OnDoubleTap(value = Key.A, windowMs = -5) void a() {} }",
                "windowMs must be > 0");
    }

    @Test
    public void duplicateBindingInOneClassIsRejected() {
        assertSingleError("public class Driver { @OnPress(Key.A) void a() {} @OnPress(Key.A) void b() {} }",
                "duplicates another @OnPress for gp1.A");
    }

    @Test
    public void sameKeyWithDifferentChordOrKindIsFine() {
        List<String> errors = messages(compile("public class Driver {\n"
                + "  @OnPress(Key.A) void a() {}\n"
                + "  @OnRelease(Key.A) void b() {}\n"
                + "  @OnPress(value = Key.A, with = Key.LEFT_BUMPER) void c() {}\n"
                + "}"), Diagnostic.Kind.ERROR);
        assertEquals(Collections.emptyList(), errors);
    }

    @Test
    public void sameControlInTwoClassesIsFine() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = compile("public class Driver { @OnPress(Key.A) void a() {} }\n"
                + "class OtherTeleOp { @OnPress(Key.A) void a() {} }");
        assertEquals(Collections.emptyList(), messages(diagnostics, Diagnostic.Kind.ERROR));
        assertEquals(Collections.emptyList(), messages(diagnostics, Diagnostic.Kind.WARNING));
    }
}
