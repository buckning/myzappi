package com.amcglynn.sqs;

public class Properties {

    /**
     * Client from Alexa permissions page under Alexa Skill Messaging
     * @return Alexa client Id
     */
    public String getAlexaClientId() {
        return System.getenv("alexaClientId");
    }

    /**
     * Client from Alexa permissions page under Alexa Skill Messaging
     * @return Alexa client secret
     */
    public String getAlexaClientSecret() {
        return System.getenv("alexaClientSecret");
    }

    public Properties() {
    }
}
