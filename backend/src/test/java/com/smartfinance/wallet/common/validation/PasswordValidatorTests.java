package com.smartfinance.wallet.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTests {

    private final PasswordValidator validator = new PasswordValidator();

    @Test
    void shouldAcceptPasswordWithinCharacterAndUtf8Limits() {
        assertThat(validator.isValid("twelve-chars", null)).isTrue();
        assertThat(validator.isValid("a".repeat(72), null)).isTrue();
        assertThat(validator.isValid("😀".repeat(18), null)).isTrue();
    }

    @Test
    void shouldRejectInvalidCharacterOrUtf8Lengths() {
        assertThat(validator.isValid("short-value", null)).isFalse();
        assertThat(validator.isValid("a".repeat(73), null)).isFalse();
        assertThat(validator.isValid("😀".repeat(19), null)).isFalse();
        assertThat(validator.isValid(" ".repeat(12), null)).isFalse();
    }

    @Test
    void shouldLetNotBlankHandleNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
