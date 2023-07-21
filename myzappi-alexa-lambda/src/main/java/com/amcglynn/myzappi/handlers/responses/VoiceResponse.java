package com.amcglynn.myzappi.handlers.responses;

import com.amcglynn.myenergi.ZappiChargeMode;
import com.amcglynn.myenergi.exception.ClientException;
import com.amcglynn.myenergi.exception.ServerCommunicationException;
import com.amcglynn.myzappi.TariffNotFoundException;
import com.amcglynn.myzappi.UserNotLinkedException;
import com.amcglynn.myzappi.core.exception.UserNotLoggedInException;

import java.util.HashMap;
import java.util.Map;

public class VoiceResponse {
    private static final Map<Object, String> RESPONSE_MAP = new HashMap<>();
    public static final String NOT_FOUND = "Voice response not found";
    static {
        RESPONSE_MAP.put(UserNotLoggedInException.class, "You are not registered. Please register on my zappi unofficial dot com with your my energy API key and serial number.");
        RESPONSE_MAP.put(UserNotLinkedException.class, "You need to set up account linking first on Alexa for the My Zappi skill.");
        RESPONSE_MAP.put(TariffNotFoundException.class, "You need to set up your tariffs on my zappi unofficial dot com to use this feature.");
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
