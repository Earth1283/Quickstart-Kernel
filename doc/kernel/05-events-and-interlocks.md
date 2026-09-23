# 5. Events and interlocks

Two kinds of communication: **events** carry news from the kernel and subsystems to userspace (and to each
other), and **interlocks** let one driver ask another subsystem a question before acting.

## Events

An event is a typed constant declared on the interface that emits it:

```java
public interface Lift extends Subsystem {
    Event<Level> AT_TARGET = Event.of("lift.atTarget");
    ...
}
```

Subscribe from userspace:

```java
Subscription s = robot.on(Lift.AT_TARGET, level -> {
    if (level == Lift.Level.HIGH) robot.claw.open();
});
robot.on(Motion.QUEUE_EMPTY, () -> telemetry.addLine("done"));  // ignore the payload
s.cancel();
```

Built-in events:

| Event                 | Payload        | Emitted when                                           |
|-----------------------|----------------|--------------------------------------------------------|
| `Motion.PATH_DONE`    | `Path`         | a followed/queued path finishes                        |
| `Motion.QUEUE_EMPTY`  | `Void`         | the last queued path finishes                          |
| `Motion.TURN_DONE`    | `Double`       | `turnTo` reaches its heading (payload = target)        |
| `Lift.AT_TARGET`      | `Lift.Level`   | the lift arrives at a new target                       |
| `Claw.OPEN_REFUSED`   | `Void`         | `open()` was refused by the lift interlock             |

### Delivery rules

- `emit()` only queues. Handlers run at the **end** of the tick (stage 5), on your OpMode thread, in emit order.
- Events emitted *by a handler* are delivered on the **next** tick, so two handlers that trigger each other can't
  lock up the loop.
- A handler may cancel its own subscription while running.
- A handler that throws becomes a `BindingPanic` naming the event.
- When the robot stops (or panics), undelivered events are dropped.

### Emitting from a driver

```java
kernel.emit(AT_TARGET, target);
kernel.emit(OPEN_REFUSED);        // Event<Void>
```

## Interlocks

A driver can look up another subsystem through the `Kernel` it was constructed with:

```java
private boolean liftIsMoving() {
    try {
        return kernel.get(Lift.class).isMoving();
    } catch (SubsystemUnavailableException noLiftOnThisRobot) {
        return false;
    }
}

@Override
public void open() {
    if (liftIsMoving()) {
        kernel.emit(OPEN_REFUSED);
        return;
    }
    setOpen(true);
}
```

`Kernel.get` throws the **checked** `SubsystemUnavailableException` when the active profile doesn't provide that
subsystem. The compiler makes the driver decide what the interlock means on a robot without the other mechanism.

Look subsystems up at *call time* (as above), not in the constructor. Subsystems are constructed one at a time,
so the one you need may not exist yet during construction.

### Userspace reaction

Interlocks refuse silently by design (the driver shouldn't know about gamepads). Userspace decides what a
refusal means:

```java
robot.on(Claw.OPEN_REFUSED, () -> gamepad2.rumble(200));
```
