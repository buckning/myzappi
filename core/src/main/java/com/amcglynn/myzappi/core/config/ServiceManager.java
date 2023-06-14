package com.amcglynn.myzappi.core.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.ZappiService;

public class ServiceManager {

    private EncryptionService encryptionService;
    private LoginCodeRepository loginCodeRepository;
    private CredentialsRepository credentialsRepository;
    private ZappiService.Builder zappiServiceBuilder;
    private LoginService loginService;
    private Properties properties;

    public ServiceManager(Properties properties) {
        var amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.fromName(properties.getAwsRegion()))
                .build();
        encryptionService = new EncryptionService(properties.getKmsKeyArn());
        credentialsRepository = new CredentialsRepository(amazonDynamoDB);
        loginCodeRepository = new LoginCodeRepository(amazonDynamoDB);
        this.properties = properties;
    }

    public String getSkillId() {
        return properties.getSkillId();
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginService(credentialsRepository, loginCodeRepository, encryptionService);
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
