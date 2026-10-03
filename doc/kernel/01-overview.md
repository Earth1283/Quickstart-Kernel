# 1. Overview

The kernel is a layer between your OpModes ("userspace") and the robot's hardware. Userspace creates one
`Robot`, says what it wants (drive this way, follow this path, open the claw), and calls `robot.tick()` once
per loop. The kernel turns that intent into hardware writes.

It exists for two reasons:

- **Clean OpModes.** TeleOp and Auto read like intent. No `hardwareMap` strings, motor objects, edge-detection
  booleans, or loop bookkeeping.
- **Portability.** Everything robot-specific (device names, directions, tuning, which mechanisms exist) lives in
  one *profile* class. The same OpModes run on a practice bot and a competition bot by changing one line.

## Layout

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
  kernel/
    Robot.java              userspace entry point
    PlayAs.java             @PlayAs(Alliance.RED): pick the alliance per OpMode class
    KernelOpMode.java       LinearOpMode base that owns robot, awaitStart and the tick loop
    Awaitable.java          isSettled(), what robot.await(...) waits on
    Kernel.java             context handed to drivers (devices, other subsystems, events, voltage)
    Subsystem.java          update()/stop() contract every mechanism implements
    Alliance.java           BLUE/RED + coordinate mirroring
    PoseStore.java          Auto -> TeleOp pose and alliance handoff
    Angles.java             wrap/error/toward helpers
    events/                 Event<T>, EventBus, Subscription
    input/                  Pad, Button, Trigger, Stick, Key, Input, annotations/
    motion/                 Motion (userspace API), PedroMotion (driver), MotionTuning, Route (straight paths)
    subsystems/             userspace-facing mechanism interfaces (Claw, Lift, Intake, Flywheel, Turret, Rangefinder)
    drivers/                hardware implementations (ServoClaw, MotorLift, MotorIntake, ...)
    telemetry/              KernelTelemetry (robot.telemetry), Report
    init/                   InitTask, InitResult, InitLog
    time/                   Scheduler (after/every), MatchClock, LoopTimer
    profiles/               RobotProfile, CompBot, Profiles.ACTIVE
    errors/                 KernelPanic and friends, checked exceptions
  opmodes/                  ExampleTeleOp, ExampleAuto, ExampleRedAuto (ExampleAuto under @PlayAs(RED))
KernelProcessor/            annotation processors: compile-time checks for key bindings, @Watch and @Every
```

## The boundary

| Userspace (OpModes) may                         | Userspace may not                                    |
|-------------------------------------------------|------------------------------------------------------|
| use `Robot` and its fields                      | touch `hardwareMap`                                  |
| use Pedro types (`Pose`, `Path`, `Vector2D`)    | construct drivers or call `Kernel`                   |
| use subsystem interfaces (`Claw`, `Lift`)       | call `follower.update()` or any hardware API         |
| subscribe to events, bind keys                  | block inside a key binding or event handler          |

Pedro's types are re-exported rather than wrapped. Pedro is already hardware-agnostic, so wrapping it would add
code without buying any portability.

## The tick pipeline

Every `robot.tick()` runs these stages, in this order, on the calling thread:

```
 robot.tick()
   │
   ├─ 1. clear bulk caches ........ every hub's MANUAL cache is invalidated; the next read is fresh
   ├─ 2. input .................... snapshot both gamepads, compute edges; fire key bindings once started
   ├─ 3. motion.update() .......... drive/aim math → follower.update() → advance the path queue
   ├─ 4. subsystem.update() ....... each mechanism, in install order (claw, lift, intake, flywheel, turret, ...)
   ├─ 5. timers + match clock ..... due robot.after()/every() callbacks run; ENDGAME/ENDING are emitted
   ├─ 6. events.dispatch() ........ deliver everything emitted during 2–5, in emit order
   └─ 7. telemetry ................ every ~100 ms, publish the status panel plus your data()/line()/watch()
```

Consequences worth knowing:

- Key bindings (stage 2) run **before** motion, so `aimTo()` from a binding takes effect the same tick.
- `robot.motion.drive(...)` called in your loop *before* `tick()` is applied in stage 3 of that tick.
- Event handlers (stage 5) see the robot after every subsystem has updated.
- Events emitted *by* a handler are delivered on the next tick.
- If anything in the pipeline throws, the kernel stops every output, reports `KERNEL PANIC` on the Driver
  Station, and rethrows (see [Errors](08-errors.md)).

## Threading

Everything runs on the thread that calls `tick()`: your OpMode's thread. There are no background threads. The
only thing that runs elsewhere is the stop handler (the SDK's event-loop thread calls it after your OpMode has
stopped). It zeroes outputs and saves the pose.

## One Robot per OpMode

Create exactly one `Robot` per OpMode run, inside `runOpMode()` (or `init()` for iterative OpModes). It binds
to that OpMode's hardware, gamepads and lifecycle. A `Robot` stored in a static field and reused in a later
OpMode is bound to a dead OpMode.
