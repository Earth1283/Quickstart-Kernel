package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;

// Straight segments with the heading turning evenly from each pose to the next.
// Reach for Pedro's builders directly for curves or other heading interpolators.
public final class Route {
    private Route() {}

    public static Path line(Pose from, Pose to) {
        return Paths.line(from, to).linear(from, to);
    }

    public static Path[] through(Pose... waypoints) {
        if (waypoints.length < 2) throw new KernelPanic("Route.through() needs at least two poses, got " + waypoints.length + ".");
        Path[] legs = new Path[waypoints.length - 1];
        for (int i = 0; i < legs.length; i++) legs[i] = line(waypoints[i], waypoints[i + 1]);
        return legs;
    }
}
