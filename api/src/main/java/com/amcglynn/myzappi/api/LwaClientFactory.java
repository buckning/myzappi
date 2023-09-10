package com.amcglynn.myzappi.api;

import com.amcglynn.lwa.LwaClient;

public class LwaClientFactory {

    public LwaClient newLwaClient() {
        return new LwaClient();
    }
}
