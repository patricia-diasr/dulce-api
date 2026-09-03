package com.dulce.backend.common.validation;

public final class PhoneNormalizer {

    private PhoneNormalizer() {}

    public static String normalize(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        String digits = rawPhone.replaceAll("\\D", "");

        if ((digits.length() == 12 || digits.length() == 13) && digits.startsWith("55")) {
            digits = digits.substring(2);
        }

        return digits;
    }
}
