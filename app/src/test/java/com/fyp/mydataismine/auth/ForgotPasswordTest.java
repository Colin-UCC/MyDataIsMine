package com.fyp.mydataismine.auth;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForgotPasswordTest {

    private ForgotPassword forgotPasswordActivity;

    @Before
    public void setUp() {
        forgotPasswordActivity = new ForgotPassword();
    }

    @Test
    public void emailValidation_CorrectEmailSimple_ReturnsTrue() {
        assertTrue(forgotPasswordActivity.validateEmail("test@example.com"));
    }

    @Test
    public void emailValidation_EmptyString_ReturnsFalse() {
        assertFalse(forgotPasswordActivity.validateEmail(""));
    }

    @Test
    public void emailValidation_InvalidEmailNoTld_ReturnsFalse() {
        assertFalse(forgotPasswordActivity.validateEmail("test@localhost"));
    }

    @Test
    public void emailValidation_InvalidEmailDoubleDot_ReturnsFalse() {
        assertFalse(forgotPasswordActivity.validateEmail("test@example..com"));
    }
}

