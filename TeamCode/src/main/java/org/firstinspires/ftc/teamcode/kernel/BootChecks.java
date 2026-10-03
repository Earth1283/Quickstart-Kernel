package org.firstinspires.ftc.teamcode.kernel;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.kernel.init.InitResult;
import org.firstinspires.ftc.teamcode.kernel.init.InitTask;
import org.firstinspires.ftc.teamcode.kernel.motion.Motion;
import org.firstinspires.ftc.teamcode.kernel.profiles.RobotProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BootChecks {
    private BootChecks() {}

    static List<InitTask> forRobot(Kernel kernel, Motion motion, RobotProfile profile) {
        List<InitTask> tasks = new ArrayList<>();
        tasks.add(InitTask.of("Hubs", () -> hubs(kernel)));
        tasks.add(InitTask.of("Battery", () -> battery(kernel, profile.lowBatteryVolts())));
        tasks.add(InitTask.critical("Localizer", () -> localizer(motion)));
        for (Map.Entry<Class<?>, Subsystem> installed : kernel.installed().entrySet()) {
            tasks.add(InitTask.of(installed.getKey().getSimpleName(), installed.getValue()::init));
        }
        tasks.addAll(profile.initTasks(kernel));
        return tasks;
    }

    private static InitResult hubs(Kernel kernel) {
        int count = kernel.hubCount();
        if (count == 0) return InitResult.warn("no hubs found, bulk caching is not active");
        return InitResult.ok(count + (count == 1 ? " hub" : " hubs"));
    }

    private static InitResult battery(Kernel kernel, double lowVolts) {
        double volts = kernel.batteryVoltage();
        String reading = String.format(Locale.US, "%.1f V", volts);
        if (volts < lowVolts) return InitResult.warn(reading + ", below " + String.format(Locale.US, "%.1f V", lowVolts));
        return InitResult.ok(reading);
    }

    private static InitResult localizer(Motion motion) {
        Pose pose = motion.pose();
        boolean finite = !Double.isNaN(pose.x()) && !Double.isNaN(pose.y()) && !Double.isNaN(pose.heading());
        return finite ? InitResult.ok("pose is live") : InitResult.fail("localizer reports NaN; check the odometry pod or Pinpoint wiring");
    }
}
