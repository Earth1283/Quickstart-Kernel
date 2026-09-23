package org.firstinspires.ftc.teamcode.kernel.input;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class FakePad implements PadSource {
    final Set<Key> down = EnumSet.noneOf(Key.class);
    final Map<PadAxis, Double> axes = new EnumMap<>(PadAxis.class);

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
