package com.amcglynn.myzappi;

public class TariffNotFoundException extends RuntimeException {

    public TariffNotFoundException(String userId) {
        super("Tariff not found for user - " + userId);
    }
}
