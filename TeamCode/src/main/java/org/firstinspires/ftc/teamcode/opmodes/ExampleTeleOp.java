package org.firstinspires.ftc.teamcode.opmodes;

import com.pedropathing.math.Vector2D;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.kernel.Robot;
import org.firstinspires.ftc.teamcode.kernel.input.Key;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnDoubleTap;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnLongPress;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnPress;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnRelease;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.OnToggle;
import org.firstinspires.ftc.teamcode.kernel.input.annotations.WhileHeld;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

@TeleOp(name = "Example TeleOp", group = "Kernel")
public class ExampleTeleOp extends LinearOpMode {
    private static final double SLOW_MODE_SCALE = 0.35;

    private Robot robot;
    private Vector2D goal;
    private double speed = 1.0;

    @Override
    public void runOpMode() throws InterruptedException {
        robot = new Robot();
        goal = robot.alliance().point(12, 132);
        robot.on(Claw.OPEN_REFUSED, () -> gamepad2.rumble(200));

        waitForStart();
        while (opModeIsActive()) {
            robot.motion.drive(
                    robot.gp1.leftStick.up() * speed,
                    robot.gp1.leftStick.left() * speed,
                    robot.gp1.rightStick.left() * speed);
            robot.tick();

            telemetry.addData("pose", robot.motion.pose());
            telemetry.addData("mode", robot.motion.mode());
            telemetry.addData("aiming", robot.motion.isAiming());
            telemetry.addData("lift", robot.lift.target());
            telemetry.update();
        }
    }

    @OnPress(Key.A)
    void toggleClaw() {
        robot.claw.toggle();
    }

    @WhileHeld(Key.RIGHT_BUMPER)
    void aimAtGoal() {
        robot.motion.aimTo(goal);
    }

    @OnRelease(Key.RIGHT_BUMPER)
    void stopAiming() {
        robot.motion.stopAiming();
    }

    @OnToggle(Key.LEFT_STICK_BUTTON)
    void slowMode(boolean on) {
        speed = on ? SLOW_MODE_SCALE : 1.0;
    }

    @OnLongPress(value = Key.BACK, ms = 600)
    void resetDriverForward() {
        robot.motion.resetDriverForward();
    }

    @OnDoubleTap(Key.B)
    void cancelAutomation() {
        robot.motion.cancel();
    }

    @OnPress(value = Key.DPAD_UP, gamepad = 2)
    void liftHigh() {
        robot.lift.goTo(Lift.Level.HIGH);
    }

    @OnPress(value = Key.DPAD_RIGHT, gamepad = 2)
    void liftLow() {
        robot.lift.goTo(Lift.Level.LOW);
    }

    @OnPress(value = Key.DPAD_DOWN, gamepad = 2)
    void liftGround() {
        robot.lift.goTo(Lift.Level.GROUND);
    }

    @OnPress(value = Key.RIGHT_TRIGGER, gamepad = 2, threshold = 0.8)
    void closeClawFirmly() {
        robot.claw.close();
    }

    @OnPress(value = Key.Y, gamepad = 2, with = Key.LEFT_BUMPER)
    void turnToFaceGoalExactly() {
        Vector2D here = robot.motion.pose().toVector2D();
        robot.motion.turnTo(goal.minus(here).theta());
    }
}
