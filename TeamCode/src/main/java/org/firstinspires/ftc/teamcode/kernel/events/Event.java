package org.firstinspires.ftc.teamcode.kernel.events;

// Compared by identity: declare each event once as a static final constant.
public final class Event<T> {
    private final String name;

    private Event(String name) {
        this.name = name;
    }

    public static <T> Event<T> of(String name) {
        return new Event<>(name);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
