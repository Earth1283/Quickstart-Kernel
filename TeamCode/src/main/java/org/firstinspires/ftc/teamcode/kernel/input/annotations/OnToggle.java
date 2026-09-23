package org.firstinspires.ftc.teamcode.kernel.input.annotations;

import org.firstinspires.ftc.teamcode.kernel.input.Key;
import org.firstinspires.ftc.teamcode.kernel.input.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// The annotated method takes one boolean: the new toggle state (first press = true).
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnToggle {
    Key value();

    int gamepad() default 1;

    Key[] with() default {};

    double threshold() default Trigger.DEFAULT_THRESHOLD;
}
