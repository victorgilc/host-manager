package com.host.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import java.time.LocalDate;

@Slf4j
public class DateAfterValidator implements ConstraintValidator<DateAfter, Object> {
    private String startFieldName;
    private String endFieldName;

    @Override
    public void initialize(final DateAfter constraintAnnotation) {
        startFieldName = constraintAnnotation.start();
        endFieldName = constraintAnnotation.end();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        try {
            final LocalDate startDate = LocalDate.parse(BeanUtils.getProperty(value, startFieldName));
            final LocalDate endDate = LocalDate.parse(BeanUtils.getProperty(value, endFieldName));
            return endDate.isAfter(startDate);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}