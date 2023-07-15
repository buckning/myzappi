package com.amcglynn.myzappi.core.config;

import com.amcglynn.myzappi.core.exception.MissingConfigurationException;

public class Properties {

    public String getAwsRegion() {
        return getOrDefault("awsRegion", "eu-west-1");
    }

    public String getLoginUrl() {
        return getOrDefault("loginUrl", "http://localhost:8080/login");
    }

    public String getRegisterUrl() {
        return getOrDefault("registerUrl", "http://localhost:8080/hub");
    }

    public String getDevFeatureToggle() {
        return getOrDefault("devFeatureEnabled", "notConfigured");
    }

    public String getKmsKeyArn() {
        return getOrThrow("kmsKeyArn");
    }

    public String getSkillId() {
        return getOrThrow("skillId");
    }

    private String getOrDefault(String propertyName, String defaultValue) {
        var property = System.getenv(propertyName);
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    private String getOrThrow(String propertyName) {
        var property = System.getenv(propertyName);
        if (property == null) {
            throw new MissingConfigurationException("Property " + propertyName + " is not configured");
        }
        return property;
    }
}
