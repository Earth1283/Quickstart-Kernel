# THE KERNEL

> *"Userspace shall not touch `hardwareMap`."*
> — the first and only commandment

```
[    0.000000] Booting FTC Kernel (Quickstart-Kernel) on Control Hub
[    0.000412] profile: CompBot (Profiles.ACTIVE)
[    0.003117] pedro: Follower online (Pinpoint + Mecanum + Foresight)
[    0.004020] subsys: claw -> ServoClaw("claw")
[    0.004388] subsys: lift -> MotorLift("lift")
[    0.004391] lynx: seizing bulk caching (MANUAL). all hubs belong to me now.
[    0.004502] input: gp1, gp2 attached. scanning userspace for @OnPress...
[    0.004977] input: 11 bindings. none of them are static. good.
[    0.005001] pose: inherited (38.2, 61.9, 1.57) from Autonomous. you're welcome.
[    0.005003] lifecycle: listener registered. strongly referenced. the SDK can't GC me.
[    0.005010] init: userspace may now call robot.tick(). call it EVERY LOOP.
```

## What is this

For years, FTC OpModes have lived like animals. They grab motors straight out of `hardwareMap` with string
names typed by hand, they forget to clear bulk caches, they `Thread.sleep()` through the end of Auto, and
there's a `while` loop in there somewhere that forgot to update the follower.

**No more.** There is now a kernel. It owns:

- the hardware (every `DcMotor`, `Servo`, hub, and the battery voltage);
- the loop (`robot.tick()`: bulk read, input, motion, subsystems, events, in that order, every time);
- Pedro (follow, queue, aim, turn, hold);
- the gamepads (edge detection, toggles, long press, double tap, chords, triggers);
- the lifecycle (safe stop when the OpMode ends, pose carried from Auto into TeleOp);
- the consequences (see **KERNEL PANIC**, below).

You, userspace, get this:

```java
Robot robot = new Robot();
```

That's it. No arguments. The kernel finds your OpMode on its own. It knows. It always knows.

## The syscall table

| you want to…                | you call                                                                 |
|-----------------------------|--------------------------------------------------------------------------|
| drive                       | `robot.motion.drive(robot.gp1.leftStick, robot.gp1.rightStick)`          |
| creep                       | `robot.motion.setSpeedScale(0.35)`                                       |
| follow a path               | `robot.motion.follow(path)`                                              |
| queue paths                 | `robot.motion.queue(p1, p2, p3)`                                         |
| stare at a point menacingly | `robot.motion.aimTo(point)`                                              |
| go somewhere                | `robot.motion.goTo(pose)`                                                |
| turn in place               | `robot.motion.turnTo(heading)`, `robot.motion.turnBy(radians)`           |
| stay put                    | `robot.motion.hold()`                                                    |
| stop everything             | `robot.motion.cancel()`                                                  |
| wait (Auto)                 | `robot.await(robot.motion.goTo(pose))`, `robot.sleep(ms)`, `robot.waitUntil(() -> …)` |
| react to something          | `robot.on(Motion.PATH_DONE, path -> …)`                                  |
| use a mechanism             | `robot.claw.open()`, `robot.lift.goTo(Lift.Level.HIGH)`                  |
| say something               | `robot.telemetry.data("k", v)`, `@Watch("k") double k() { … }`           |
| do it in half a second      | `robot.after(500, () -> …)`, `@Every(250) void poll() { … }`             |
| be told it's endgame        | `robot.on(MatchClock.ENDGAME, () -> …)`                                  |
| tap someone on the shoulder | `robot.gp2.rumble(200)`, `robot.gp1.led(1, 0, 0)`                        |
| read a button               | `robot.gp1.a.justPressed()`                                              |
| bind a button               | `@OnPress(Key.A) void grab() { … }`                                      |
| play for red                | `@PlayAs(Alliance.RED)` on the OpMode class                             |

## A TeleOp, as the kernel intended

```java
@TeleOp(name = "Drive")
public class Drive extends KernelOpMode {
    @Override
    protected void onLoop() {
        robot.motion.drive(robot.gp1.leftStick, robot.gp1.rightStick);
    }

    @OnPress(Key.A)
    void grab() {
        robot.claw.toggle();
    }

    @WhileHeld(Key.RIGHT_BUMPER)
    void lockOn() {
        robot.motion.aimTo(robot.alliance().point(12, 132));
    }

    @OnRelease(Key.RIGHT_BUMPER)
    void unlock() {
        robot.motion.stopAiming();
    }
}
```

