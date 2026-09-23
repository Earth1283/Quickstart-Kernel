package org.firstinspires.ftc.teamcode.kernel.motion;

import static com.pedropathing.api.Paths.line;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.kernel.events.EventBus;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PedroMotionTest {
    private static final double EPS = 1e-9;

    private final FakeFollower follower = new FakeFollower();
    private final EventBus events = new EventBus();
    private final MotionTuning tuning = new MotionTuning();
    private long nowNanos;
    private PedroMotion motion;

    private final List<Path> pathsDone = new ArrayList<>();
    private int queueEmpties;
    private final List<Double> turnsDone = new ArrayList<>();

    private final Path first = line(new Pose(0, 0), new Pose(10, 0)).constant(0);
    private final Path second = line(new Pose(10, 0), new Pose(10, 10)).constant(0);
    private final Path third = line(new Pose(10, 10), new Pose(0, 10)).constant(0);

    @Before
    public void setUp() {
        tuning.fieldCentric = false;
        motion = new PedroMotion(follower, events, tuning, () -> nowNanos);
        events.on(Motion.PATH_DONE, pathsDone::add);
        events.on(Motion.QUEUE_EMPTY, () -> queueEmpties++);
        events.on(Motion.TURN_DONE, turnsDone::add);
    }

    private void tick() {
        nowNanos += 20_000_000L;
        motion.update();
        events.dispatch();
    }

    private void finishCurrentPath() {
        follower.finishCurrentPathOnNextUpdate = true;
        tick();
    }

    @Test
    public void queueRunsPathsInOrderAndReportsEachOne() {
        motion.queue(first, second, third);
        assertSame(first, follower.following);
        assertEquals(2, motion.queued());
        assertTrue(motion.isBusy());

        finishCurrentPath();
        assertSame(second, follower.following);
        finishCurrentPath();
        assertSame(third, follower.following);
        finishCurrentPath();

        assertEquals(Arrays.asList(first, second, third), pathsDone);
        assertEquals(1, queueEmpties);
        assertFalse(motion.isBusy());
        assertEquals(Motion.Mode.HOLD, motion.mode());
    }

    @Test
    public void queueingWhileFollowingAppends() {
        motion.follow(first);
        motion.queue(second);
        assertSame(first, follower.following);
        finishCurrentPath();
        assertSame(second, follower.following);
    }

    @Test
    public void followReplacesTheQueue() {
        motion.queue(first, second);
        motion.follow(third);
        assertSame(third, follower.following);
        assertEquals(0, motion.queued());
    }

    @Test
    public void endsIdleWhenPedroDoesNotHoldTheEndpoint() {
        follower.holdAtPathEnd = false;
        motion.follow(first);
        finishCurrentPath();
        assertEquals(Motion.Mode.IDLE, motion.mode());
    }

    @Test
    public void smallStickNoiseDoesNotInterruptAPath() {
        motion.queue(first, second);
        motion.drive(0.01, -0.02, 0.0);
        tick();
        assertEquals(Motion.Mode.FOLLOW, motion.mode());
        assertEquals(1, motion.queued());
    }

    @Test
    public void realStickInputTakesOverAndClearsTheQueue() {
        motion.queue(first, second);
        motion.drive(0.5, 0, 0);
        tick();
        assertEquals(Motion.Mode.DRIVE, motion.mode());
        assertEquals(0, motion.queued());
        assertArrayEquals(new double[]{0.5, 0, 0}, follower.lastManual, EPS);
    }

    @Test
    public void driveInputIsConsumedEachTick() {
        motion.drive(0.5, 0.25, 0.1);
        tick();
        tick();
        assertArrayEquals(new double[]{0, 0, 0}, follower.lastManual, EPS);
    }

    @Test
    public void fieldCentricRotatesByHeadingRelativeToDriverForward() {
        tuning.fieldCentric = true;
        follower.pose = new Pose(0, 0, Math.PI / 2);
        motion.drive(1, 0, 0);
        tick();
        assertArrayEquals(new double[]{0, -1, 0}, follower.lastManual, EPS);

        motion.resetDriverForward();
        motion.drive(1, 0, 0);
        tick();
        assertArrayEquals(new double[]{1, 0, 0}, follower.lastManual, EPS);
    }

    @Test
    public void aimWhileDrivingOverridesTurnAndTurnsTowardTarget() {
        follower.pose = new Pose(0, 0, 0);
        motion.aimTo(Vector2D.cartesian(0, 10));
        motion.drive(0.3, 0, -1.0);
        tick();
        assertEquals(0.3, follower.lastManual[0], EPS);
        assertTrue("should turn counter-clockwise toward +y", follower.lastManual[2] > 0);
        assertTrue(follower.lastManual[2] <= tuning.aimMaxTurn + EPS);

        motion.stopAiming();
        motion.drive(0.3, 0, -1.0);
        tick();
        assertEquals(-1.0, follower.lastManual[2], EPS);
    }

    @Test
    public void aimWhileIdleHoldsPositionFacingTarget() {
        follower.pose = new Pose(5, 5, 0);
        motion.aimTo(Vector2D.cartesian(5, 50));
        assertEquals(Motion.Mode.HOLD, motion.mode());
        assertNotNull(follower.holding);
        assertEquals(5, follower.holding.x(), EPS);
        assertEquals(5, follower.holding.y(), EPS);
        assertEquals(Math.PI / 2, follower.holding.heading(), EPS);
    }

    @Test
    public void aimSetDuringAPathTakesEffectWhenTheQueueEmpties() {
        motion.follow(first);
        motion.aimTo(Vector2D.cartesian(0, 100));
        assertSame(first, follower.following);
        follower.pose = new Pose(10, 0, 0);
        finishCurrentPath();
        assertEquals(Motion.Mode.HOLD, motion.mode());
        assertEquals(Math.atan2(100, -10), follower.holding.heading(), EPS);
    }

    @Test
    public void turnToReportsDoneWithinTolerance() {
        motion.turnTo(Math.PI / 2);
        assertTrue(motion.isBusy());
        tick();
        assertEquals(Collections.emptyList(), turnsDone);
        follower.pose = new Pose(0, 0, Math.PI / 2 - Math.toRadians(1));
        tick();
        assertEquals(Collections.singletonList(Math.PI / 2), turnsDone);
        assertFalse(motion.isBusy());
        assertEquals(Motion.Mode.HOLD, motion.mode());
    }

    @Test
    public void cancelStopsEverything() {
        motion.queue(first, second);
        motion.aimTo(Vector2D.cartesian(1, 1));
        motion.cancel();
        assertEquals(Motion.Mode.IDLE, motion.mode());
        assertEquals(0, motion.queued());
        assertFalse(motion.isAiming());
        assertEquals(1, follower.stops);
    }
}
