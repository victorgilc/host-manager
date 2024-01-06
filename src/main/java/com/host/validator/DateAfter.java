package com.host.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateAfterValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DateAfter {
    String message() default "start date greater than end date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String start();
    String end();
}
