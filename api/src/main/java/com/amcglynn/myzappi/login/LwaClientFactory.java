package com.amcglynn.myzappi.login;

import com.amcglynn.lwa.LwaClient;

public class LwaClientFactory {

    public LwaClient newLwaClient() {
        return new LwaClient();
    }
}
