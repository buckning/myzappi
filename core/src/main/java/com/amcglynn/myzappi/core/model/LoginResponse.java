package com.amcglynn.myzappi.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginResponse {
    private ZappiCredentials creds;
    private LoginState loginState;
}
