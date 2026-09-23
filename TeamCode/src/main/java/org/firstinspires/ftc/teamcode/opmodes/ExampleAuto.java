package org.firstinspires.ftc.teamcode.opmodes;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.kernel.Alliance;
import org.firstinspires.ftc.teamcode.kernel.Robot;
import org.firstinspires.ftc.teamcode.kernel.motion.Motion;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

@Autonomous(name = "Example Auto", group = "Kernel")
public class ExampleAuto extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        Robot robot = new Robot();
        robot.alliance(Alliance.BLUE);
        Alliance side = robot.alliance();

        Pose start = side.pose(9, 60, 0);
        Pose scoring = side.pose(30, 110, Math.toRadians(90));
        Pose pickup = side.pose(40, 36, 0);
        Path toScoring = line(start, scoring).linear(start, scoring);
        Path toPickup = line(scoring, pickup).linear(scoring, pickup);
        Path backToScoring = line(pickup, scoring).linear(pickup, scoring);

        robot.motion.setPose(start);
        robot.claw.close();
        robot.on(Motion.PATH_DONE, path -> {
            if (path == toScoring || path == backToScoring) robot.claw.open();
            if (path == toPickup) robot.claw.close();
        });

        waitForStart();

        robot.lift.goTo(Lift.Level.HIGH);
        robot.motion.queue(toScoring, toPickup, backToScoring);
        robot.waitForMotion();

        robot.lift.goTo(Lift.Level.GROUND);
        robot.motion.aimTo(side.point(72, 72));
        robot.sleep(1000);
    }
}
