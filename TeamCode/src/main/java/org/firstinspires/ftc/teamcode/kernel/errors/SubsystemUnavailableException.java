package org.firstinspires.ftc.teamcode.kernel.errors;

// Checked so interlocking drivers must decide what to do on a robot that lacks the other subsystem.
public class SubsystemUnavailableException extends Exception {
    public final Class<?> subsystem;

    public SubsystemUnavailableException(Class<?> subsystem, String profile) {
        super("Profile " + profile + " doesn't provide a " + subsystem.getSimpleName());
        this.subsystem = subsystem;
    }
}
