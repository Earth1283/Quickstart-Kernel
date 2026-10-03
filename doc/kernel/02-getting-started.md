# 2. Getting started

## 0. Tune the drivetrain

`profiles/CompBot.java` has three `null` configs. Run the **Tuning** OpMode's procedures (Mecanum, your
localizer, Foresight) and paste each generated config over the matching `null`:

```java
public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> { ... });   // Mecanum Tuner
public static PinpointConfig localizerConfig = new PinpointConfig(c -> { ... });  // Pinpoint Tuner
public static ForesightConfig foresightConfig = new ForesightConfig(c -> { ... }); // Foresight Tuner
```

If you use a different localizer (OTOS, OctoQuad, two/three-wheel), change the field type and the
`new PinpointLocalizer(...)` line in `CompBot.follower()` to match. Until these are set, `new Robot()` panics
with a message telling you which tuner to run.

`pedro/Constants.create()` now returns `Profiles.ACTIVE.follower(h)`, so the tuners and the kernel share the
same configuration.

## 1. Your first TeleOp

```java
@TeleOp(name = "My TeleOp")
public class MyTeleOp extends KernelOpMode {
    @Override
    protected void onLoop() {
        robot.motion.drive(robot.gp1.leftStick, robot.gp1.rightStick);
    }

    @OnPress(Key.A)
    void toggleClaw() {
        robot.claw.toggle();
    }

    @OnPress(value = Key.DPAD_UP, gamepad = 2)
    void liftHigh() {
        robot.lift.goTo(Lift.Level.HIGH);
    }
}
```

Things to notice:

- `KernelOpMode` creates `robot` for you, waits for start while keeping the init screen live, and calls
  `robot.tick()` after every `onLoop()`. You can't forget the tick.
- It has three hooks, all optional: `onInit()` (before start), `onStart()` (once, at start) and `onLoop()`
  (every loop until stop).
- The annotated methods are bound automatically, because the `Robot` scans the OpMode it attaches to.
- `drive(translate, rotate)` reads the left stick as forward/left and the right stick as turn. Sticks are
  already sign-corrected: `up()` is positive when pushed up, `left()` is positive when pushed left. That matches
  Pedro's convention (forward = +x, left = +y, counter-clockwise = +heading). `drive(forward, left, ccw)` takes
  raw numbers if you need to mix sources.
- Inside the hooks, use `robot.sleep(ms)`, never `sleep(ms)`. The SDK's `sleep()` is final, so the kernel can't
  intercept it, and it stops the robot ticking. `waitForStart()` is safe: `KernelOpMode` redirects it to
  `robot.awaitStart()`.
- Drive is **field-centric** by default. Hold BACK for 600 ms in `ExampleTeleOp` to re-zero it, or set
  `fieldCentric = false` in the profile's `motionTuning()`.

## 2. Your first Auto

```java
@Autonomous(name = "My Auto")
public class MyAuto extends KernelOpMode {
    private Pose start, goal;

    @Override
    protected void onInit() {
        Alliance side = robot.alliance();
        start = side.pose(9, 60, 0);
        goal = side.pose(30, 110, Math.toRadians(90));
        robot.motion.setPose(start);
    }

    @Override
    protected void onStart() throws InterruptedException {
        robot.await(robot.motion.follow(Route.line(start, goal)), robot.lift.goTo(Lift.Level.HIGH));
        robot.claw.open();
    }
}
```

Motion, lift, turret and flywheel commands return their subsystem, and `robot.await(...)` blocks until
everything it's given has settled. So the line above drives and raises the lift in parallel, and moves on
once both are done. `await()` and `sleep()` keep calling `tick()` for you, so the follower, lift and claw keep
running while you wait. If the driver presses stop mid-wait they throw `OpModeStoppedException`, and your
Auto unwinds cleanly. Once `onStart()` returns, `KernelOpMode` keeps ticking until stop, so the robot holds
its final pose.

`Route.line(a, b)` is a straight path that turns from `a`'s heading to `b`'s along the way.

For the red version, don't copy the class: subclass it and annotate it.

```java
@Autonomous(name = "My Auto (Red)")
@PlayAs(Alliance.RED)
public class MyRedAuto extends MyAuto {}
```

`side.pose(...)` then mirrors every pose onto the red side. See [Alliances](03-motion.md#alliances).

When this Auto stops, the kernel saves the robot's pose and alliance. The next TeleOp starts from both, so
field-centric forward is right for red without any code in TeleOp.

## 3. Without KernelOpMode

A plain `LinearOpMode` works too: call `new Robot()` (it finds the running OpMode itself), use
`robot.awaitStart()` in place of `waitForStart()`, and call `robot.tick()` once per loop. Keep `robot` in a
field, not a local, so annotated binding methods can reach it.

`OpMode` (with `init()`/`loop()`) works as well. Create the `Robot` in `init()` and call `robot.tick()` at the
end of `loop()`. The blocking helpers (`waitUntil`, `sleep`, `await`) panic in iterative OpModes, because
blocking `loop()` would freeze the SDK. Poll `robot.motion.isSettled()` instead.

## Next

- [Motion](03-motion.md) for queues, aiming and alliances.
- [Input](04-input.md) for every binding style.
- [Profiles and porting](06-profiles-and-porting.md) when you add a second robot.
