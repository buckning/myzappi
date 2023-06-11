package com.amcglynn.myzappi.core.service;

import com.amcglynn.myzappi.core.model.LoginCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginCodeTest {

    @Test
    void testGenerateLoginCodeGeneratesCorrectSizeCode() {
        var code = new LoginCode();
        assertThat(code.toString()).hasSize(6);
    }

    @Test
    void testGenerateLoginCodeGeneratesAlphaNumericChars() {
        var code = new LoginCode();
        assertThat(code.toString()).matches("[a-z0-9]+");
    }
}
