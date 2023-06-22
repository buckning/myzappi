package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;
import com.amcglynn.myzappi.core.model.LoginState;

import java.util.HashMap;
import java.util.Map;

public class VoiceResponse {
    private static final Map<Object, String> RESPONSE_MAP = new HashMap<>();
    public static final String NOT_FOUND = "Voice response not found";
    static {
        RESPONSE_MAP.put(LoginState.NEW, "Thank you, your {brandName} code is {myZappiCode}. Please enter this code on my zappi unofficial dot com with your API key and serial number");
        RESPONSE_MAP.put(LoginState.EXISTING_LOGIN_CODE, "Thank you, your {brandName} code is {myZappiCode}. Please use this on the {brandName} website when configuring your API key");
        RESPONSE_MAP.put(LoginState.RECREATED_NEW_CODE, "Your old {brandName} code has expired. Your new {brandName} code is {myZappiCode}. Please use this on the {brandName} website when configuring your API key");
        RESPONSE_MAP.put(LoginState.LOGIN_COMPLETE, "You already have Zappi credentials configured. There is no need to login again.");
        RESPONSE_MAP.put(UserNotLoggedInException.class, "You need to login first.");
        RESPONSE_MAP.put(ClientException.class, "Could not authenticate with my energy APIs. Perhaps you entered the wrong serial number or API key. Ask me to log out and log in again to reset them.");
        RESPONSE_MAP.put(ServerCommunicationException.class, "I couldn't communicate with my energy servers.");
        RESPONSE_MAP.put(ZappiChargeMode.class, "Changing charge mode to {zappiChargeMode}. This may take a few minutes.");
        RESPONSE_MAP.put(Exception.class, "There was an unexpected error.");
    }

    public static String get(Object object) {
        return RESPONSE_MAP.getOrDefault(object, NOT_FOUND);
    }

    private VoiceResponse() {
    }
}
