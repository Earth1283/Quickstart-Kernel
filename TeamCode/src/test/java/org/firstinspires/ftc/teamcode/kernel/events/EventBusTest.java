package org.firstinspires.ftc.teamcode.kernel.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventBusTest {
    private static final Event<String> SAID = Event.of("test.said");
    private static final Event<Void> PING = Event.of("test.ping");

    private final EventBus bus = new EventBus();

    @Test
    public void nothingIsDeliveredUntilDispatch() {
        List<String> heard = new ArrayList<>();
        bus.on(SAID, heard::add);
        bus.emit(SAID, "hi");
        assertEquals(Collections.emptyList(), heard);
        bus.dispatch();
        assertEquals(Collections.singletonList("hi"), heard);
    }

    @Test
    public void deliversInEmitOrder() {
        List<String> heard = new ArrayList<>();
        bus.on(SAID, heard::add);
        bus.on(PING, () -> heard.add("ping"));
        bus.emit(SAID, "a");
        bus.emit(PING);
        bus.emit(SAID, "b");
        bus.dispatch();
        assertEquals(Arrays.asList("a", "ping", "b"), heard);
    }

    @Test
    public void eventsEmittedByHandlersWaitForTheNextDispatch() {
        List<String> heard = new ArrayList<>();
        bus.on(PING, () -> bus.emit(SAID, "echo"));
        bus.on(SAID, heard::add);
        bus.emit(PING);
        bus.dispatch();
        assertEquals(Collections.emptyList(), heard);
        bus.dispatch();
        assertEquals(Collections.singletonList("echo"), heard);
    }

    @Test
    public void cancelledSubscriptionsStopHearing() {
        int[] count = {0};
        Subscription subscription = bus.on(PING, () -> count[0]++);
        bus.emit(PING);
        bus.dispatch();
        subscription.cancel();
        bus.emit(PING);
        bus.dispatch();
        assertEquals(1, count[0]);
    }

    @Test
    public void handlerMayCancelItselfWhileBeingDelivered() {
        int[] count = {0};
        Subscription[] self = new Subscription[1];
        self[0] = bus.on(PING, () -> {
            count[0]++;
            self[0].cancel();
        });
        bus.emit(PING);
        bus.emit(PING);
        bus.dispatch();
        assertEquals(1, count[0]);
    }

    @Test
    public void clearDropsPendingEvents() {
        int[] count = {0};
        bus.on(PING, () -> count[0]++);
        bus.emit(PING);
        bus.clear();
        bus.dispatch();
        assertEquals(0, count[0]);
    }

    @Test
    public void throwingHandlerBecomesBindingPanicNamingTheEvent() {
        bus.on(PING, () -> {
            throw new IllegalStateException("nope");
        });
        bus.emit(PING);
        try {
            bus.dispatch();
            fail();
        } catch (BindingPanic panic) {
            assertTrue(panic.getMessage().contains("test.ping"));
        }
    }
}
