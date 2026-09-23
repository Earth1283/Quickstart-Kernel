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
    default MotionTuning motionTuning() { return new MotionTuning(); }
    default double stickDeadband() { return 0.05; }
    default String name() { return getClass().getSimpleName(); }
}
```

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
