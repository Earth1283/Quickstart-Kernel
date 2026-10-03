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

`isBusy()` is true in `FOLLOW` and `TURN`, the modes that finish on their own. `isSettled()` is its opposite,
and it's what `robot.await(robot.motion)` waits for.

## Driving

```java
robot.motion.drive(robot.gp1.leftStick, robot.gp1.rightStick);  // translate stick, rotate stick
robot.motion.drive(forward, left, counterClockwise);            // or raw numbers
```

All three numbers are in [-1, 1]. The stick form reads `translate.up()`, `translate.left()` and
`rotate.left()`, with the profile's deadband and `stickCurve()` already applied. Rules:

- **Call it every loop.** Drive input is consumed each tick. If your loop stops calling `drive()`, the next tick
  sends zero power. A loop that stops talking to the drivetrain doesn't keep driving it.
- **Driver takeover.** While a path, turn or hold is running, any input bigger than
  `MotionTuning.driverTakeoverThreshold` (default 0.05) on any axis cancels it and switches to `DRIVE`, which
  also clears the path queue. Smaller input is ignored, so resting thumbs don't break automation.
- **Field-centric** (default, `MotionTuning.fieldCentric`). "Forward" means the *driver forward* direction,
  which is the field's +x on blue and −x on red (set by `robot.alliance(...)`). `robot.motion.resetDriverForward()`
  redefines it as wherever the robot is facing now.
- **Speed scale.** `robot.motion.setSpeedScale(0.35)` multiplies driver input (all three axes) until you set it
  back to `1.0`. It's for slow mode. Paths, turns and aiming keep full speed, and takeover is judged on the
  unscaled input, so slow mode doesn't make it harder to interrupt automation.

## Paths

```java
robot.motion.follow(path);          // clear the queue, follow this now
robot.motion.queue(p1, p2, p3);     // append; starts immediately if nothing is following
robot.motion.queued();              // paths waiting behind the current one
```

Commands (`follow`, `queue`, `goTo`, `turnTo`, `turnBy`, `hold`) return `robot.motion`, so in a linear Auto a
step and its wait fit on one line, and other subsystems can join the same wait:

```java
robot.await(robot.motion.follow(toBasket), robot.lift.goTo(Lift.Level.HIGH));
robot.claw.open();
robot.await(robot.motion.follow(toSample));
```

When a path finishes, the kernel emits `Motion.PATH_DONE` (payload: that `Path`, compare by identity) and starts
the next one. When the last one finishes it emits `Motion.QUEUE_EMPTY` and ends in `HOLD` (or `IDLE` if Pedro's
`holdEnd` is off).

Reach for `PATH_DONE` when something must happen mid-queue without stopping, e.g. in TeleOp or when chaining
paths with no pause. In a linear Auto, the step-by-step `await` style above is easier to read.

### Building paths

`Route` covers the common case: straight segments with the heading turning evenly from one pose to the next.

```java
Path toBasket = Route.line(start, basket);
robot.motion.queue(Route.through(start, basket, sample, basket));  // one leg per consecutive pair
```

For curves or other heading behaviour, build paths with Pedro's `com.pedropathing.api.Paths` (`line`, `curve`,
`through`, `path`). Always give a path a heading interpolator (`.constant(h)`, `.linear(a, b)`, `.tangent()`,
`.facingPoint(p)`). Pedro throws if the follower asks for the heading of a path that has none.

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

## Going somewhere

```java
robot.await(robot.motion.goTo(side.pose(30, 110, Math.toRadians(90))));  // straight line from here, heading blends to the target's
robot.await(robot.motion.turnBy(Math.toRadians(-45)));                   // relative turn; wraps, so 170° + 20° is fine
```

`goTo` is `follow` with the path written for you: it replaces whatever is queued, runs in `FOLLOW`, and emits
`PATH_DONE` / `QUEUE_EMPTY` like any other path. If the target is closer than `goToMinDistance` (default 0.5 in) it
turns in place instead, so it emits `TURN_DONE` rather than `PATH_DONE`. Use `follow` or `queue` when you need a curve
or several segments.

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
robot.alliance(Alliance.RED);               // or @PlayAs(Alliance.RED) on the OpMode class
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

### `@PlayAs`

Rather than calling `robot.alliance(...)`, annotate the OpMode class. Red and blue Autos then share one class:

```java
@Autonomous(name = "Example Auto", group = "Kernel")
@PlayAs(Alliance.BLUE)
public class ExampleAuto extends KernelOpMode { ... }

@Autonomous(name = "Example Auto (Red)", group = "Kernel")
@PlayAs(Alliance.RED)
public class ExampleRedAuto extends ExampleAuto {}
```

The alliance is set inside `new Robot()`, so by the time `onInit()` runs, `robot.alliance()` is already right
and every pose built there lands on the correct side. A subclass inherits `@PlayAs` and can override it with
its own. On a TeleOp it wins over the alliance handed over from Auto. A later `robot.alliance(...)` call still
overrides it; don't combine the two in one OpMode, or the call silently undoes the annotation.

Setting the alliance also sets the field-centric driver forward (+x for blue, −x for red). The alliance an
Auto ran with is handed to the next TeleOp along with the pose (see [Lifecycle](09-lifecycle.md)).

## Tuning knobs

`MotionTuning` (returned by your profile's `motionTuning()`):

| Field                     | Default | Meaning                                                      |
|---------------------------|---------|--------------------------------------------------------------|
| `fieldCentric`            | `true`  | interpret `drive()` in the driver frame                      |
| `aimKp`                   | `1.2`   | turn power per radian of aim error                           |
| `aimKd`                   | `0.08`  | turn power per rad/s of aim-error change                     |
| `aimMaxTurn`              | `0.8`   | clamp on aim turn power                                      |
| `turnToleranceRadians`    | 2°      | `turnTo` counts as done within this                          |
| `goToMinDistance`         | 0.5 in  | `goTo` closer than this turns in place instead               |
| `driverTakeoverThreshold` | `0.05`  | driver input above this interrupts FOLLOW/TURN/HOLD          |
