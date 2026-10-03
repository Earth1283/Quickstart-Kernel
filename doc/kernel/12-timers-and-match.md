# 12. Timers and the match clock

## Timers

```java
Subscription later = robot.after(500, () -> robot.claw.close());   // once
Subscription beat  = robot.every(250, () -> robot.gp1.rumble(40)); // repeating
later.cancel();
```

## `@Every`

```java
@Every(250) void pollCamera() { ... }                       // every 250 ms, from start
@Every(value = 1000, duringInit = true) void checkSensors() { ... }  // also during init
```

An annotated method on the OpMode (or anything passed to `robot.bind(obj)`) becomes an `every(...)` timer. By
default it starts at start, like key bindings, so nothing moves during init; the first call is one period after
start. `duringInit = true` starts it as soon as the `Robot` is built. The method must take no parameters and
not be `static` or `private`, and the period must be > 0. These are checked at compile time and again at
runtime. There's no `@After`: "after what?" is clearer in code.

## How timers run

Timers run inside `tick()` on the kernel clock, so they never use a thread, never need `Thread.sleep`, and are
dropped on stop. A repeating timer that falls behind (a slow loop) fires once and re-aims at the next slot rather
than firing a burst. A callback that throws becomes a `BindingPanic`. Like key bindings and event handlers, callbacks
must not block or call `tick()`.

## The match clock

It starts when the OpMode is started. TeleOp is 120 s, Auto is 30 s.

```java
robot.on(MatchClock.ENDGAME, () -> robot.gp1.rumbleBlips(3));  // TeleOp, 30 s left
robot.on(MatchClock.ENDING,  () -> robot.lift.goTo(Lift.Level.GROUND)); // 10 s left in TeleOp, 5 s left in Auto
double left = robot.matchSecondsRemaining();                   // the full length before start
```

Each event fires once. Auto has no `ENDGAME`. The remaining time also shows in the status header.
