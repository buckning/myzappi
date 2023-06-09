package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myzappi.core.model.LoginState;

import java.util.HashMap;
import java.util.Map;

public class CardResponse {
    private static final Map<Object, String> RESPONSE_MAP = new HashMap<>();

    static {
        RESPONSE_MAP.put(LoginState.NEW, "Thank you, your {brandName} code is {myZappiCode}. Please use this on the {brandName} website when configuring your API key.");
        RESPONSE_MAP.put(LoginState.EXISTING_LOGIN_CODE, "Thank you, your {brandName} code is {myZappiCode}. Please use this on the {brandName} website when configuring your API key.");
        RESPONSE_MAP.put(LoginState.LOGIN_COMPLETE, "You already have Zappi credentials configured. There is no need to login again.");
    }

    public static String get(Object object) {
        return RESPONSE_MAP.getOrDefault(object, "Card response not found");
    }

    private CardResponse() {
    }
}
