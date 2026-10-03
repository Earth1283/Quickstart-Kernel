package org.firstinspires.ftc.teamcode.kernel.errors;

/**
 * {@code robot.tick()} was called while a tick was already running. That usually means a key
 * binding or event handler called {@code tick()}, {@code waitUntil()}, {@code sleep()}, or
 * {@code await()}. Handlers run <em>inside</em> a tick, so they must return quickly.
 * Set a flag in the handler and act on it in your main loop instead.
 */
public class TickReentrancyPanic extends KernelPanic {
    public TickReentrancyPanic() {
        super("robot.tick() was called from inside a tick. A key binding or event handler probably called "
                + "tick(), waitUntil(), sleep() or await(). Handlers must not block; set a flag and "
                + "handle it in your main loop.");
    }
}
