package com.amcglynn.myzappi.core.config;

import com.amcglynn.myzappi.core.exception.MissingConfigurationException;

public class Properties {

    public String getAwsRegion() {
        return getOrDefault("awsRegion", "eu-west-1");
    }

    public String getLoginUrl() {
        return getOrDefault("loginUrl", "http://localhost:8080/login");
    }

    public String getLogoutUrl() {
        return getOrDefault("logoutUrl", "http://localhost:8080/logout");
    }

    public String getRegisterUrl() {
        return getOrDefault("registerUrl", "http://localhost:8080/hub");
    }

    public String getDevFeatureToggle() {
        return getOrDefault("devFeatureEnabled", "notConfigured");
    }

    /**
     * An admin user can execute APIs for any user in the system. This requires the on-behalf-of header to be set.
     * @return the admin user configured for the system. Only one user is supported.
     */
    public String getAdminUser() {
        return getOrDefault("adminUser", "notConfigured");
    }

    public String getKmsKeyArn() {
        return getOrThrow("kmsKeyArn");
    }

    public String getSkillId() {
        return getOrThrow("skillId");
    }

    public String getEddiSkillId() {
        return getOrThrow("eddiSkillId");
    }

    public String getLibbiSkillId() {
        return getOrThrow("libbiSkillId");
    }

    public String getSchedulerExecutionRoleArn() {
        return getOrThrow("schedulerExecutionRoleArn");
    }

    public String getSchedulerTargetLambdaArn() {
        return getOrThrow("schedulerTargetLambdaArn");
    }

    public String getSqsQueueUrl() {
        return getOrDefault("sqsQueueUrl", "");
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
