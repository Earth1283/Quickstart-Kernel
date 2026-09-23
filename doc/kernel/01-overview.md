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
    Kernel.java             context handed to drivers (devices, other subsystems, events, voltage)
    Subsystem.java          update()/stop() contract every mechanism implements
    Alliance.java           BLUE/RED + coordinate mirroring
    PoseStore.java          Auto -> TeleOp pose handoff
    Angles.java             wrap/error/toward helpers
    events/                 Event<T>, EventBus, Subscription
    input/                  Pad, Button, Trigger, Stick, Key, Input, annotations/
    motion/                 Motion (userspace API), PedroMotion (driver), MotionTuning
    subsystems/             userspace-facing mechanism interfaces (Claw, Lift)
    drivers/                hardware implementations (ServoClaw, MotorLift)
    profiles/               RobotProfile, CompBot, Profiles.ACTIVE
    errors/                 KernelPanic and friends, checked exceptions
  opmodes/                  ExampleTeleOp, ExampleAuto
KernelProcessor/            annotation processor: compile-time checks for key bindings
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
   ├─ 2. input.update() ........... snapshot both gamepads, compute edges, fire key bindings
   ├─ 3. motion.update() .......... drive/aim math → follower.update() → advance the path queue
   ├─ 4. subsystem.update() ....... each mechanism, in profile install order (claw, then lift)
   └─ 5. events.dispatch() ........ deliver everything emitted during 2–4, in emit order
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
