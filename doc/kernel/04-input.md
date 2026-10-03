# 4. Input

The kernel owns both gamepads. Once per tick (stage 2 of the [pipeline](01-overview.md#the-tick-pipeline)) it
snapshots every button, computes edges, and fires bindings. Everything in one tick sees the same snapshot, so
`justPressed()` means the same thing everywhere in that tick.

There are two front ends over the same engine: **annotations** and a **fluent API**. Mix them freely.

## Annotations

Put them on methods of your OpMode. `new Robot()` binds them automatically. To bind another object (a helper
class that owns some controls), call `robot.bind(obj)`. That also picks up its `@Watch` and `@Every` members
([Telemetry](10-telemetry.md), [Timers](12-timers-and-match.md)). `robot.input.bind(obj)` binds keys only.

| Annotation       | Fires                                                         | Method signature        |
|------------------|---------------------------------------------------------------|-------------------------|
| `@OnPress`       | once, on the tick the control goes down                       | `void m()`              |
| `@OnRelease`     | once, on the tick it comes up                                 | `void m()`              |
| `@WhileHeld`     | every tick it's down, including the press tick                | `void m()`              |
| `@OnToggle`      | on each press, with the new state (first press → `true`)      | `void m(boolean on)`    |
| `@OnLongPress`   | once per hold, after it's been down `ms` (default 500)        | `void m()`              |
| `@OnDoubleTap`   | on the second press within `windowMs` (default 300)           | `void m()`              |

Common attributes:

| Attribute   | Default | Meaning                                                                          |
|-------------|---------|----------------------------------------------------------------------------------|
| `value`     | —       | the `Key`                                                                        |
| `gamepad`   | `1`     | `1` or `2`                                                                       |
| `with`      | `{}`    | layer modifiers: `with = Key.LEFT_BUMPER` or `with = {…}` (see [layers](#chords-and-layers)) |
| `threshold` | `0.5`   | for `LEFT_TRIGGER`/`RIGHT_TRIGGER` only: how far the trigger counts as "down"    |

```java
@OnPress(Key.A)                                       void toggleClaw() { robot.claw.toggle(); }
@OnPress(value = Key.DPAD_UP, gamepad = 2)            void liftHigh() { robot.lift.goTo(Lift.Level.HIGH); }
@OnPress(value = Key.Y, gamepad = 2, with = Key.LEFT_BUMPER) void faceGoal() { ... }
@WhileHeld(Key.RIGHT_BUMPER)                          void aim() { robot.motion.aimTo(goal); }
@OnRelease(Key.RIGHT_BUMPER)                          void stopAim() { robot.motion.stopAiming(); }
@OnToggle(Key.LEFT_STICK_BUTTON)                      void slowMode(boolean on) { robot.motion.setSpeedScale(on ? 0.35 : 1); }
@OnLongPress(value = Key.BACK, ms = 600)              void rezero() { robot.motion.resetDriverForward(); }
@OnDoubleTap(Key.B)                                   void abort() { robot.motion.cancel(); }
@OnPress(value = Key.RIGHT_TRIGGER, threshold = 0.8)  void squeeze() { robot.claw.close(); }
```

Rules (checked at compile time by `KernelProcessor`, and again at runtime by the binder):

- not `static`, not `private` (package-private, protected or public all work);
- no parameters, except `@OnToggle`, which takes exactly one `boolean`;
- `gamepad` is 1 or 2; `threshold` is only allowed on triggers and must be in (0, 1];
- `ms`/`windowMs` > 0; a key can't be its own modifier;
- one binding per annotation kind per control per class (two `@OnPress(Key.A)` in one class is an error).

Inherited annotated methods are bound too. Superclasses are scanned up to the SDK's own classes.

`Key` uses Xbox names. PlayStation equivalents: `CROSS = A`, `CIRCLE = B`, `SQUARE = X`, `TRIANGLE = Y`,
`SHARE = BACK`, `OPTIONS = START`, `PS = GUIDE`.

## Fluent API

Same engine, no reflection, and lambdas can capture locals. Set bindings up **once** (before the loop). Every
call adds another binding.

```java
robot.gp1.a.onPress(() -> robot.claw.toggle());
robot.gp2.dpadUp.onPress(() -> robot.lift.goTo(Lift.Level.HIGH));
robot.gp1.rightBumper.whileHeld(() -> robot.motion.aimTo(goal)).onRelease(robot.motion::stopAiming);
robot.gp1.x.onToggle(on -> telemetry.addLine(on ? "slow" : "fast"));
robot.gp1.y.onLongPress(600, robot.motion::resetDriverForward);
robot.gp1.b.onDoubleTap(robot.motion::cancel);
robot.gp1.b.onDoubleTap(250, robot.motion::cancel);          // custom window
robot.gp1.leftBumper.and(robot.gp1.a).onPress(...);          // chord
robot.gp2.rightTrigger.asButton(0.8).onPress(...);           // trigger as button
```

Every `on*` returns the same `Button`, so you can chain.

## Polling

For logic that lives in your loop:

```java
if (robot.gp1.b.justPressed()) ...
if (robot.gp1.leftBumper.isDown()) ...
if (robot.gp1.a.justReleased()) ...
long ms = robot.gp1.y.heldMillis();

double drive = robot.gp1.leftStick.up();      // also down(), left(), right()
double trigger = robot.gp1.rightTrigger.value();
```

Polled values reflect the snapshot from the **most recent** `tick()`. Poll after `tick()` (or at the top of the
next loop), not before the first one.

`robot.gp1.button(Key.A)` gives you the `Button` for a `Key`, which is handy when the key is a variable.

## Sticks

- `up()` is positive when pushed away from you, and `left()` is positive to the left. The SDK's upside-down y is
  already fixed.
- A **radial** deadband (profile `stickDeadband()`, default 0.05) is applied to the stick as a whole, and the
  output is rescaled so it still starts at 0 and reaches ±1.
- A response curve (profile `stickCurve()`, default 1 = linear) is applied to every stick. Above 1 it softens
  small deflections for fine control and still reaches ±1 at full push.
- `curve(exponent)` returns a copy of a stick with a different curve, for one-off overrides:
  `robot.gp1.leftStick.curve(3).up()`. It builds a new object each call, so keep it in a field if you call it
  every loop.

## Chords and layers

```java
robot.gp1.chord(Key.LEFT_BUMPER, Key.A).onPress(...);        // down only while every key is down

Layer shift = robot.gp2.layer(Key.LEFT_BUMPER);              // a modifier key
shift.button(Key.A).onPress(() -> robot.claw.open());        // LB+A
shift.isActive();                                            // is LB held right now?
robot.gp2.layer(Key.LEFT_BUMPER, Key.RIGHT_BUMPER);          // several modifiers, held together
```

```java
@OnPress(value = Key.A, gamepad = 2, with = Key.LEFT_BUMPER)  void open() { ... }   // the same LB+A layer
```

A layer button is exclusive, and the most specific layer wins: while `LB` is held, the plain `A` reports as up,
so `A` and `LB+A` never both fire. While `LB` and `RB` are both held, `LB+RB+A` fires and `LB+A`, `RB+A` and `A`
stay quiet. The modifiers themselves still report normally, so a binding on `LB` alone still fires.

Chords are *not* exclusive: `chord(LB, A)` or `leftBumper.and(a)` fires alongside the plain `A`. Use a layer
unless you specifically want both.

## Feedback

```java
robot.gp2.rumble(200);               // both motors, milliseconds
robot.gp2.rumble(0.2, 1.0, 400);     // left, right, milliseconds
robot.gp2.rumbleBlips(3);
robot.gp1.led(1, 0, 0);              // DualShock 4 / DualSense light bar, until changed
robot.gp1.isConnected();
```

`robot.alliance(...)` already paints both light bars blue or red. Output goes through the same `PadSource` as
input, so tests can record it.

## Init and bindings

Before the OpMode is started, `tick()` still samples the gamepads (so `robot.gp1.x.justPressed()` works for picking an
alliance in init) but **key bindings stay disarmed** until start. A stray press during init can't move a mechanism.

## Timing semantics

- Edges are per tick. If a button goes down and up between two ticks, the kernel never sees it. Keep loops fast.
- `@OnLongPress` measures from the tick the press was seen, and fires on the first tick at or past `ms`.
- `@OnDoubleTap` fires on the second press if it lands within `windowMs` of the first, then resets. A triple tap
  counts as one double tap plus a first tap.
- Bindings fire in the order they were registered. A chord (`LB+A`) doesn't suppress the plain `A` binding: both
  fire. A layer (`layer(LB)` or `with = LB`) does.

## Errors

A handler that throws becomes a `BindingPanic` naming the binding, e.g.
`@OnPress(A) MyTeleOp.toggleClaw() threw java.lang.IllegalStateException: ...`. Handlers run inside `tick()`,
so they must not call `tick()`, `sleep()`, `waitUntil()` or `await()`. That's a `TickReentrancyPanic`.
