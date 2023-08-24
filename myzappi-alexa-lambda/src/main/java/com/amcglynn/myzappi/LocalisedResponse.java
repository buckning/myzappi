package com.amcglynn.myzappi;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class LocalisedResponse {


    public static String voiceResponse(HandlerInput handlerInput, String key) {
        return voiceResponse(handlerInput, key, Map.of());
    }

    public static String voiceResponse(Locale locale, String key) {
        return voiceResponse(locale, key, Map.of());
    }

    public static String voiceResponse(HandlerInput handlerInput, String key, Map<String, String> substitutions) {
        return voiceResponse(Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale()), key, substitutions);
    }

    public static String voiceResponse(Locale locale, String key, Map<String, String> substitutions) {
        var resourceBundle = ResourceBundle.getBundle("voice-response", locale);
        var str = resourceBundle.getString(key);
        for (var entrySet : substitutions.entrySet()) {
            str = str.replace("{" + entrySet.getKey() + "}", entrySet.getValue());
        }
        return str;
    }

    public static String cardResponse(HandlerInput handlerInput, String key) {
        return cardResponse(handlerInput, key, Map.of());
    }

    public static String cardResponse(Locale locale, String key) {
        return cardResponse(locale, key, Map.of());
    }

    public static String cardResponse(HandlerInput handlerInput, String key, Map<String, String> substitutions) {
        return cardResponse(Locale.forLanguageTag(handlerInput.getRequestEnvelope().getRequest().getLocale()), key, substitutions);
    }

    public static String cardResponse(Locale locale, String key, Map<String, String> substitutions) {
        var resourceBundle = ResourceBundle.getBundle("card-response", locale);
        var str = resourceBundle.getString(key);
        for (var entrySet : substitutions.entrySet()) {
            str = str.replace("{" + entrySet.getKey() + "}", entrySet.getValue());
        }
        return str;
    }
}
