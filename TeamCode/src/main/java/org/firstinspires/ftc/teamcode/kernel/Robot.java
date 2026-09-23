package org.firstinspires.ftc.teamcode.kernel;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.OpModeStoppedException;
import org.firstinspires.ftc.teamcode.kernel.errors.ProfileMisconfiguredPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.SubsystemUnavailableException;
import org.firstinspires.ftc.teamcode.kernel.errors.TickReentrancyPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.WrongOpModeTypePanic;
import org.firstinspires.ftc.teamcode.kernel.events.Event;
import org.firstinspires.ftc.teamcode.kernel.events.EventBus;
import org.firstinspires.ftc.teamcode.kernel.events.Subscription;
import org.firstinspires.ftc.teamcode.kernel.input.Input;
import org.firstinspires.ftc.teamcode.kernel.input.Pad;
import org.firstinspires.ftc.teamcode.kernel.input.SdkPadSource;
import org.firstinspires.ftc.teamcode.kernel.motion.Motion;
import org.firstinspires.ftc.teamcode.kernel.motion.PedroMotion;
import org.firstinspires.ftc.teamcode.kernel.profiles.Profiles;
import org.firstinspires.ftc.teamcode.kernel.profiles.RobotProfile;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Claw;
import org.firstinspires.ftc.teamcode.kernel.subsystems.Lift;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class Robot {
    public final Motion motion;
    public final Claw claw;
    public final Lift lift;

    public final Input input;
    public final Pad gp1;
    public final Pad gp2;

    private final OpMode opMode;
    private final Kernel kernel;
    private final EventBus events = new EventBus();
    private final PedroMotion pedroMotion;
    private final LongSupplier nanoClock = System::nanoTime;
    // The SDK holds lifecycle listeners weakly; this field keeps ours alive.
    private final OpModeManagerNotifier.Notifications lifecycle;

    private Alliance alliance = Alliance.BLUE;
    private boolean ticking;
    private boolean stopped;

    public Robot() {
        this(ActiveOpMode.require());
    }

    public Robot(OpMode opMode) {
        this.opMode = opMode;
        RobotProfile profile = Profiles.ACTIVE;
        if (profile == null) throw new ProfileMisconfiguredPanic("Profiles.ACTIVE is null. Point it at your robot's profile.");

        kernel = new Kernel(opMode.hardwareMap, profile.name(), events, nanoClock);

        Follower follower = profile.follower(opMode.hardwareMap);
        if (follower == null) throw new ProfileMisconfiguredPanic(profile.name() + ".follower() returned null.");
        pedroMotion = new PedroMotion(follower, events, profile.motionTuning(), nanoClock);
        motion = pedroMotion;

        claw = install(Claw.class, profile.claw(kernel));
        lift = install(Lift.class, profile.lift(kernel));

        input = new Input(new SdkPadSource(() -> opMode.gamepad1), new SdkPadSource(() -> opMode.gamepad2),
                profile.stickDeadband(), nanoClock);
        gp1 = input.gp1;
        gp2 = input.gp2;

        kernel.takeOverBulkCaching();
        alliance(Alliance.BLUE);
        if (!isAutonomous(opMode)) resumePoseFromAutonomous();

        lifecycle = new ShutdownOnStop();
        ActiveOpMode.manager().registerListener(lifecycle);

        input.bind(opMode);
    }

    private <T extends Subsystem> T install(Class<T> type, T subsystem) {
        if (subsystem == null) return Missing.subsystem(type, kernel.profileName());
        kernel.register(type, subsystem);
        return subsystem;
    }

    private static boolean isAutonomous(OpMode opMode) {
        return opMode.getClass().isAnnotationPresent(Autonomous.class);
    }

    private void resumePoseFromAutonomous() {
        Pose pose = PoseStore.take();
        if (pose != null) motion.setPose(pose);
    }

    public void tick() {
        if (ticking) throw new TickReentrancyPanic();
        if (stopped) return;
        ticking = true;
        try {
            kernel.clearBulkCaches();
            input.update();
            pedroMotion.update();
            for (Subsystem subsystem : kernel.subsystems()) subsystem.update();
            events.dispatch();
        } catch (RuntimeException e) {
            panic(e);
            throw e;
        } finally {
            ticking = false;
        }
    }

    private void panic(RuntimeException cause) {
        stopOutputs();
        String label = cause instanceof KernelPanic ? "KERNEL PANIC: " : "KERNEL PANIC (" + cause.getClass().getSimpleName() + "): ";
        RobotLog.setGlobalErrorMsg(label + cause.getMessage());
    }

    public void waitUntil(BooleanSupplier condition) throws OpModeStoppedException {
        LinearOpMode linear = requireLinear("waitUntil");
        while (!condition.getAsBoolean()) {
            if (linear.isStopRequested() || stopped) throw new OpModeStoppedException();
            tick();
        }
    }

    public void sleep(long millis) throws OpModeStoppedException {
        requireLinear("sleep");
        long end = nanoClock.getAsLong() + millis * 1_000_000L;
        waitUntil(() -> nanoClock.getAsLong() >= end);
    }

    public void waitForMotion() throws OpModeStoppedException {
        requireLinear("waitForMotion");
        waitUntil(() -> !motion.isBusy());
    }

    private LinearOpMode requireLinear(String method) {
        if (ticking) throw new TickReentrancyPanic();
        if (!(opMode instanceof LinearOpMode)) throw new WrongOpModeTypePanic(method, opMode.getClass());
        return (LinearOpMode) opMode;
    }

    public <T> Subscription on(Event<T> event, Consumer<? super T> handler) {
        return events.on(event, handler);
    }

    public Subscription on(Event<?> event, Runnable handler) {
        return events.on(event, handler);
    }

    public Alliance alliance() {
        return alliance;
    }

    public void alliance(Alliance alliance) {
        this.alliance = alliance;
        pedroMotion.setDriverForwardHeading(alliance.heading(0));
    }

    public boolean has(Class<? extends Subsystem> type) {
        try {
            kernel.get(type);
            return true;
        } catch (SubsystemUnavailableException notInstalled) {
            return false;
        }
    }

    private void stopOutputs() {
        pedroMotion.stop();
        for (Subsystem subsystem : kernel.subsystems()) {
            try {
                subsystem.stop();
            } catch (RuntimeException ignored) {
                // Keep stopping the rest; one broken driver must not leave a motor running.
            }
        }
        events.clear();
    }

    private final class ShutdownOnStop implements OpModeManagerNotifier.Notifications {
        @Override
        public void onOpModePreInit(OpMode ignored) {}

        @Override
        public void onOpModePreStart(OpMode ignored) {}

        @Override
        public void onOpModePostStop(OpMode stoppedOpMode) {
            if (stoppedOpMode != opMode) return;
            stopped = true;
            if (isAutonomous(opMode)) PoseStore.save(motion.pose());
            stopOutputs();
            OpModeManagerImpl manager = ActiveOpMode.manager();
            manager.unregisterListener(this);
        }
    }
}
