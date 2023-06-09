package com.amcglynn.myzappi.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.LoginCodeRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;

public class ServiceManager {

    private EncryptionService encryptionService;
    private LoginCodeRepository loginCodeRepository;
    private CredentialsRepository credentialsRepository;
    private LoginService loginService;

    public ServiceManager(Properties properties) {
        var amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.fromName(properties.getAwsRegion()))
                .build();
        encryptionService = new EncryptionService(properties.getKmsKeyArn());
        credentialsRepository = new CredentialsRepository(amazonDynamoDB);
        loginCodeRepository = new LoginCodeRepository(amazonDynamoDB);
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginService(credentialsRepository, loginCodeRepository, encryptionService);
        }
        return loginService;
    }
}
