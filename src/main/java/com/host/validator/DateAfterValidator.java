package com.host.validator;

import com.host.model.Booking;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateAfterValidator implements ConstraintValidator<DateAfter, Booking> {
    @Override
    public boolean isValid(final Booking booking, final ConstraintValidatorContext context) {
        return booking.end.isAfter(booking.start);
    }
}