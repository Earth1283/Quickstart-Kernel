# 9. Lifecycle

## Construction: `new Robot()`

In order:

1. Find the running OpMode (or use the one passed to `new Robot(opMode)`).
2. Read `Profiles.ACTIVE`, then build the Pedro follower and wrap it in `PedroMotion`.
3. Build each subsystem from the profile and install it, or install a stand-in for `null`.
4. Attach both gamepads.
5. **Take over bulk caching**: every hub is set to `MANUAL` *after* all drivers are built, so nothing a driver
   did in its constructor can leave a hub in `AUTO` or `OFF`.
6. Set the alliance to `BLUE` (which also sets the field-centric forward).
7. If this OpMode is **not** `@Autonomous`, take the pose and alliance saved by the last Auto, if there is one.
8. If the OpMode class (or a superclass) has `@PlayAs(...)`, set that alliance. It wins over the handoff.
9. Register a stop listener with the SDK.
10. Bind the OpMode's annotated members: key bindings, `@Watch`, and `@Every` (armed now if `duringInit`,
    otherwise on the first tick after start).
11. Run the boot checks and the profile's init tasks ([Init tasks](11-init-tasks.md)); a failed critical one
    panics here, after the init screen has been published.

## Waiting for start

`KernelOpMode` does this for you between `onInit()` and `onStart()`, and redirects any `waitForStart()` call to
it. In a plain `LinearOpMode`, use `robot.awaitStart()` instead of `waitForStart()`. It ticks while it waits, so
the init screen stays live (gamepads, battery, pose) and gamepad state keeps sampling. Key bindings and the match
clock arm at start. In an iterative OpMode, call `robot.tick()` from `init_loop()` for the same effect.

## KernelOpMode

```java
public final void runOpMode() throws InterruptedException {
    robot = new Robot(this);
    onInit();
    robot.awaitStart();
    onStart();
    while (opModeIsActive()) {
        onLoop();
        robot.tick();
    }
}
```

`runOpMode()` is final; override the hooks. An Auto puts its sequence in `onStart()`; after it returns, the loop
keeps ticking until stop so the robot holds position and the pose is saved on stop. `LinearOpMode.sleep()` is
final in the SDK, so `KernelOpMode` can't stop you calling it, and it freezes the kernel. Use `robot.sleep()`.

## Bulk caching

The kernel owns bulk caching. Each tick starts by clearing every hub's cache, so the first read of any encoder,
motor position or digital input that tick triggers one bulk read per hub, and every later read that tick is free.
Don't call `setBulkCachingMode` or `clearBulkCache` yourself.

## Pose handoff (Auto → TeleOp)

- When an `@Autonomous` OpMode stops (normally, by timer, or by the stop button), the kernel saves
  `motion.pose()` and `robot.alliance()` to `PoseStore`.
- The next non-Autonomous `new Robot()` takes both, **once**: it sets the alliance (so field-centric forward and
  the light bars match the Auto) and calls `motion.setPose`.
- An explicit `robot.motion.setPose(...)` or `robot.alliance(...)` in TeleOp always overrides it.
- `PoseStore` is a static field. It survives from one OpMode to the next but not an app restart.
- If you move the robot by hand between Auto and TeleOp, set the pose yourself.

`PoseStore.peek()` and `PoseStore.peekAlliance()` show what's saved without consuming it (useful on an
init-time telemetry screen).

## Safe stop

When the OpMode stops, the SDK's event-loop thread calls the kernel's listener, which:

1. marks the robot stopped (later `tick()` calls do nothing, and a blocked `waitUntil` throws
   `OpModeStoppedException`);
2. saves the pose and alliance if this was an Auto;
3. stops motion (`follower.stop()`, then the drivetrain directly, so it doesn't wait for another update) and calls
   every subsystem's `stop()`;
4. drops pending events and unregisters itself.

The SDK holds listeners by weak reference, so `Robot` keeps a strong reference to its own. Otherwise the garbage
collector could remove the listener mid-match and the stop would never arrive.

The same stop sequence runs after any panic. See [Errors](08-errors.md#3-runtime-panics).

## Iterative OpModes

`init()` → `new Robot(this)`. `loop()` → your logic, then `robot.tick()`. `stop()` doesn't need to do anything,
since the listener handles it.
