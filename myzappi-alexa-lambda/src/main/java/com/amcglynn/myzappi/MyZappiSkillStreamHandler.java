package com.amcglynn.myzappi;

import com.amazon.ask.SkillStreamHandler;
import com.amazon.ask.Skills;
import com.amcglynn.lwa.LwaClient;
import com.amcglynn.myzappi.core.config.Properties;
import com.amcglynn.myzappi.core.config.ServiceManager;
import com.amcglynn.myzappi.core.dal.AlexaToLwaLookUpRepository;
import com.amcglynn.myzappi.core.service.Clock;
import com.amcglynn.myzappi.handlers.BoostEddiHandler;
import com.amcglynn.myzappi.handlers.ChargeMyCarHandler;
import com.amcglynn.myzappi.handlers.FallbackHandler;
import com.amcglynn.myzappi.handlers.GetEnergyCostHandler;
import com.amcglynn.myzappi.handlers.GetEnergyUsageHandler;
import com.amcglynn.myzappi.handlers.GetPlugStatusHandler;
import com.amcglynn.myzappi.handlers.GetSolarReportHandler;
import com.amcglynn.myzappi.handlers.GoGreenHandler;
import com.amcglynn.myzappi.handlers.HelpHandler;
import com.amcglynn.myzappi.handlers.LaunchHandler;
import com.amcglynn.myzappi.handlers.MessageReceivedHandler;
import com.amcglynn.myzappi.handlers.MyZappiExceptionHandler;
import com.amcglynn.myzappi.handlers.QuitHandler;
import com.amcglynn.myzappi.handlers.ScheduleJobHandler;
import com.amcglynn.myzappi.handlers.SetChargeModeHandler;
import com.amcglynn.myzappi.handlers.SetEddiModeToNormalHandler;
import com.amcglynn.myzappi.handlers.SetEddiModeToStoppedHandler;
import com.amcglynn.myzappi.handlers.SetReminderHandler;
import com.amcglynn.myzappi.handlers.StartBoostHandler;
import com.amcglynn.myzappi.handlers.StatusSummaryHandler;
import com.amcglynn.myzappi.handlers.StopBoostHandler;
import com.amcglynn.myzappi.handlers.StopEddiBoostHandler;
import com.amcglynn.myzappi.handlers.UnlockZappiHandler;
import com.amcglynn.myzappi.service.ReminderServiceFactory;
import com.amcglynn.myzappi.service.SchedulerService;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

public class MyZappiSkillStreamHandler extends SkillStreamHandler {

    public MyZappiSkillStreamHandler() {
        this(new ServiceManager(new Properties()), new UserIdResolverFactory(new LwaClient()),
                new UserZoneResolver(new LwaClient()),
                new ReminderServiceFactory(new SchedulerService(SchedulerClient.builder().region(Region.EU_WEST_1).build(),
                        new Properties().getSchedulerExecutionRoleArn(), new Properties().getSchedulerTargetLambdaArn())));
    }

    public MyZappiSkillStreamHandler(ServiceManager serviceManager, UserIdResolverFactory userIdResolverFactory,
                                     UserZoneResolver userZoneResolver, ReminderServiceFactory reminderServiceFactory) {
        super(Skills.standard()
                .withSkillId(serviceManager.getSkillId())
                .addRequestHandler(new LaunchHandler())
                .addRequestHandler(new HelpHandler())
                .addRequestHandler(new FallbackHandler())
                .addRequestHandler(new UnlockZappiHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new StatusSummaryHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GetSolarReportHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new StartBoostHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory, userZoneResolver))
                .addRequestHandler(new StopBoostHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GetPlugStatusHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GetEnergyUsageHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory, userZoneResolver))
                .addRequestHandler(new GetEnergyCostHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory, userZoneResolver, serviceManager.getTariffService()))
                .addRequestHandler(new SetChargeModeHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new SetEddiModeToNormalHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new SetEddiModeToStoppedHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new BoostEddiHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new StopEddiBoostHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new GoGreenHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new ChargeMyCarHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new SetReminderHandler(reminderServiceFactory, userZoneResolver, userIdResolverFactory,
                        new AlexaToLwaLookUpRepository(serviceManager.getAmazonDynamoDB()),
                        new SchedulerService(SchedulerClient.builder().region(Region.EU_WEST_1).build(),
                                serviceManager.getSchedulerExecutionRoleArn(), serviceManager.getSchedulerTargetLambdaArn())))
                .addRequestHandler(new MessageReceivedHandler(reminderServiceFactory, serviceManager.getMyEnergiServiceBuilder(),
                        new AlexaToLwaLookUpRepository(serviceManager.getAmazonDynamoDB())))
                .addRequestHandler(new QuitHandler())
                .addRequestHandler(new ScheduleJobHandler(serviceManager.getScheduleService(), userIdResolverFactory, userZoneResolver, new Clock()))
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}
