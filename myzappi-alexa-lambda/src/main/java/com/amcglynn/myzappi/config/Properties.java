package com.amcglynn.myzappi.config;

import com.amcglynn.myzappi.exception.MissingConfigurationException;

public class Properties {

    public String getAwsRegion() {
        return getOrDefault("awsRegion", "eu-west-1");
    }

    public String getKmsKeyArn() {
        return getOrThrow("kmsKeyArn");
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
