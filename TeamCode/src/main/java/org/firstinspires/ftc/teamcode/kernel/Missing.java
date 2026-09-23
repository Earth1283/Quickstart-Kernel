package org.firstinspires.ftc.teamcode.kernel;

import org.firstinspires.ftc.teamcode.kernel.errors.ProfileMisconfiguredPanic;

import java.lang.reflect.Proxy;

// Stands in for a subsystem the active profile doesn't provide, so a practice bot without a lift
// still boots; the panic only fires if userspace actually uses robot.lift.
final class Missing {
    private Missing() {}

    static <T extends Subsystem> T subsystem(Class<T> type, String profileName) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (self, method, args) -> {
            switch (method.getName()) {
                case "update":
                case "stop":
                    return null;
                case "toString":
                    return "Missing " + type.getSimpleName();
                case "hashCode":
                    return System.identityHashCode(self);
                case "equals":
                    return self == args[0];
                default:
                    throw new ProfileMisconfiguredPanic("robot." + lowerFirst(type.getSimpleName()) + "." + method.getName()
                            + "() was called, but profile " + profileName + " doesn't provide a " + type.getSimpleName()
                            + ". Guard it with robot.has(" + type.getSimpleName() + ".class) or add one to the profile.");
            }
        });
        return type.cast(proxy);
    }

    private static String lowerFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
