package org.firstinspires.ftc.teamcode.kernel;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.kernel.errors.OpModeStoppedException;

// Owns the loop so userspace can't forget robot.tick(). Inside these hooks, use robot.sleep()
// rather than sleep(): LinearOpMode.sleep() is final, and it stops the kernel ticking.
public abstract class KernelOpMode extends LinearOpMode {
    protected Robot robot;

    protected void onInit() throws InterruptedException {}

    protected void onStart() throws InterruptedException {}

    protected void onLoop() throws InterruptedException {}

    @Override
    public final void runOpMode() throws InterruptedException {
        robot = new Robot(this);
        onInit();
        robot.awaitStart();
        onStart();
        while (opModeIsActive()) {
            onLoop();
            robot.tick();
        }
    }

    // The SDK version stops ticking, which freezes the init screen and gamepad sampling.
    @Override
    public void waitForStart() {
        try {
            robot.awaitStart();
        } catch (OpModeStoppedException stopped) {
            Thread.currentThread().interrupt();
        }
    }
}
