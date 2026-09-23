package org.firstinspires.ftc.teamcode.kernel.input.annotations;

import org.firstinspires.ftc.teamcode.kernel.input.Button;
import org.firstinspires.ftc.teamcode.kernel.input.Key;
import org.firstinspires.ftc.teamcode.kernel.input.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnDoubleTap {
    Key value();

    long windowMs() default Button.DEFAULT_DOUBLE_TAP_WINDOW_MS;

    int gamepad() default 1;

    Key[] with() default {};

    double threshold() default Trigger.DEFAULT_THRESHOLD;
}
