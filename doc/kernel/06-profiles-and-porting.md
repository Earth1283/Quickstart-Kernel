# 6. Profiles and porting

A **profile** describes one physical robot: how to build its Pedro follower, which mechanisms it has, and which
drivers implement them. OpModes never mention a profile.

## Choosing the active robot

```java
// kernel/profiles/Profiles.java
public static RobotProfile ACTIVE = new CompBot();
```

Change that one line (or assign it from an init-time selector *before* `new Robot()`) to run the same OpModes
on a different robot.

## RobotProfile

```java
public interface RobotProfile {
    Follower follower(HardwareMap hardwareMap);          // required
    default Claw claw(Kernel kernel) { return null; }    // null = this robot has no claw
    default Lift lift(Kernel kernel) { return null; }
    default Intake intake(Kernel kernel) { return null; }
    default Flywheel flywheel(Kernel kernel) { return null; }
    default Turret turret(Kernel kernel) { return null; }
    default Rangefinder rangefinder(Kernel kernel) { return null; }
    default Map<Class<? extends Subsystem>, Subsystem> custom(Kernel kernel) { return emptyMap(); }
    default List<InitTask> initTasks(Kernel kernel) { return emptyList(); }
    default double lowBatteryVolts() { return 12.0; }
    default MotionTuning motionTuning() { return new MotionTuning(); }
    default double stickDeadband() { return 0.05; }
    default double stickCurve() { return 1.0; }          // response curve for every stick; >1 = finer near center
    default String name() { return getClass().getSimpleName(); }
}
```

## The stock mechanisms

| Interface     | Driver                | Userspace                                                        | Events                         |
|---------------|-----------------------|------------------------------------------------------------------|--------------------------------|
| `Claw`        | `ServoClaw`           | `open()`, `close()`, `toggle()`                                  | `OPEN_REFUSED`                 |
| `Lift`        | `MotorLift`           | `goTo(Level)`; every `Level` needs a `ticks` entry or boot panics | `AT_TARGET`                    |
| `Intake`      | `MotorIntake`         | `in()`, `out()`, `idle()`, `mode()`                              | `JAMMED` (sustained over-current) |
| `Flywheel`    | `MotorFlywheel`       | `spinTo(ticksPerSecond)`, `spinDown()`, `velocity()`, `isReady()` | `READY` (held within tolerance) |
| `Turret`      | `MotorTurret`         | `turnTo(radians)`, `angle()`, `isMoving()`                       | `AT_ANGLE`                     |
| `Rangefinder` | `DistanceRangefinder` | `inches()`, `isDetecting()`                                      | `DETECTED`, `CLEARED`          |

```java
@Override
public Flywheel flywheel(Kernel kernel) {
    MotorFlywheel.Config config = new MotorFlywheel.Config();
    config.motorName = "shooter";
    return new MotorFlywheel(kernel, config);
}
```

Each is a `robot.` field (`robot.flywheel.spinTo(1500)`) and a stand-in panics only if used on a robot without it.
`Lift`, `Turret` and `Flywheel` commands return the subsystem, and all three can be passed to `robot.await(...)`:
a lift or turret has settled when it's within tolerance of its target, and a flywheel when it's `isReady()` (or
told to stop).

For a mechanism the kernel has no interface for, return it from `custom()` and fetch it with
`robot.get(Wrist.class)` (checked `SubsystemUnavailableException` if the profile lacks it).

## Adding a second robot

```java
public class PracticeBot extends CompBot {
    @Override
    public Lift lift(Kernel kernel) {
        return null;                         // no lift on the practice bot
    }

    @Override
    public Claw claw(Kernel kernel) {
        return new ServoClaw(kernel, "grabber", 0.6, 0.2);   // different name and positions
    }
}
```

Extending `CompBot` reuses its drivetrain config. Implement `RobotProfile` directly if the drivetrain is
different too, with its own tuned configs.

## Robots without a mechanism

Returning `null` from a factory is allowed. The robot still boots, and:

- `robot.lift` is a stand-in that panics with a clear message if userspace actually calls it;
- `robot.has(Lift.class)` returns `false`, so shared OpModes can guard optional mechanisms;
- `kernel.get(Lift.class)` throws `SubsystemUnavailableException`, so interlocks degrade gracefully.

```java
if (robot.has(Lift.class)) robot.lift.goTo(Lift.Level.HIGH);
```

## Per-robot tuning

```java
@Override
public MotionTuning motionTuning() {
    MotionTuning tuning = new MotionTuning();
    tuning.aimKp = 1.6;
    tuning.fieldCentric = false;
    return tuning;
}
```

## Porting to next season

1. Keep `kernel/` except `subsystems/`, `drivers/`, and the season fields on `Robot` (`claw`, `lift`).
2. Write the new season's subsystem interfaces and drivers ([Writing subsystems](07-writing-subsystems.md)).
3. Add their factories to `RobotProfile` and fields to `Robot`.
4. Re-run the tuners and paste the configs into the new profile.
5. Check `Alliance`'s mirror still matches the field's symmetry.
