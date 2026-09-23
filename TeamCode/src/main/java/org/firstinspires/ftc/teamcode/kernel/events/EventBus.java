package org.firstinspires.ftc.teamcode.kernel.events;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;
import org.firstinspires.ftc.teamcode.kernel.errors.KernelPanic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Handlers run at dispatch() (end of tick), never inside emit(). Events emitted by a handler
// are delivered on the next tick, so mutually-triggering handlers can't stall the loop.
public final class EventBus {
    private static final class Pending<T> {
        final Event<T> event;
        final T payload;

        Pending(Event<T> event, T payload) {
            this.event = event;
            this.payload = payload;
        }
    }

    private final Map<Event<?>, List<Consumer<?>>> handlers = new HashMap<>();
    private List<Pending<?>> queue = new ArrayList<>();

    public <T> Subscription on(Event<T> event, Consumer<? super T> handler) {
        List<Consumer<?>> list = handlers.get(event);
        if (list == null) {
            list = new ArrayList<>();
            handlers.put(event, list);
        }
        final List<Consumer<?>> owner = list;
        final Consumer<T> entry = handler::accept; // fresh identity per subscription
        owner.add(entry);
        return () -> owner.remove(entry);
    }

    public Subscription on(Event<?> event, Runnable handler) {
        return on(event, payload -> handler.run());
    }

    public <T> void emit(Event<T> event, T payload) {
        queue.add(new Pending<>(event, payload));
    }

    public void emit(Event<Void> event) {
        emit(event, null);
    }

    public void dispatch() {
        if (queue.isEmpty()) return;
        List<Pending<?>> batch = queue;
        queue = new ArrayList<>();
        for (Pending<?> pending : batch) deliver(pending);
    }

    public void clear() {
        queue.clear();
    }

    @SuppressWarnings("unchecked")
    private <T> void deliver(Pending<T> pending) {
        List<Consumer<?>> list = handlers.get(pending.event);
        if (list == null) return;
        for (Consumer<?> handler : new ArrayList<>(list)) { // handlers may cancel themselves
            try {
                ((Consumer<T>) handler).accept(pending.payload);
            } catch (KernelPanic panic) {
                throw panic;
            } catch (RuntimeException e) {
                throw new BindingPanic("Handler for event " + pending.event + " threw " + e, e);
            }
        }
    }
}
