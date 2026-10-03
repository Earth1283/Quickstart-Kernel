package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.Set;

public final class Layer {
    private final Pad pad;
    private final Set<Key> modifiers;

    Layer(Pad pad, Set<Key> modifiers) {
        this.pad = pad;
        this.modifiers = modifiers;
    }

    public boolean isActive() {
        for (Key modifier : modifiers) if (!pad.button(modifier).isDown()) return false;
        return true;
    }

    // The base button goes quiet while the modifiers are held, so one press never fires both layers.
    public Button button(Key key) {
        return button(key, Trigger.DEFAULT_THRESHOLD);
    }

    public Button button(Key key, double triggerThreshold) {
        return pad.layered(modifiers, key, triggerThreshold);
    }
}
