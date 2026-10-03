package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class FakePad implements PadSource {
    final Set<Key> down = EnumSet.noneOf(Key.class);
    final Map<PadAxis, Double> axes = new EnumMap<>(PadAxis.class);

    boolean connected = true;
    final java.util.List<String> output = new java.util.ArrayList<>();

    @Override
    public boolean connected() {
        return connected;
    }

    @Override
    public void rumble(double left, double right, int millis) {
        output.add("rumble " + left + " " + right + " " + millis);
    }

    @Override
    public void rumbleBlips(int count) {
        output.add("blips " + count);
    }

    @Override
    public void led(double red, double green, double blue) {
        output.add("led " + red + " " + green + " " + blue);
    }

    @Override
    public boolean isDown(Key key) {
        return down.contains(key);
    }

    @Override
    public double axis(PadAxis axis) {
        Double value = axes.get(axis);
        return value == null ? 0 : value;
    }
}
