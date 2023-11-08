package com.amcglynn.myzappi.core.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.ZappiService;
import lombok.Getter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

public class ServiceManager {

    private final EncryptionService encryptionService;
    private final CredentialsRepository credentialsRepository;
    @Getter
    private final DevicesRepository devicesRepository;
    private ZappiService.Builder zappiServiceBuilder;
    private LoginService loginService;
    private final TariffService tariffService;
    private final Properties properties;
    private final AmazonDynamoDB amazonDynamoDB;
    @Getter
    private final ScheduleService scheduleService;

    public ServiceManager(Properties properties) {
        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.fromName(properties.getAwsRegion()))
                .build();
        encryptionService = new EncryptionService(properties.getKmsKeyArn());
        credentialsRepository = new CredentialsRepository(amazonDynamoDB);
        devicesRepository = new DevicesRepository(amazonDynamoDB);

        tariffService = new TariffService(new TariffRepository(amazonDynamoDB));
        this.properties = properties;
        var schedulerClient = SchedulerClient.builder().region(Region.EU_WEST_1).build();
        this.scheduleService = new ScheduleService(new UserScheduleRepository(amazonDynamoDB),
                new ScheduleDetailsRepository(amazonDynamoDB),
                schedulerClient,
                getLoginService(),
                properties.getSchedulerExecutionRoleArn(),
                properties.getSchedulerTargetLambdaArn());
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

    public String getSchedulerExecutionRoleArn() {
        return properties.getSchedulerExecutionRoleArn();
    }

    public String getSchedulerTargetLambdaArn() {
        return properties.getSchedulerTargetLambdaArn();
    }

    public TariffService getTariffService() {
        return tariffService;
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginService(credentialsRepository, devicesRepository, encryptionService);
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
