package com.smartfinance.wallet.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MINIMUM_CHARACTER_COUNT = 12;
    private static final int MAXIMUM_UTF8_BYTE_COUNT = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        int characterCount = password.codePointCount(0, password.length());
        int utf8ByteCount = password.getBytes(StandardCharsets.UTF_8).length;

        return !password.isBlank()
                && characterCount >= MINIMUM_CHARACTER_COUNT
                && utf8ByteCount <= MAXIMUM_UTF8_BYTE_COUNT;
    }
}
