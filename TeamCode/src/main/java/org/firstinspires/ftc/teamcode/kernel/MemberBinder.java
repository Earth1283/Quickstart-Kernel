package org.firstinspires.ftc.teamcode.kernel;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;
import org.firstinspires.ftc.teamcode.kernel.telemetry.KernelTelemetry;
import org.firstinspires.ftc.teamcode.kernel.telemetry.Watch;
import org.firstinspires.ftc.teamcode.kernel.time.Every;
import org.firstinspires.ftc.teamcode.kernel.time.Scheduler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

// Binds @Watch and @Every; key bindings go through AnnotationBinder. Re-checks at runtime what
// KernelMemberProcessor checks at compile time, for builds that skip the processor.
final class MemberBinder {
    private static final class Watched {
        final String key;
        final int order;
        final Supplier<?> value;

        Watched(String key, int order, Supplier<?> value) {
            this.key = key;
            this.order = order;
            this.value = value;
        }
    }

    private final KernelTelemetry telemetry;
    private final Scheduler scheduler;
    private final List<Runnable> armAtStart = new ArrayList<>();
    private boolean started;

    MemberBinder(KernelTelemetry telemetry, Scheduler scheduler) {
        this.telemetry = telemetry;
        this.scheduler = scheduler;
    }

    void bind(Object target) {
        List<Watched> watched = new ArrayList<>();
        for (Class<?> type : UserClasses.of(target)) {
            for (Field field : type.getDeclaredFields()) {
                Watch watch = field.getAnnotation(Watch.class);
                if (watch != null) watched.add(watchField(target, field, watch));
            }
            for (Method method : type.getDeclaredMethods()) {
                Watch watch = method.getAnnotation(Watch.class);
                if (watch != null) watched.add(watchMethod(target, method, watch));
                Every every = method.getAnnotation(Every.class);
                if (every != null) schedule(target, method, every);
            }
        }
        watched.sort((a, b) -> a.order != b.order ? Integer.compare(a.order, b.order) : a.key.compareTo(b.key));
        Set<String> keys = new HashSet<>();
        for (Watched w : watched) {
            if (!keys.add(w.key)) {
                throw new BindingPanic("Two @Watch members in " + target.getClass().getSimpleName() + " use the key \""
                        + w.key + "\"; only one would show.");
            }
            telemetry.watch(w.key, w.value);
        }
    }

    // Idempotent; Robot calls it on every tick after start.
    void start() {
        if (started) return;
        started = true;
        for (Runnable arm : armAtStart) arm.run();
        armAtStart.clear();
    }

    private static Watched watchField(Object target, Field field, Watch watch) {
        String key = watch.value().isEmpty() ? field.getName() : watch.value();
        String description = "@Watch(" + key + ") " + field.getDeclaringClass().getSimpleName() + "." + field.getName();
        if (Modifier.isStatic(field.getModifiers())) throw new BindingPanic(description + " must not be static.");
        field.setAccessible(true);
        return new Watched(key, watch.order(), () -> read(target, field, description));
    }

    private static Watched watchMethod(Object target, Method method, Watch watch) {
        String key = watch.value().isEmpty() ? method.getName() : watch.value();
        String description = "@Watch(" + key + ") " + describe(method);
        requireNoArgInstanceMethod(method, description);
        if (method.getReturnType() == void.class) throw new BindingPanic(description + " must return the value to show.");
        return new Watched(key, watch.order(), () -> call(target, method, description));
    }

    private void schedule(Object target, Method method, Every every) {
        String description = "@Every(" + every.value() + ") " + describe(method);
        requireNoArgInstanceMethod(method, description);
        if (every.value() <= 0) throw new BindingPanic(description + ": the period must be > 0 ms, got " + every.value());
        Runnable arm = () -> scheduler.every(every.value(), () -> call(target, method, description));
        if (started || every.duringInit()) arm.run();
        else armAtStart.add(arm);
    }

    private static void requireNoArgInstanceMethod(Method method, String description) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) throw new BindingPanic(description + " must not be static.");
        if (Modifier.isPrivate(modifiers)) throw new BindingPanic(description + " must not be private; make it package-private.");
        if (method.getParameterTypes().length != 0) throw new BindingPanic(description + " must take no parameters.");
        method.setAccessible(true);
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()";
    }

    private static Object read(Object target, Field field, String description) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new BindingPanic(description + " couldn't be read: " + e, e);
        }
    }

    private static Object call(Object target, Method method, String description) {
        try {
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof KernelPanic) throw (KernelPanic) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new BindingPanic(description + " threw " + cause, cause);
        } catch (IllegalAccessException e) {
            throw new BindingPanic(description + " couldn't be called: " + e, e);
        }
    }
}
