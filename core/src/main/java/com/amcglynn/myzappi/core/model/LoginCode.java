package com.amcglynn.myzappi.core.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;

@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginCode {

    private static final int CODE_LENGTH = 6;
    private static final String CODE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";

    private final String code;

    public LoginCode() {
        var random = new SecureRandom();
        var generatedCode = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CODE_CHARS.length());
            generatedCode.append(CODE_CHARS.charAt(index));
        }

        this.code = generatedCode.toString();
    }

    /**
     * Create a login code from an existing string. This is reserved for creating login codes that were previously
     * generated and stored in the DB.
     * @param loginCode login code value
     */
    public static LoginCode from(String loginCode) {
        return new LoginCode(loginCode);
    }

    @Override
    public String toString() {
        return code;
    }
}
