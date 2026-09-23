package org.firstinspires.ftc.teamcode.kernel.input;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnDoubleTap;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnLongPress;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnPress;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnRelease;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnToggle;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.WhileHeld;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Re-checks at runtime what KernelProcessor checks at compile time, for builds that skip the
// processor (OnBotJava, or a missing annotationProcessor line).
final class AnnotationBinder {
    private AnnotationBinder() {}

    static void bind(Input input, Object target) {
        for (Class<?> type = target.getClass(); isUserClass(type); type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) bindMethod(input, target, method);
        }
    }

    private static boolean isUserClass(Class<?> type) {
        if (type == null || type == Object.class) return false;
        String name = type.getName();
        return !name.startsWith("com.qualcomm.")
                && !name.startsWith("org.firstinspires.ftc.robotcore.")
                && !name.startsWith("java.")
                && !name.startsWith("android.");
    }

    private static void bindMethod(Input input, Object target, Method method) {
        OnPress press = method.getAnnotation(OnPress.class);
        if (press != null) {
            Spec spec = new Spec(press, press.value(), press.gamepad(), press.with(), press.threshold());
            Button.Action action = noArgs(target, method, spec);
            resolve(input, spec).onPress(spec.describe(method), action);
        }
        OnRelease release = method.getAnnotation(OnRelease.class);
        if (release != null) {
            Spec spec = new Spec(release, release.value(), release.gamepad(), release.with(), release.threshold());
            Button.Action action = noArgs(target, method, spec);
            resolve(input, spec).onRelease(spec.describe(method), action);
        }
        WhileHeld held = method.getAnnotation(WhileHeld.class);
        if (held != null) {
            Spec spec = new Spec(held, held.value(), held.gamepad(), held.with(), held.threshold());
            Button.Action action = noArgs(target, method, spec);
            resolve(input, spec).whileHeld(spec.describe(method), action);
        }
        OnToggle toggle = method.getAnnotation(OnToggle.class);
        if (toggle != null) {
            Spec spec = new Spec(toggle, toggle.value(), toggle.gamepad(), toggle.with(), toggle.threshold());
            Button.ToggleAction action = oneBoolean(target, method, spec);
            resolve(input, spec).onToggle(spec.describe(method), action);
        }
        OnLongPress longPress = method.getAnnotation(OnLongPress.class);
        if (longPress != null) {
            Spec spec = new Spec(longPress, longPress.value(), longPress.gamepad(), longPress.with(), longPress.threshold());
            requirePositive(spec, method, "ms", longPress.ms());
            Button.Action action = noArgs(target, method, spec);
            resolve(input, spec).onLongPress(spec.describe(method), longPress.ms(), action);
        }
        OnDoubleTap doubleTap = method.getAnnotation(OnDoubleTap.class);
        if (doubleTap != null) {
            Spec spec = new Spec(doubleTap, doubleTap.value(), doubleTap.gamepad(), doubleTap.with(), doubleTap.threshold());
            requirePositive(spec, method, "windowMs", doubleTap.windowMs());
            Button.Action action = noArgs(target, method, spec);
            resolve(input, spec).onDoubleTap(spec.describe(method), doubleTap.windowMs(), action);
        }
    }

    private static final class Spec {
        final String annotation;
        final Key key;
        final int gamepad;
        final Key[] modifiers;
        final double threshold;

        Spec(Annotation annotation, Key key, int gamepad, Key[] modifiers, double threshold) {
            this.annotation = "@" + annotation.annotationType().getSimpleName();
            this.key = key;
            this.gamepad = gamepad;
            this.modifiers = modifiers;
            this.threshold = threshold;
        }

        String describe(Method method) {
            StringBuilder sb = new StringBuilder(annotation).append('(').append(key);
            if (gamepad != 1) sb.append(", gamepad = ").append(gamepad);
            for (Key modifier : modifiers) sb.append(", with ").append(modifier);
            return sb.append(") ").append(method.getDeclaringClass().getSimpleName())
                    .append('.').append(method.getName()).append("()").toString();
        }
    }

    private static Button resolve(Input input, Spec spec) {
        if (spec.threshold <= 0 || spec.threshold > 1) {
            throw new BindingPanic(spec.annotation + "(" + spec.key + ") threshold must be in (0, 1], got " + spec.threshold);
        }
        Pad pad = input.pad(spec.gamepad);
        Button button = pad.button(spec.key, spec.threshold);
        for (Key modifier : spec.modifiers) {
            if (modifier == spec.key) {
                throw new BindingPanic(spec.annotation + "(" + spec.key + ") lists " + modifier + " as its own modifier.");
            }
            button = pad.button(modifier).and(button);
        }
        return button;
    }

    private static void requirePositive(Spec spec, Method method, String attribute, long value) {
        if (value <= 0) {
            throw new BindingPanic(spec.describe(method) + ": " + attribute + " must be > 0, got " + value);
        }
    }

    private static Button.Action noArgs(Object target, Method method, Spec spec) {
        checkShape(method, spec);
        if (method.getParameterTypes().length != 0) {
            throw new BindingPanic(spec.describe(method) + " must take no parameters.");
        }
        return () -> invoke(target, method);
    }

    private static Button.ToggleAction oneBoolean(Object target, Method method, Spec spec) {
        checkShape(method, spec);
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 1 || params[0] != boolean.class) {
            throw new BindingPanic(spec.describe(method) + " must take exactly one boolean (the new toggle state).");
        }
        return on -> invoke(target, method, on);
    }

    private static void checkShape(Method method, Spec spec) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            throw new BindingPanic(spec.describe(method) + " must not be static.");
        }
        if (Modifier.isPrivate(modifiers)) {
            throw new BindingPanic(spec.describe(method) + " must not be private; make it package-private.");
        }
        method.setAccessible(true);
    }

    private static void invoke(Object target, Method method, Object... args) throws Exception {
        try {
            method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }
}
