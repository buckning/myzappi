package com.amcglynn.sqs;

public class Properties extends com.amcglynn.myzappi.core.config.Properties {

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

    @Override
    public String getSkillId() {
        throw new IllegalStateException("Skill ID not required for schedule service");
    }

    public Properties() {
    }
}
