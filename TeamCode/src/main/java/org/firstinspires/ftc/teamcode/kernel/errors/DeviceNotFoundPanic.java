package org.firstinspires.ftc.teamcode.kernel.errors;

import java.util.Collection;

public class DeviceNotFoundPanic extends KernelPanic {
    public final String deviceName;
    public final Class<?> deviceType;

    public DeviceNotFoundPanic(String deviceName, Class<?> deviceType, String profile, Collection<String> configured) {
        super("No " + deviceType.getSimpleName() + " named \"" + deviceName + "\" (wanted by profile " + profile + "). "
                + "Configured devices: " + join(configured) + ". "
                + "Fix the name in the profile, or add the device in the Driver Station's Configure Robot menu.");
        this.deviceName = deviceName;
        this.deviceType = deviceType;
    }

    // String.join needs API 26; the Control Hub build targets 24.
    private static String join(Collection<String> names) {
        if (names.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }
        return sb.toString();
    }
}
