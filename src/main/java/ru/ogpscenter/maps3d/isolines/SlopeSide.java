package ru.ogpscenter.maps3d.isolines;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by Mikhail Senin (Mikhail.Senin@jetbrains.com) on 28.10.16.
 * Enum for slope side.
 */
public enum SlopeSide {
    NONE(0),
    LEFT(1),
    RIGHT(-1);

    public int getIntValue() {
        return intValue;
    }

    private final int intValue;

    SlopeSide(int intValue) {
        this.intValue = intValue;
    }

    public static SlopeSide fromInt(int intVal) {
        Optional<SlopeSide> val = Arrays.stream(values()).filter(it -> it.getIntValue() == intVal).findFirst();
        if (val.isPresent()) {
            return val.get();
        }
        return SlopeSide.NONE;
    }

    public boolean isOppositeTo(SlopeSide slopeSide) {
        return slopeSide.getIntValue() == - this.getIntValue();
    }

    public SlopeSide getOpposite() {
        return SlopeSide.fromInt(-1 * this.getIntValue());
    }
}
