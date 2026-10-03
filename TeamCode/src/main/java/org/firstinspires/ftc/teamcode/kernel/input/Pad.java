package org.firstinspires.ftc.teamcode.kernel.input;

import org.firstinspires.ftc.teamcode.kernel.errors.BindingPanic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class Pad {
    public final Button a, b, x, y;
    public final Button dpadUp, dpadDown, dpadLeft, dpadRight;
    public final Button leftBumper, rightBumper;
    public final Button leftStickButton, rightStickButton;
    public final Button back, start, guide;
    public final Trigger leftTrigger, rightTrigger;
    public final Stick leftStick, rightStick;

    private final String name;
    private final Map<Key, Button> buttonsByKey = new EnumMap<>(Key.class);
    private final Map<Set<Key>, Layer> layers = new HashMap<>();
    private final Map<Button, Map<Set<Key>, Button>> variantsByBase = new HashMap<>();
    private final PadSource source;
    private final Input input;

    Pad(String name, PadSource source, double stickDeadband, double stickCurve, Input input) {
        this.name = name;
        this.source = source;
        this.input = input;
        for (Key key : Key.values()) {
            if (key.isTrigger()) continue;
            buttonsByKey.put(key, new Button(name + "." + key.name().toLowerCase(), () -> source.isDown(key), input));
        }
        a = buttonsByKey.get(Key.A);
        b = buttonsByKey.get(Key.B);
        x = buttonsByKey.get(Key.X);
        y = buttonsByKey.get(Key.Y);
        dpadUp = buttonsByKey.get(Key.DPAD_UP);
        dpadDown = buttonsByKey.get(Key.DPAD_DOWN);
        dpadLeft = buttonsByKey.get(Key.DPAD_LEFT);
        dpadRight = buttonsByKey.get(Key.DPAD_RIGHT);
        leftBumper = buttonsByKey.get(Key.LEFT_BUMPER);
        rightBumper = buttonsByKey.get(Key.RIGHT_BUMPER);
        leftStickButton = buttonsByKey.get(Key.LEFT_STICK_BUTTON);
        rightStickButton = buttonsByKey.get(Key.RIGHT_STICK_BUTTON);
        back = buttonsByKey.get(Key.BACK);
        start = buttonsByKey.get(Key.START);
        guide = buttonsByKey.get(Key.GUIDE);
        leftTrigger = new Trigger(name + ".left_trigger", source, PadAxis.LEFT_TRIGGER, input);
        rightTrigger = new Trigger(name + ".right_trigger", source, PadAxis.RIGHT_TRIGGER, input);
        leftStick = new Stick(source, PadAxis.LEFT_STICK_RIGHT, PadAxis.LEFT_STICK_UP, stickDeadband, stickCurve);
        rightStick = new Stick(source, PadAxis.RIGHT_STICK_RIGHT, PadAxis.RIGHT_STICK_UP, stickDeadband, stickCurve);
    }

    public Button button(Key key) {
        return button(key, Trigger.DEFAULT_THRESHOLD);
    }

    public Button button(Key key, double triggerThreshold) {
        if (key == Key.LEFT_TRIGGER) return leftTrigger.asButton(triggerThreshold);
        if (key == Key.RIGHT_TRIGGER) return rightTrigger.asButton(triggerThreshold);
        return buttonsByKey.get(key);
    }

    public Button chord(Key... keys) {
        if (keys.length == 0) throw new BindingPanic("chord() needs at least one key.");
        Button chord = button(keys[0]);
        for (int i = 1; i < keys.length; i++) chord = chord.and(button(keys[i]));
        return chord;
    }

    public Layer layer(Key... modifiers) {
        if (modifiers.length == 0) throw new BindingPanic("layer() needs at least one modifier key.");
        Set<Key> held = EnumSet.copyOf(Arrays.asList(modifiers));
        Layer layer = layers.get(held);
        if (layer == null) {
            layer = new Layer(this, held);
            layers.put(held, layer);
        }
        return layer;
    }

    // The most specific layer wins: each variant of a key goes quiet while a layer that adds
    // modifiers on top of its own is held, so one press never fires two layers.
    Button layered(Set<Key> modifiers, Key key, double triggerThreshold) {
        if (modifiers.contains(key)) throw new BindingPanic(name + " layer " + modifiers + " lists " + key + " as its own modifier.");
        Button base = button(key, triggerThreshold);
        Map<Set<Key>, Button> variants = variantsByBase.get(base);
        if (variants == null) {
            variants = new HashMap<>();
            variants.put(EnumSet.noneOf(Key.class), base);
            variantsByBase.put(base, variants);
        }
        Button existing = variants.get(modifiers);
        if (existing != null) return existing;

        BooleanSupplier modifiersHeld = allRawDown(modifiers);
        Button layered = new Button(layerName(modifiers) + "+" + base.name(), () -> modifiersHeld.getAsBoolean() && base.rawDown(), input);
        for (Map.Entry<Set<Key>, Button> variant : variants.entrySet()) {
            Set<Key> other = variant.getKey();
            if (modifiers.containsAll(other)) variant.getValue().suppressWhile(modifiersHeld);
            else if (other.containsAll(modifiers)) layered.suppressWhile(allRawDown(other));
        }
        variants.put(modifiers, layered);
        return layered;
    }

    private BooleanSupplier allRawDown(Set<Key> keys) {
        List<Button> buttons = new ArrayList<>();
        for (Key key : keys) buttons.add(button(key));
        return () -> {
            for (Button button : buttons) if (!button.rawDown()) return false;
            return true;
        };
    }

    private String layerName(Set<Key> modifiers) {
        StringBuilder sb = new StringBuilder();
        for (Key modifier : modifiers) {
            if (sb.length() > 0) sb.append('+');
            sb.append(button(modifier).name());
        }
        return sb.toString();
    }

    public boolean isConnected() {
        return source.connected();
    }

    public void rumble(int millis) {
        rumble(1.0, 1.0, millis);
    }

    public void rumble(double left, double right, int millis) {
        source.rumble(left, right, millis);
    }

    public void rumbleBlips(int count) {
        source.rumbleBlips(count);
    }

    public void led(double red, double green, double blue) {
        source.led(red, green, blue);
    }
}