Notice what's missing: `hardwareMap`, `gamepad1.a && !lastA`, `follower.update()`, `LynxModule`,
`robot.tick()` (KernelOpMode calls it so you can't forget), the will to debug any of it.

## An Auto, as the kernel intended

```java
@Override
protected void onStart() throws InterruptedException {
    robot.await(robot.motion.follow(Route.line(start, basket)), robot.lift.goTo(Lift.Level.HIGH));
    robot.claw.open();
    robot.await(robot.motion.follow(Route.line(basket, sample)));
}
```

Press stop in the middle of that and `await()` throws `OpModeStoppedException`, which *is an*
`InterruptedException`, which `runOpMode()` already declares. Your Auto unwinds cleanly. Nobody
had to write `if (isStopRequested()) return;` fourteen times.

## KERNEL PANIC

When something is wrong, the kernel does not "log a warning and hope". It **panics**: it zeroes every
output, writes `KERNEL PANIC: <what happened and how to fix it>` to the Driver Station, and rethrows.
Every panic message tells you how to fix it. That's a rule, not a suggestion.

### Hall of fame

```
KERNEL PANIC: No Servo named "claw" (wanted by profile CompBot). Configured devices: arm, fl, fr, bl, br,
pinpoint. Fix the name in the profile, or add the device in the Driver Station's Configure Robot menu.
```
*Someone named it "Claw". Capital C. The kernel lists every configured device so you can see it from the
Driver Station.*

```
KERNEL PANIC: robot.tick() was called from inside a tick. A key binding or event handler probably called
tick(), waitUntil(), sleep() or await(). Handlers must not block; set a flag and handle it in your
main loop.
```
*Someone put `robot.sleep(500)` in an `@OnPress`. Handlers run inside the tick. The tick does not
recurse. The tick has boundaries.*

```
KERNEL PANIC: robot.lift.goTo() was called, but profile PracticeBot doesn't provide a Lift. Guard it with
robot.has(Lift.class) or add one to the profile.
```
*The practice bot doesn't have a lift. It boots anyway. It only panics when you actually try to lift.*

```
KERNEL PANIC: CompBot.foresightConfig is not set. Run the Foresight Tuner from the Tuning OpMode and paste
its output into profiles/CompBot.java.
```
*You skipped tuning. The kernel noticed.*

And some panics never make it to the robot, because **the compiler catches them**:

```
error: [kernel] @OnPress(START) Driver.hidden() must not be private; make it package-private (drop the 'private').
error: [kernel] @OnToggle(X) Driver.slow() must take exactly one boolean parameter (the new toggle state).
error: [kernel] @OnPress(B) Driver.b() gamepad must be 1 or 2, got 3.
```
*There is no gamepad 3. There was never a gamepad 3.*

## FAQ

**Can I just call `setPower` directly?**
No.

**But what if I really need to?**
Write a driver. It's twenty lines. [doc/kernel/07-writing-subsystems.md](../../../../../../../../../doc/kernel/07-writing-subsystems.md).

**Can I call `robot.tick()` twice in one loop?**
You can. It's a full extra cycle: bulk read, input, motion, everything. Edges will be consumed on the
first one. Just call it once.

**Can I call `robot.tick()` from a key binding?**
No, and it'll panic if you try.

**What happens if I stop calling `robot.motion.drive()`?**
The robot stops. Drive input is consumed every tick. This is a feature. A loop that stops talking to
the drivetrain should not keep driving it.

**Why is `OpModeStoppedException` checked?**
It's an `InterruptedException`, which your `runOpMode()` already throws, so it costs you nothing. And
the compiler stops you from accidentally swallowing a stop request in a `catch (Exception e)`.

**Why is `SubsystemUnavailableException` checked?**
Because if your claw interlocks with a lift, the compiler should make you decide what happens on the
robot that doesn't *have* a lift. (Usually: nothing. Catch it and move on.)

**Is it actually a kernel?**
It has a scheduler, a syscall table, interrupts (sort of), signals, IPC, and panics. Also no.

## Read the actual docs

The jokes stop here. [`doc/kernel/`](../../../../../../../../../doc/kernel/) has the real manual:

1. [Overview](../../../../../../../../../doc/kernel/01-overview.md): architecture, the tick pipeline, the rules
2. [Getting started](../../../../../../../../../doc/kernel/02-getting-started.md): first TeleOp and first Auto
3. [Motion](../../../../../../../../../doc/kernel/03-motion.md): drive, follow, queue, aim, turn, hold, alliances
4. [Input](../../../../../../../../../doc/kernel/04-input.md): annotations, the fluent API, gestures, timing
5. [Events and interlocks](../../../../../../../../../doc/kernel/05-events-and-interlocks.md)
6. [Profiles and porting](../../../../../../../../../doc/kernel/06-profiles-and-porting.md)
7. [Writing subsystems](../../../../../../../../../doc/kernel/07-writing-subsystems.md)
8. [Errors](../../../../../../../../../doc/kernel/08-errors.md): every exception and every compile-time check
9. [Lifecycle](../../../../../../../../../doc/kernel/09-lifecycle.md): pose handoff, safe stop, bulk caching
10. [Telemetry](../../../../../../../../../doc/kernel/10-telemetry.md): `robot.telemetry`, the status panel, `report()`
11. [Init tasks](../../../../../../../../../doc/kernel/11-init-tasks.md): boot checks, `awaitStart()`
12. [Timers and the match clock](../../../../../../../../../doc/kernel/12-timers-and-match.md)

```
[ 1337.000000] kernel: userspace exited with code 0. motors zeroed. goodnight.
```
