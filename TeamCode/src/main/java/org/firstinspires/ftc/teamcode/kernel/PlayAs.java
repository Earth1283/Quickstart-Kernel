package org.firstinspires.ftc.teamcode.kernel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Applied in new Robot(), before onInit(), so poses built there are already on the right side.
// Wins over the alliance handed over from Auto.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PlayAs {
    Alliance value();
}
