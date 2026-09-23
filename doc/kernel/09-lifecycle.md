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
7. If this OpMode is **not** `@Autonomous`, take the pose saved by the last Auto, if there is one.
8. Register a stop listener with the SDK.
9. Bind the OpMode's annotated methods.

## Bulk caching

The kernel owns bulk caching. Each tick starts by clearing every hub's cache, so the first read of any encoder,
motor position or digital input that tick triggers one bulk read per hub, and every later read that tick is free.
Don't call `setBulkCachingMode` or `clearBulkCache` yourself.

## Pose handoff (Auto → TeleOp)

- When an `@Autonomous` OpMode stops (normally, by timer, or by the stop button), the kernel saves
  `motion.pose()` to `PoseStore`.
- The next non-Autonomous `new Robot()` takes that pose, **once**, and calls `motion.setPose` with it.
- An explicit `robot.motion.setPose(...)` in TeleOp always overrides it.
- `PoseStore` is a static field. It survives from one OpMode to the next but not an app restart.
- If you move the robot by hand between Auto and TeleOp, set the pose yourself.

`PoseStore.peek()` shows what's saved without consuming it (useful on an init-time telemetry screen).

## Safe stop

When the OpMode stops, the SDK's event-loop thread calls the kernel's listener, which:

1. marks the robot stopped (later `tick()` calls do nothing, and a blocked `waitUntil` throws
   `OpModeStoppedException`);
2. saves the pose if this was an Auto;
3. stops motion (`follower.stop()`, then the drivetrain directly, so it doesn't wait for another update) and calls
   every subsystem's `stop()`;
4. drops pending events and unregisters itself.

The SDK holds listeners by weak reference, so `Robot` keeps a strong reference to its own. Otherwise the garbage
collector could remove the listener mid-match and the stop would never arrive.

The same stop sequence runs after any panic. See [Errors](08-errors.md#3-runtime-panics).

## Iterative OpModes

`init()` → `new Robot(this)`. `loop()` → your logic, then `robot.tick()`. `stop()` doesn't need to do anything,
since the listener handles it.
