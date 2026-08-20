package com.dulce.backend.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String digits = NON_DIGITS.matcher(value).replaceAll("");

        if ((digits.length() == 12 || digits.length() == 13) && digits.startsWith("55")) {
            digits = digits.substring(2);
        }

        return digits.length() == 10 || digits.length() == 11;
    }
}
