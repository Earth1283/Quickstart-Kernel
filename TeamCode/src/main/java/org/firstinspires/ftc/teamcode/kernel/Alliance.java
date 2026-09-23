package org.firstinspires.ftc.teamcode.kernel;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;

// Userspace writes blue coordinates; these helpers return the equivalent on this alliance's side.
// The mirror assumes a field symmetric across its vertical centerline. Change the three
// mirror methods together if the season's field is point-symmetric.
public enum Alliance {
    BLUE, RED;

    public static final double FIELD_SIZE_INCHES = 144.0;

    public Pose pose(double x, double y, double heading) {
        return pose(new Pose(x, y, heading));
    }

    public Pose pose(Pose blue) {
        return this == BLUE ? blue : mirror(blue);
    }

    public Vector2D point(double x, double y) {
        Vector2D blue = Vector2D.cartesian(x, y);
        return this == BLUE ? blue : mirror(blue);
    }

    public double heading(double blue) {
        return this == BLUE ? blue : mirrorHeading(blue);
    }

    public static Pose mirror(Pose p) {
        return new Pose(FIELD_SIZE_INCHES - p.x(), p.y(), mirrorHeading(p.heading()));
    }

    public static Vector2D mirror(Vector2D v) {
        return Vector2D.cartesian(FIELD_SIZE_INCHES - v.x(), v.y());
    }

    public static double mirrorHeading(double heading) {
        return Angles.wrap(Math.PI - heading);
    }
}
