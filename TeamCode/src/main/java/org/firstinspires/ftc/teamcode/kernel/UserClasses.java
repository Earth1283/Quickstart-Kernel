package org.firstinspires.ftc.teamcode.kernel;

import java.util.ArrayList;
import java.util.List;

public final class UserClasses {
    private UserClasses() {}

    // The target's class and its superclasses, stopping at the SDK's own classes.
    public static List<Class<?>> of(Object target) {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> type = target.getClass(); isUserClass(type); type = type.getSuperclass()) classes.add(type);
        return classes;
    }

    private static boolean isUserClass(Class<?> type) {
        if (type == null || type == Object.class) return false;
        String name = type.getName();
        return !name.startsWith("com.qualcomm.")
                && !name.startsWith("org.firstinspires.ftc.robotcore.")
                && !name.startsWith("java.")
                && !name.startsWith("android.");
    }
}
