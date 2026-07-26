package com.project.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemAuditLog {

    String module() default "";

    String action() default "";

    String actionType() default "UPDATE";

    String targetType() default "";

    String targetId() default "";

    String detail() default "";
}