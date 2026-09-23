package org.firstinspires.ftc.teamcode.kernel;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.teamcode.kernel.errors.DeviceNotFoundPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.SubsystemUnavailableException;
import org.firstinspires.ftc.teamcode.kernel.events.Event;
import org.firstinspires.ftc.teamcode.kernel.events.EventBus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class Kernel {
    private static final long VOLTAGE_REFRESH_NANOS = TimeUnit.MILLISECONDS.toNanos(250);

    private final HardwareMap hardwareMap;
    private final String profileName;
    private final EventBus events;
    private final LongSupplier nanoClock;
    private final Map<Class<?>, Subsystem> subsystems = new LinkedHashMap<>();
    private final List<LynxModule> hubs = new ArrayList<>();

    private double batteryVoltage = Double.NaN;
    private long batteryVoltageReadAt;

    Kernel(HardwareMap hardwareMap, String profileName, EventBus events, LongSupplier nanoClock) {
        this.hardwareMap = hardwareMap;
        this.profileName = profileName;
        this.events = events;
        this.nanoClock = nanoClock;
    }

    public <T extends HardwareDevice> T device(Class<T> type, String name) {
        T device = hardwareMap.tryGet(type, name);
        if (device == null) throw new DeviceNotFoundPanic(name, type, profileName, configuredDeviceNames());
        return device;
    }

    private TreeSet<String> configuredDeviceNames() {
        TreeSet<String> names = new TreeSet<>();
        for (HardwareDevice device : hardwareMap) names.addAll(hardwareMap.getNamesOf(device));
        return names;
    }

    public <T extends Subsystem> T get(Class<T> type) throws SubsystemUnavailableException {
        Subsystem subsystem = subsystems.get(type);
        if (subsystem == null) throw new SubsystemUnavailableException(type, profileName);
        return type.cast(subsystem);
    }

    public <T> void emit(Event<T> event, T payload) {
        events.emit(event, payload);
    }

    public void emit(Event<Void> event) {
        events.emit(event);
    }

    public long nanoTime() {
        return nanoClock.getAsLong();
    }

    // Not part of the bulk read, and each read is a separate hub transaction; cache it.
    public double batteryVoltage() {
        long now = nanoClock.getAsLong();
        if (Double.isNaN(batteryVoltage) || now - batteryVoltageReadAt >= VOLTAGE_REFRESH_NANOS) {
            batteryVoltage = readLowestBatteryVoltage();
            batteryVoltageReadAt = now;
        }
        return batteryVoltage;
    }

    private double readLowestBatteryVoltage() {
        double lowest = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double voltage = sensor.getVoltage();
            if (voltage > 0) lowest = Math.min(lowest, voltage);
        }
        return lowest == Double.POSITIVE_INFINITY ? 12.0 : lowest;
    }

    public String profileName() {
        return profileName;
    }

    <T extends Subsystem> void register(Class<T> type, T subsystem) {
        subsystems.put(type, subsystem);
    }

    Iterable<Subsystem> subsystems() {
        return subsystems.values();
    }

    // Must run after every driver is constructed, in case one of them changed the mode.
    void takeOverBulkCaching() {
        hubs.clear();
        hubs.addAll(hardwareMap.getAll(LynxModule.class));
        for (LynxModule hub : hubs) hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
    }

    void clearBulkCaches() {
        for (LynxModule hub : hubs) hub.clearBulkCache();
    }
}
