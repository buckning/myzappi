package com.amcglynn.myzappi.core.config;

import com.amcglynn.myzappi.core.dal.AutomationProcessorLockRepository;
import com.amcglynn.myzappi.core.dal.AutomationRepository;
import com.amcglynn.myzappi.core.dal.AutomationStateRepository;
import com.amcglynn.myzappi.core.dal.CredentialsRepository;
import com.amcglynn.myzappi.core.dal.DeviceStateReconcileRequestsRepository;
import com.amcglynn.myzappi.core.dal.DevicesRepository;
import com.amcglynn.myzappi.core.dal.MyEnergiAccountCredentialsRepository;
import com.amcglynn.myzappi.core.dal.ScheduleDetailsRepository;
import com.amcglynn.myzappi.core.dal.TariffRepository;
import com.amcglynn.myzappi.core.dal.UserScheduleRepository;
import com.amcglynn.myzappi.core.service.EncryptionService;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.core.service.LoginService;
import com.amcglynn.myzappi.core.service.MyEnergiService;
import com.amcglynn.myzappi.core.service.ScheduleService;
import com.amcglynn.myzappi.core.service.SqsSenderService;
import com.amcglynn.myzappi.core.service.StateReconcilerService;
import com.amcglynn.myzappi.core.service.TariffService;
import com.amcglynn.myzappi.core.service.automation.AutomationActionExecutor;
import com.amcglynn.myzappi.core.service.automation.AutomationOptionsService;
import com.amcglynn.myzappi.core.service.automation.AutomationProcessorService;
import com.amcglynn.myzappi.core.service.automation.AutomationService;
import com.amcglynn.myzappi.core.service.automation.AutomationValidator;
import com.amcglynn.myzappi.core.service.automation.PredicateEvaluator;
import com.amcglynn.myzappi.core.service.reconciler.DeviceStateReconciler;
import com.amcglynn.myzappi.core.service.reconciler.EddiModeReconciler;
import com.amcglynn.myzappi.core.service.reconciler.ReconcilerRegistry;
import com.amcglynn.myzappi.core.service.reconciler.ZappiChargeReconciler;

import java.util.List;

import com.amcglynn.myzappi.core.service.reconciler.ZappiMinimumGreenLevelReconciler;
import lombok.Getter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceManager {

    @Getter
    private final EncryptionService encryptionService;
    private final CredentialsRepository credentialsRepository;
    private final MyEnergiAccountCredentialsRepository myEnergiAccountCredentialsRepository;
    @Getter
    private final DevicesRepository devicesRepository;
    @Getter
    private final DeviceStateReconcileRequestsRepository deviceStateReconcileRequestsRepository;
    private MyEnergiService.Builder myEnergiServiceBuilder;
    private LoginService loginService;
    @Getter
    private final TariffService tariffService;
    @Getter
    private final Properties properties;
    @Getter
    private final DynamoDbClient amazonDynamoDB;
    @Getter
    private final ScheduleService scheduleService;
    @Getter
    private final ExecutorService executorService;
    @Getter
    private final StateReconcilerService stateReconciliationService;
    @Getter
    private final AutomationRepository automationRepository;
    @Getter
    private final AutomationStateRepository automationStateRepository;
    @Getter
    private final AutomationProcessorLockRepository automationProcessorLockRepository;
    @Getter
    private final AutomationService automationService;
    @Getter
    private final AutomationProcessorService automationProcessorService;

    public ServiceManager(Properties properties) {
        amazonDynamoDB = DynamoDbClient.builder()
                .region(Region.of(properties.getAwsRegion()))
                .build();
        automationRepository = new AutomationRepository(amazonDynamoDB);
        automationStateRepository = new AutomationStateRepository(amazonDynamoDB);
        automationProcessorLockRepository = new AutomationProcessorLockRepository(amazonDynamoDB);
        deviceStateReconcileRequestsRepository = new DeviceStateReconcileRequestsRepository(amazonDynamoDB);
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

        var sqsSenderService = new SqsSenderService(getProperties());
        List<DeviceStateReconciler<?>> reconcilers = List.of(
                new ZappiChargeReconciler(sqsSenderService),
                new ZappiMinimumGreenLevelReconciler(sqsSenderService),
                new EddiModeReconciler(sqsSenderService)
        );
        var reconcilerRegistry = new ReconcilerRegistry(reconcilers);

        this.stateReconciliationService = new StateReconcilerService(
                reconcilerRegistry,
                getDeviceStateReconcileRequestsRepository(),
                sqsSenderService,
                getMyEnergiServiceBuilder());
        this.automationService = new AutomationService(
                automationRepository,
                automationStateRepository,
                new AutomationValidator(getLoginService()),
                new AutomationOptionsService(),
                new Clock());
        var automationActionExecutor = new AutomationActionExecutor(getStateReconciliationService());
        this.automationProcessorService = new AutomationProcessorService(
                automationStateRepository,
                getMyEnergiServiceBuilder(),
                new PredicateEvaluator(),
                automationActionExecutor,
                new Clock());
    }

    public String getSchedulerExecutionRoleArn() {
        return properties.getSchedulerExecutionRoleArn();
    }

    public String getSchedulerTargetLambdaArn() {
        return properties.getSchedulerTargetLambdaArn();
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
