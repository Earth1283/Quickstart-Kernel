package org.firstinspires.ftc.teamcode.opmodes;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.kernel.Alliance;
import org.firstinspires.ftc.teamcode.kernel.KernelOpMode;
import org.firstinspires.ftc.teamcode.kernel.PlayAs;
import org.firstinspires.ftc.teamcode.kernel.motion.Route;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

@Autonomous(name = "Example Auto", group = "Kernel")
@PlayAs(Alliance.BLUE)
public class ExampleAuto extends KernelOpMode {
    private Alliance side;
    private Pose start, scoring, pickup;

    @Override
    protected void onInit() {
        side = robot.alliance();
        start = side.pose(9, 60, 0);
        scoring = side.pose(30, 110, Math.toRadians(90));
        pickup = side.pose(40, 36, 0);

        robot.motion.setPose(start);
        robot.claw.close();
    }

    @Override
    protected void onStart() throws InterruptedException {
        robot.await(robot.motion.follow(Route.line(start, scoring)), robot.lift.goTo(Lift.Level.HIGH));
        robot.claw.open();
        robot.await(robot.motion.follow(Route.line(scoring, pickup)));
        robot.claw.close();
        robot.await(robot.motion.follow(Route.line(pickup, scoring)));
        robot.claw.open();

        robot.lift.goTo(Lift.Level.GROUND);
        robot.await(robot.motion.goTo(side.pose(60, 72, 0)));
        robot.await(robot.motion.turnBy(Math.toRadians(90)));
        robot.motion.aimTo(side.point(72, 72));
    }
}
