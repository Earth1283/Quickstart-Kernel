package org.firstinspires.ftc.teamcode.kernel.motion;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.kernel.Angles;
import org.firstinspires.ftc.teamcode.kernel.Subsystem;
import org.firstinspires.ftc.teamcode.kernel.events.EventBus;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.LongSupplier;

public final class PedroMotion implements Motion, Subsystem {
    private final FollowerPort follower;
    private final EventBus events;
    private final MotionTuning tuning;
    private final LongSupplier nanoClock;

    private final Deque<Path> pending = new ArrayDeque<>();
    private Path current;
    private Mode mode = Mode.IDLE;

    private double requestedForward, requestedLeft, requestedTurn;
    private double driverForwardHeading;

    private Vector2D aimTarget;
    private double previousAimError;
    private long previousAimNanos;
    private double turnTarget;

    public PedroMotion(Follower follower, EventBus events, MotionTuning tuning, LongSupplier nanoClock) {
        this(new PedroFollowerPort(follower), events, tuning, nanoClock);
    }

    PedroMotion(FollowerPort follower, EventBus events, MotionTuning tuning, LongSupplier nanoClock) {
        this.follower = follower;
        this.events = events;
        this.tuning = tuning;
        this.nanoClock = nanoClock;
    }

    @Override
    public void drive(double forward, double left, double counterClockwise) {
        boolean autonomousMotion = mode == Mode.FOLLOW || mode == Mode.TURN || mode == Mode.HOLD;
        if (autonomousMotion && !exceedsTakeover(forward, left, counterClockwise)) return;
        if (mode != Mode.DRIVE) {
            pending.clear();
            current = null;
            mode = Mode.DRIVE;
        }
        requestedForward = forward;
        requestedLeft = left;
        requestedTurn = counterClockwise;
    }

    private boolean exceedsTakeover(double... inputs) {
        for (double input : inputs) if (Math.abs(input) > tuning.driverTakeoverThreshold) return true;
        return false;
    }

    @Override
    public void follow(Path path) {
        pending.clear();
        start(path);
    }

    @Override
    public void queue(Path... paths) {
        pending.addAll(Arrays.asList(paths));
        if (mode != Mode.FOLLOW && !pending.isEmpty()) start(pending.poll());
    }

    private void start(Path path) {
        current = path;
        mode = Mode.FOLLOW;
        follower.follow(path);
    }

    @Override
    public void aimTo(Vector2D target) {
        boolean targetChanged = aimTarget == null || aimTarget.x() != target.x() || aimTarget.y() != target.y();
        aimTarget = target;
        if (targetChanged) previousAimNanos = 0;
        if (targetChanged && (mode == Mode.IDLE || mode == Mode.HOLD)) holdFacing(target);
    }

    @Override
    public void stopAiming() {
        aimTarget = null;
    }

    @Override
    public boolean isAiming() {
        return aimTarget != null;
    }

    private void holdFacing(Vector2D target) {
        Pose pose = follower.pose();
        follower.hold(new Pose(pose.x(), pose.y(), Angles.toward(pose.x(), pose.y(), target.x(), target.y())));
        mode = Mode.HOLD;
    }

    @Override
    public void turnTo(double heading) {
        pending.clear();
        current = null;
        turnTarget = heading;
        Pose pose = follower.pose();
        follower.hold(new Pose(pose.x(), pose.y(), heading));
        mode = Mode.TURN;
    }

    @Override
    public void hold() {
        pending.clear();
        current = null;
        follower.hold(follower.pose());
        mode = Mode.HOLD;
    }

    @Override
    public void cancel() {
        pending.clear();
        current = null;
        aimTarget = null;
        follower.stop();
        mode = Mode.IDLE;
    }

    @Override
    public void stop() {
        cancel();
    }

    @Override
    public Pose pose() {
        return follower.pose();
    }

    @Override
    public void setPose(Pose pose) {
        follower.setPose(pose);
    }

    @Override
    public void resetDriverForward() {
        driverForwardHeading = follower.pose().heading();
    }

    public void setDriverForwardHeading(double heading) {
        driverForwardHeading = heading;
    }

    @Override
    public boolean isBusy() {
        return mode == Mode.FOLLOW || mode == Mode.TURN;
    }

    @Override
    public int queued() {
        return pending.size();
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public void update() {
        if (mode == Mode.DRIVE) applyDriverInput();
        if (mode == Mode.TURN) checkTurnDone();
        follower.update();
        if (mode == Mode.FOLLOW && !follower.following()) finishPath();
    }

    private void applyDriverInput() {
        Pose pose = follower.pose();
        double forward = requestedForward;
        double left = requestedLeft;
        if (tuning.fieldCentric) {
            double theta = pose.heading() - driverForwardHeading;
            forward = requestedForward * Math.cos(theta) + requestedLeft * Math.sin(theta);
            left = -requestedForward * Math.sin(theta) + requestedLeft * Math.cos(theta);
        }
        double turn = aimTarget != null ? aimTurn(pose) : requestedTurn;
        follower.manual(forward, left, turn);
        requestedForward = requestedLeft = requestedTurn = 0;
    }

    private double aimTurn(Pose pose) {
        double desired = Angles.toward(pose.x(), pose.y(), aimTarget.x(), aimTarget.y());
        double error = Angles.error(pose.heading(), desired);
        long now = nanoClock.getAsLong();
        double derivative = previousAimNanos == 0 ? 0 : (error - previousAimError) / ((now - previousAimNanos) / 1e9);
        previousAimError = error;
        previousAimNanos = now;
        double turn = tuning.aimKp * error + tuning.aimKd * derivative;
        return Math.max(-tuning.aimMaxTurn, Math.min(tuning.aimMaxTurn, turn));
    }

    private void checkTurnDone() {
        if (Math.abs(Angles.error(follower.pose().heading(), turnTarget)) > tuning.turnToleranceRadians) return;
        mode = Mode.HOLD;
        events.emit(TURN_DONE, turnTarget);
    }

    private void finishPath() {
        Path finished = current;
        current = null;
        events.emit(PATH_DONE, finished);
        if (!pending.isEmpty()) {
            start(pending.poll());
            return;
        }
        events.emit(QUEUE_EMPTY);
        if (aimTarget != null) holdFacing(aimTarget);
        else mode = follower.holding() ? Mode.HOLD : Mode.IDLE;
    }
}
