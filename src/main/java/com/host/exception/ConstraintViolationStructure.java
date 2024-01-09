package com.host.exception;

import jakarta.validation.ConstraintViolation;
import org.h2.util.StringUtils;
import org.jboss.resteasy.api.validation.ConstraintType;

import java.util.List;

public class ConstraintViolationStructure {
    public List<ParameterViolation> parameterViolations;

    public static class ParameterViolation {

        public ParameterViolation(final ConstraintViolation constraintViolation){
            var path = constraintViolation.getPropertyPath().toString();
            this.constraintType = ConstraintType.Type.PARAMETER.name();
            this.message = constraintViolation.getMessage();
            this.value = constraintViolation.getInvalidValue().toString();
            this.path = "create.booking"+ (StringUtils.isNullOrEmpty(path)?"":"."+path);
        }

        public final String constraintType;
        public final String path;
        public final String message;
        public final String value;
    }
}

