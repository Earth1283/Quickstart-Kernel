# 3. Motion

`robot.motion` is the drivetrain plus localization, backed by Pedro's `Follower`. Pedro types (`Pose`, `Path`,
`Vector2D`) are used directly.

## Modes

`robot.motion.mode()` is always one of:

| Mode     | Entered by                                   | Left when                                      |
|----------|----------------------------------------------|------------------------------------------------|
| `IDLE`   | start, `cancel()`, a path ending without hold | anything else is requested                    |
| `DRIVE`  | `drive(...)`                                 | `follow`, `queue`, `turnTo`, `hold`, `cancel`  |
| `FOLLOW` | `follow(path)`, `queue(...)`                 | the queue empties, or driver input takes over  |
| `HOLD`   | `hold()`, a path ending, `aimTo` while idle, a finished turn | anything else is requested |
| `TURN`   | `turnTo(heading)`                            | within tolerance (→ `HOLD`), or takeover       |

`isBusy()` is true in `FOLLOW` and `TURN`, the modes that finish on their own.

## Driving

```java
robot.motion.drive(forward, left, counterClockwise);
```

All three are in [-1, 1]. Rules:

- **Call it every loop.** Drive input is consumed each tick. If your loop stops calling `drive()`, the next tick
  sends zero power. A loop that stops talking to the drivetrain doesn't keep driving it.
- **Driver takeover.** While a path, turn or hold is running, any input bigger than
  `MotionTuning.driverTakeoverThreshold` (default 0.05) on any axis cancels it and switches to `DRIVE`, which
  also clears the path queue. Smaller input is ignored, so resting thumbs don't break automation.
- **Field-centric** (default, `MotionTuning.fieldCentric`). "Forward" means the *driver forward* direction,
  which is the field's +x on blue and −x on red (set by `robot.alliance(...)`). `robot.motion.resetDriverForward()`
  redefines it as wherever the robot is facing now.

## Paths

```java
robot.motion.follow(path);          // clear the queue, follow this now
robot.motion.queue(p1, p2, p3);     // append; starts immediately if nothing is following
robot.motion.queued();              // paths waiting behind the current one
```

When a path finishes, the kernel emits `Motion.PATH_DONE` (payload: that `Path`, compare by identity) and starts
the next one. When the last one finishes it emits `Motion.QUEUE_EMPTY` and ends in `HOLD` (or `IDLE` if Pedro's
`holdEnd` is off).

```java
robot.on(Motion.PATH_DONE, path -> {
    if (path == toBasket) robot.claw.open();
});
robot.motion.queue(toBasket, toSample, backToBasket);
robot.waitForMotion();
```

Build paths with Pedro's `com.pedropathing.api.Paths` (`line`, `curve`, `through`, `path`). Always give a path
a heading interpolator (`.constant(h)`, `.linear(a, b)`, `.tangent()`, `.facingPoint(p)`). Pedro throws if the
follower asks for the heading of a path that has none.

## Aiming

```java
robot.motion.aimTo(point);   // Vector2D, field coordinates
robot.motion.stopAiming();
robot.motion.isAiming();
```

What aiming does depends on the mode:

- **`DRIVE`:** the kernel owns heading. Translation still comes from `drive()`, but its turn argument is replaced
  by a PD controller pointing the robot at `point` (`aimKp`, `aimKd`, clamped to `aimMaxTurn`). This is
  "drive while locked on".
- **`IDLE`/`HOLD`:** the robot holds its current x/y and turns to face `point` (a Pedro hold).
- **`FOLLOW`/`TURN`:** the target is remembered, and when the queue empties the robot holds facing it. To aim
  *during* a path, give the path `.facingPoint(point)` instead.

Calling `aimTo` every tick with the same point (e.g. from `@WhileHeld`) is fine. It only re-plans when the
point changes.

## Turning and holding

```java
robot.motion.turnTo(Math.toRadians(90)); // TURN until within turnToleranceRadians, then HOLD; emits TURN_DONE
robot.motion.hold();                     // HOLD current pose
robot.motion.cancel();                   // clear queue, stop aiming, stop motors → IDLE
```

## Pose

```java
robot.motion.setPose(start);  // tell the localizer where the robot is
Pose here = robot.motion.pose();
```

## Alliances

Write every Auto once, in **blue** coordinates, and pass every field coordinate through the alliance helpers:

```java
robot.alliance(Alliance.RED);               // usually from an init-time selector
Alliance side = robot.alliance();
Pose start = side.pose(9, 60, 0);           // blue: (9, 60, 0)   red: (135, 60, π)
Vector2D goal = side.point(12, 132);
double facing = side.heading(Math.PI / 2);
```

On blue the helpers return their input unchanged. On red they mirror it across the field's vertical centerline:
`(x, y, h) → (144 − x, y, π − h)`. The motion API itself never mirrors anything. It takes and returns real field
coordinates. Mirroring happens in exactly one place, the helpers, so nothing can get mirrored twice.

If this season's field is point-symmetric instead, change `Alliance.mirror(Pose)`, `mirror(Vector2D)` and
`mirrorHeading` together.

Setting the alliance also sets the field-centric driver forward (+x for blue, −x for red).

## Tuning knobs

`MotionTuning` (returned by your profile's `motionTuning()`):

| Field                     | Default | Meaning                                                      |
|---------------------------|---------|--------------------------------------------------------------|
| `fieldCentric`            | `true`  | interpret `drive()` in the driver frame                      |
| `aimKp`                   | `1.2`   | turn power per radian of aim error                           |
| `aimKd`                   | `0.08`  | turn power per rad/s of aim-error change                     |
| `aimMaxTurn`              | `0.8`   | clamp on aim turn power                                      |
| `turnToleranceRadians`    | 2°      | `turnTo` counts as done within this                          |
| `driverTakeoverThreshold` | `0.05`  | driver input above this interrupts FOLLOW/TURN/HOLD          |
