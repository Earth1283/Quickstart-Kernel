# 7. Writing subsystems

A mechanism is two pieces:

- an **interface** in `kernel/subsystems/`: what userspace can ask for, in the mechanism's own words;
- a **driver** in `kernel/drivers/`: how one particular piece of hardware does it.

The profile picks the driver, and userspace only ever sees the interface. That split is what lets a practice bot
with a different claw run the same OpModes.

## The contract

```java
public interface Subsystem {
    default void update() {}
    void stop();
}
```

**`update()`** is called once per tick, after input and motion, with fresh bulk-read data.
- Must not block (no `sleep`, no waiting on hardware).
- Should do the hardware writes. Methods userspace calls (`open()`, `goTo()`) should only record intent, and
  `update()` applies it. Then five calls to `open()` in one loop cost one servo write, and every write happens at
  a predictable point in the tick.
- Can emit events via `kernel.emit(...)`.

**`stop()`** puts outputs in a safe state. It's called when the OpMode stops and after any panic.
- Must be idempotent and must not throw. (If it throws anyway, the kernel ignores it and keeps stopping the other
  subsystems.)
- Motors → 0 power. Servos usually stay where they are (moving a servo during an emergency stop is rarely what
  you want).

## Worked example: a wrist

### 1. The interface

```java
package org.firstinspires.ftc.teamcode.kernel.subsystems;

public interface Wrist extends Subsystem {
    enum Angle { STOWED, SCORING, INTAKE }

    void moveTo(Angle angle);

    Angle angle();
}
```

Name methods for what the driver *wants*, not how the hardware does it (`moveTo(SCORING)`, not
`setPosition(0.62)`). Robot-specific numbers belong in the driver's config.

### 2. The driver

```java
package org.firstinspires.ftc.teamcode.kernel.drivers;

public final class ServoWrist implements Wrist {
    private final Servo servo;
    private final Map<Angle, Double> positions;
    private Angle angle = Angle.STOWED;
    private boolean dirty = true;

    public ServoWrist(Kernel kernel, String servoName, Map<Angle, Double> positions) {
        this.servo = kernel.device(Servo.class, servoName);
        this.positions = positions;
    }

    @Override
    public void moveTo(Angle angle) {
        dirty |= this.angle != angle;
        this.angle = angle;
    }

    @Override
    public Angle angle() {
        return angle;
    }

    @Override
    public void update() {
        if (!dirty) return;
        servo.setPosition(positions.get(angle));
        dirty = false;
    }

    @Override
    public void stop() {}
}
```

Always get hardware through `kernel.device(Type.class, "name")`, never `hardwareMap.get`. A wrong name then
becomes a `DeviceNotFoundPanic` that lists every configured device, instead of the SDK's bare exception.

### 3. Plug it in

```java
// RobotProfile
default Wrist wrist(Kernel kernel) {
    return null;
}

// CompBot
@Override
public Wrist wrist(Kernel kernel) {
    Map<Wrist.Angle, Double> positions = new EnumMap<>(Wrist.Angle.class);
    positions.put(Wrist.Angle.STOWED, 0.10);
    positions.put(Wrist.Angle.SCORING, 0.62);
    positions.put(Wrist.Angle.INTAKE, 0.95);
    return new ServoWrist(kernel, "wrist", positions);
}

// Robot
public final Wrist wrist;
...
wrist = install(Wrist.class, profile.wrist(kernel));
```

`install` registers it for ticking, for `kernel.get(Wrist.class)` interlocks, and for safe stop. Or, when the
profile returns `null`, it substitutes the panicking stand-in. Subsystems update in install order.

### 4. Use it

```java
@OnPress(value = Key.DPAD_LEFT, gamepad = 2)
void wristToScore() {
    robot.wrist.moveTo(Wrist.Angle.SCORING);
}
```

## Kernel services available to drivers

| Call                                  | Gives you                                                        |
|---------------------------------------|------------------------------------------------------------------|
| `kernel.device(Type.class, "name")`   | a hardware device, or a `DeviceNotFoundPanic` with the device list |
| `kernel.get(Other.class)`             | another subsystem (checked `SubsystemUnavailableException`)      |
| `kernel.emit(EVENT, payload)`         | queue an event for end-of-tick delivery                          |
| `kernel.batteryVoltage()`             | lowest hub voltage, cached for 250 ms (for voltage compensation) |
| `kernel.nanoTime()`                   | the kernel's clock (same one input timing uses)                  |
| `kernel.profileName()`                | for your own error messages                                      |

Voltage compensation, for a motor you want to behave the same at 12.4 V and 13.8 V:

```java
motor.setPower(feedforward * 12.0 / kernel.batteryVoltage());
```
