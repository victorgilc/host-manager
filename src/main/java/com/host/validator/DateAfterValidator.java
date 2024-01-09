package com.host.validator;

import com.host.model.Booking;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DateAfterValidator implements ConstraintValidator<DateAfter, Booking> {
    @Override
    public boolean isValid(final Booking booking, final ConstraintValidatorContext context) {
        try {
            return booking.end.isAfter(booking.start);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}