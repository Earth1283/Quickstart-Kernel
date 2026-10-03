# 8. Errors

The kernel fails loudly, early, and with instructions. There are three layers:

1. **Compile-time errors**: misuse of the kernel's annotations never reaches the robot.
2. **Checked exceptions**: situations userspace or drivers must consciously handle.
3. **Runtime panics**: unrecoverable problems. Outputs stop and the Driver Station says why.

## 1. Compile-time checks (`KernelProcessor`)

`KernelProcessor` is an annotation processor wired into TeamCode with
`annotationProcessor project(':KernelProcessor')`. It checks every `@OnPress`, `@OnRelease`, `@WhileHeld`,
`@OnToggle`, `@OnLongPress` and `@OnDoubleTap` and fails the build on:

| Rule                                                  | Example message (abridged)                                                  |
|-------------------------------------------------------|-----------------------------------------------------------------------------|
| not `static`                                          | `must not be static; bindings run on the OpMode instance.`                  |
| not `private`                                         | `must not be private; make it package-private (drop the 'private').`       |
| no parameters (except `@OnToggle`)                    | `must take no parameters.`                                                  |
| `@OnToggle` takes exactly one `boolean`               | `must take exactly one boolean parameter (the new toggle state).`          |
| `gamepad` is 1 or 2                                   | `gamepad must be 1 or 2, got 3.`                                            |
| `threshold` only on triggers                          | `threshold only applies to LEFT_TRIGGER and RIGHT_TRIGGER, not Y.`          |
| `threshold` in (0, 1]                                 | `threshold must be in (0, 1], got 1.5.`                                     |
| key isn't its own modifier                            | `lists A in 'with'; a key can't be its own modifier.`                      |
| `ms` / `windowMs` > 0                                 | `ms must be > 0, got 0.`                                                    |
| one binding per kind per control per class            | `duplicates another @OnPress for gp1.DPAD_UP in this class; ...`           |

Real output from a Gradle build with a deliberately broken class:

```
BrokenBindings.java:8: error: [kernel] @OnPress(A) BrokenBindings.isStatic() must not be static; bindings run on the OpMode instance.
    @OnPress(Key.A) static void isStatic() {}
    ^
BrokenBindings.java:9: error: [kernel] @OnToggle(X) BrokenBindings.noBoolean() must take exactly one boolean parameter (the new toggle state).
    @OnToggle(Key.X) void noBoolean() {}
    ^
BrokenBindings.java:10: error: [kernel] @OnPress(B) BrokenBindings.thirdGamepad() gamepad must be 1 or 2, got 3.
    @OnPress(value = Key.B, gamepad = 3) void thirdGamepad() {}
    ^
BrokenBindings.java:11: error: [kernel] @OnPress(Y) BrokenBindings.thresholdOnButton() threshold only applies to LEFT_TRIGGER and RIGHT_TRIGGER, not Y.
    @OnPress(value = Key.Y, threshold = 0.3) void thresholdOnButton() {}
    ^
BrokenBindings.java:13: error: [kernel] @OnPress(DPAD_UP) BrokenBindings.second() duplicates another @OnPress for gp1.DPAD_UP in this class; only one would be useful.
    @OnPress(Key.DPAD_UP) void second() {}
    ^
BrokenBindings.java:14: error: [kernel] @OnPress(START) BrokenBindings.hidden() must not be private; make it package-private (drop the 'private').
    @OnPress(Key.START) private void hidden() {}
    ^
6 errors
```

The same key bound in two *different* classes is fine and produces no warning, since separate OpModes bind the
same buttons all the time.

A second processor in the same module checks `@Watch` and `@Every`:

| Rule                                                  | Example message (abridged)                                                  |
|-------------------------------------------------------|-----------------------------------------------------------------------------|
| not `static`                                          | `@Watch(speed) Driver.speed must not be static; ...`                        |
| methods: not `private`, no parameters                 | `@Every(250) Driver.poll() must take no parameters.`                        |
| `@Watch` methods return something                     | `@Watch(ready) Driver.ready() must return the value to show.`              |
| `@Every` period > 0                                   | `@Every(0) Driver.poll() period must be > 0 ms, got 0.`                     |
| one `@Watch` key per class                            | `@Watch(lift) Driver.b() duplicates another @Watch key in this class; ...` |

The runtime binders repeat these checks (as `BindingPanic`), so builds that skip annotation
processing (OnBotJava, or a missing `annotationProcessor` line) still fail at `new Robot()` instead of
misbehaving mid-match.

The processor is tested in `KernelProcessor/src/test` (`./gradlew :KernelProcessor:test`).

## 2. Checked exceptions

### `OpModeStoppedException extends InterruptedException`

Thrown by `robot.waitUntil`, `robot.sleep`, `robot.await` and `robot.awaitStart` when stop is pressed mid-wait.
`runOpMode()` already declares `throws InterruptedException`, so a linear Auto needs no extra code and unwinds
straight out. If you catch it to clean up, **rethrow it**.

### `SubsystemUnavailableException`

Thrown by `kernel.get(Type.class)` when the active profile doesn't provide that subsystem. Being checked means
an interlocking driver can't compile until it decides what to do on a robot without the other mechanism. See
[Events and interlocks](05-events-and-interlocks.md#interlocks).

## 3. Runtime panics

All extend `KernelPanic extends RuntimeException`. When a panic (or any other `RuntimeException`) escapes
`robot.tick()`, the kernel:

1. stops motion and calls `stop()` on every subsystem (a subsystem whose `stop()` throws is skipped, not fatal);
2. drops pending events;
3. sets the Driver Station error to `KERNEL PANIC: <message>` (or `KERNEL PANIC (<ExceptionType>): <message>`
   for non-kernel exceptions);
4. rethrows, so the SDK ends the OpMode.

The SDK clears the error message when the next OpMode starts.

| Panic                        | When                                                                      | Fix                                                        |
|------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------|
| `DeviceNotFoundPanic`        | `kernel.device(...)` name/type not in the Robot Configuration              | message lists configured devices; fix the name or config   |
| `ProfileMisconfiguredPanic`  | `Profiles.ACTIVE` null; tuner config not pasted; `follower()` null; using a subsystem the profile lacks | follow the message                   |
| `NoActiveOpModePanic`        | `new Robot()` outside a running OpMode                                     | construct inside `runOpMode()`/`init()`                    |
| `WrongOpModeTypePanic`       | `waitUntil`/`sleep`/`await` from an iterative `OpMode`                     | poll in `loop()` or use `KernelOpMode`                     |
| `BindingPanic`               | invalid annotation at bind time; a key binding or event handler threw      | message names the binding/event; cause is attached        |
| `TickReentrancyPanic`        | `tick()` (or a blocking helper) called from inside a handler               | set a flag in the handler, act on it in the main loop      |

A `KernelPanic` thrown inside a handler passes through unwrapped, so you see the original panic, not a
`BindingPanic` around it.
