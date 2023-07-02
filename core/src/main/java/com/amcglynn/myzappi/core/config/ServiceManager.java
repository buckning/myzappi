package com.amcglynn.myzappi.core.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.ZappiService;

public class ServiceManager {

    private final EncryptionService encryptionService;
    private final CredentialsRepository credentialsRepository;
    private ZappiService.Builder zappiServiceBuilder;
    private LoginService loginService;
    private final Properties properties;
    private final AmazonDynamoDB amazonDynamoDB;

    public ServiceManager(Properties properties) {
        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.fromName(properties.getAwsRegion()))
                .build();
        encryptionService = new EncryptionService(properties.getKmsKeyArn());
        credentialsRepository = new CredentialsRepository(amazonDynamoDB);
        this.properties = properties;
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public AmazonDynamoDB getAmazonDynamoDB() {
        return amazonDynamoDB;
    }

    public String getSkillId() {
        return properties.getSkillId();
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginService(credentialsRepository, encryptionService);
        }
        return loginService;
    }

    public ZappiService.Builder getZappiServiceBuilder() {
        if (this.zappiServiceBuilder == null) {
            zappiServiceBuilder = new ZappiService.Builder(getLoginService(), encryptionService);
        }
        return this.zappiServiceBuilder;
    }
}
