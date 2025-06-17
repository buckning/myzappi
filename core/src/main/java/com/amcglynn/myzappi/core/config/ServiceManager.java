package com.amcglynn.myzappi.core.config;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.dal.MyEnergiAccountCredentialsRepository;
import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.TariffService;
import lombok.Getter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceManager {

    private final EncryptionService encryptionService;
    private final CredentialsRepository credentialsRepository;
    private final MyEnergiAccountCredentialsRepository myEnergiAccountCredentialsRepository;
    @Getter
    private final DevicesRepository devicesRepository;
    private MyEnergiService.Builder myEnergiServiceBuilder;
    private LoginService loginService;
    private final TariffService tariffService;
    private final Properties properties;
    private final AmazonDynamoDB amazonDynamoDB;
    @Getter
    private final ScheduleService scheduleService;
    @Getter
    private final ExecutorService executorService;

    public ServiceManager(Properties properties) {
        amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.fromName(properties.getAwsRegion()))
                .build();
        encryptionService = new EncryptionService(properties.getKmsKeyArn());
        credentialsRepository = new CredentialsRepository(amazonDynamoDB);
        devicesRepository = new DevicesRepository(amazonDynamoDB);
        myEnergiAccountCredentialsRepository = new MyEnergiAccountCredentialsRepository(amazonDynamoDB);
        executorService = Executors.newFixedThreadPool(2);

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

    public Properties getProperties() {
        return properties;
    }

    public TariffService getTariffService() {
        return tariffService;
    }

    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = new LoginService(credentialsRepository, devicesRepository,
                    myEnergiAccountCredentialsRepository, encryptionService);
        }
        return loginService;
    }

    public MyEnergiService.Builder getMyEnergiServiceBuilder() {
        if (this.myEnergiServiceBuilder == null) {
            myEnergiServiceBuilder = new MyEnergiService.Builder(getLoginService());
        }
        return this.myEnergiServiceBuilder;
    }
}
