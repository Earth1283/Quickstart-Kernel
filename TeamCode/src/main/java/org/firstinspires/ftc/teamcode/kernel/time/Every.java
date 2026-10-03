package org.firstinspires.ftc.teamcode.kernel.time;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Every {
    long value();

    // Off by default so, like key bindings, nothing moves before start.
    boolean duringInit() default false;
}
