# 10. Telemetry

`robot.telemetry` is the one place userspace puts things on the Driver Station screen. The kernel publishes it
from `tick()` about ten times a second, so you never call `telemetry.update()`.

```java
robot.telemetry.data("shots", shots);               // this loop only; same key again replaces
robot.telemetry.line("lined up, fire when ready");  // this loop only; identical lines collapse
Subscription w = robot.telemetry.watch("lift", () -> robot.lift.target());  // re-read every frame until cancelled
```

`watch` is the one to reach for: register it once and the value stays current without touching your loop.

## `@Watch`

The annotation form of `watch`, for methods and fields of the OpMode (or anything passed to `robot.bind(obj)`):

```java
@Watch("aiming") boolean aiming() { return robot.motion.isAiming(); }
@Watch private boolean slowMode;          // key defaults to the member name: "slowMode"
@Watch(value = "lift", order = -1) Lift.Level lift() { return robot.lift.target(); }
```

Each member is re-read every frame, exactly like `watch`. Java can't tell the kernel what order members were
declared in, so annotated lines are sorted by `order` (default 0, lower first), then alphabetically by key. The
OpMode is bound while the `Robot` is built, so its annotated lines come before anything added with `watch(...)`
later; an object passed to `robot.bind(obj)` gets its lines at that point.

Rules (checked at compile time by `KernelProcessor`, and again at runtime):

- not `static`;
- a method takes no parameters, returns the value to show, and isn't `private`. Fields may be `private`;
- one key per class.

A method that throws becomes a `BindingPanic` naming it, e.g. `@Watch(aiming) MyTeleOp.aiming() threw ...`.

## What the kernel shows by itself

Running:

```
KERNEL CompBot | BLUE | TeleOp | 1:23 left
loop 5.1 ms (196 Hz) | battery 12.6 V
pose (72.0, 48.3) 90° | FOLLOW | queued 2
[WARN] Battery 11.8 V, below 12.0 V (0 ms)     <- only init entries that weren't OK
claw open: true                                <- each subsystem's report()
lift target: HIGH
lift moving: false
... your watches, data, lines
```

Before start (`robot.awaitStart()` keeps this live) it shows the full init log, whether each gamepad is
connected, the live pose, and any Auto pose waiting to be handed to TeleOp. See [Init tasks](11-init-tasks.md).

## Reporting from a subsystem

Override `report` and the values appear under the subsystem's name:

```java
@Override
public void report(Report report) {
    report.data("angle", wristAngle);   // shows as "wrist angle: 0.42"
}
```

It runs only when a frame is due, not every tick, so building strings there is fine.

## Formatting

`Pose` prints as `(x, y) heading°`. `double` prints with two decimals. Everything else uses `String.valueOf`.

## Mixing with the SDK

`opMode.telemetry` still works, but the kernel calls `update()` on it, which sends whatever has been added. Prefer
`robot.telemetry` so frames aren't interleaved.
