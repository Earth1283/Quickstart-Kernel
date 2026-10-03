# 11. Init tasks

At the end of `new Robot()` the kernel runs a list of *init tasks*. Each one returns `ok`, `warn` or `fail`, and every
result goes on the init screen with how long it took, so a dead odometry pod or a sagging battery shows up before the
match starts rather than during it.

## What runs, in order

| Task                  | Result                                                                          |
|-----------------------|---------------------------------------------------------------------------------|
| `Hubs`                | warns if no hubs are found (bulk caching wouldn't be active)                    |
| `Battery`             | warns below the profile's `lowBatteryVolts()` (default 12.0)                    |
| `Localizer` (critical)| fails if the pose is NaN                                                        |
| one per subsystem     | its `init()`: lift and turret report their zeroing, rangefinder reads once      |
| `profile.initTasks()` | whatever your robot needs                                                       |

A task that throws is recorded as a `FAIL`; it never stops the others. After all of them have run, any failed
**critical** task throws `InitFailedPanic` listing every failure, the same way a runtime panic would.

## Writing one

For the robot, in the profile:

```java
@Override
public List<InitTask> initTasks(Kernel kernel) {
    return Arrays.asList(
            InitTask.of("Wrist zero", () -> wristSwitchPressed(kernel)
                    ? InitResult.ok("switch pressed")
                    : InitResult.warn("wrist not at its zero switch")),
            InitTask.critical("Flywheel encoder", () -> InitResult.fail("cable unplugged")));
}
```

For one OpMode, from userspace, after `new Robot()`:

```java
robot.init("Camera", () -> camera.isReady() ? InitResult.ok() : InitResult.warn("still booting"));
```

For a mechanism, override `Subsystem.init()`:

```java
@Override
public InitResult init() {
    return InitResult.ok("zeroed at GROUND");
}
```

`InitResult.ok()`, `ok(detail)`, `warn(detail)` and `fail(detail)` are the whole vocabulary. Put the *reason* in the
detail; that line is what the driver team reads on the Driver Station.

## Showing it

```java
Robot robot = new Robot();
robot.awaitStart();      // instead of waitForStart()
```

`KernelOpMode` does this for you between `onInit()` and `onStart()`.

During init the screen lists every task, one line each, plus gamepad connection and the live pose. After start only
the warnings and failures remain, under the status header.
