package org.firstinspires.ftc.teamcode.kernel.telemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Lines appear sorted by order(), then by key: reflection doesn't report declaration order.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Watch {
    String value() default "";

    int order() default 0;
}
