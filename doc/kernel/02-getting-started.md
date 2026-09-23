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
public class MyTeleOp extends LinearOpMode {
    private Robot robot;

    @Override
    public void runOpMode() throws InterruptedException {
        robot = new Robot();
        waitForStart();
        while (opModeIsActive()) {
            robot.motion.drive(
                    robot.gp1.leftStick.up(),
                    robot.gp1.leftStick.left(),
                    robot.gp1.rightStick.left());
            robot.tick();
        }
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

- `new Robot()` takes no arguments. It finds the running OpMode itself. (`new Robot(this)` also works.)
- The annotated methods are bound automatically, because `new Robot()` scans the OpMode it attaches to.
- `robot` is a field, not a local, so the binding methods can use it.
- Sticks are already sign-corrected: `up()` is positive when pushed up, `left()` is positive when pushed left.
  That matches Pedro's convention (forward = +x, left = +y, counter-clockwise = +heading).
- Drive is **field-centric** by default. Hold BACK for 600 ms in `ExampleTeleOp` to re-zero it, or set
  `fieldCentric = false` in the profile's `motionTuning()`.

## 2. Your first Auto

```java
@Autonomous(name = "My Auto")
public class MyAuto extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Robot robot = new Robot();
        Alliance side = robot.alliance();

        Pose start = side.pose(9, 60, 0);
        Pose goal = side.pose(30, 110, Math.toRadians(90));
        Path toGoal = line(start, goal).linear(start, goal);

        robot.motion.setPose(start);
        waitForStart();

        robot.lift.goTo(Lift.Level.HIGH);
        robot.motion.follow(toGoal);
        robot.waitForMotion();
        robot.claw.open();
        robot.sleep(300);
    }
}
```

`waitForMotion()` and `sleep()` keep calling `tick()` for you, so the follower, lift and claw keep running
while you wait. If the driver presses stop mid-wait they throw `OpModeStoppedException`, and your Auto
unwinds cleanly through `runOpMode()`'s existing `throws InterruptedException`.

When this Auto stops, the kernel saves the robot's pose. The next TeleOp's `new Robot()` starts from it.

## 3. Iterative OpModes

`OpMode` (with `init()`/`loop()`) works too. Create the `Robot` in `init()` and call `robot.tick()` at the end
of `loop()`. The blocking helpers (`waitUntil`, `sleep`, `waitForMotion`) panic in iterative OpModes, because
blocking `loop()` would freeze the SDK. Poll `robot.motion.isBusy()` instead.

## Next

- [Motion](03-motion.md) for queues, aiming and alliances.
- [Input](04-input.md) for every binding style.
- [Profiles and porting](06-profiles-and-porting.md) when you add a second robot.
