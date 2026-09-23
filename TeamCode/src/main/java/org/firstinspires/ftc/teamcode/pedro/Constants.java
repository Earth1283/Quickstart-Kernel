package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.kernel.profiles.Profiles;

public class Constants {
    public static Follower create(HardwareMap h) {
        return Profiles.ACTIVE.follower(h);
    }
}
