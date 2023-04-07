package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * IOTimer annotation to schedule timed events for specific period or date
 **/
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IOTimer {
    /**
     * The cron expression is the string to define specific period or date.
     *
     * @return cron expression string
     */
    String cron() default "";

    /**
     * limit to interrupt.
     *
     * @return
     */
    long limit() default 0;
    
    /**
     * intermediate delay.
     *
     * @return
     */
    long delay() default 0;

    /**
     * Time unit used for the timer duration
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

}