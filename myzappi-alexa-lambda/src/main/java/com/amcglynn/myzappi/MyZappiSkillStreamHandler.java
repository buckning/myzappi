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
import com.amcglynn.myzappi.handlers.GetChargeModeHandler;
import com.amcglynn.myzappi.handlers.GetChargeRateHandler;
import com.amcglynn.myzappi.handlers.GetEnergyCostHandler;
import com.amcglynn.myzappi.handlers.GetEnergyUsageHandler;
import com.amcglynn.myzappi.handlers.GetLibbiChargeFromGridEnabledHandler;
import com.amcglynn.myzappi.handlers.GetLibbiChargeTargetHandler;
import com.amcglynn.myzappi.handlers.GetLibbiEnabledHandler;
import com.amcglynn.myzappi.handlers.GetLibbiStateOfChargeHandler;
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
import com.amcglynn.myzappi.handlers.SetLibbiChargeFromGridDisabledHandler;
import com.amcglynn.myzappi.handlers.SetLibbiChargeFromGridEnabledHandler;
import com.amcglynn.myzappi.handlers.SetLibbiChargeTargetHandler;
import com.amcglynn.myzappi.handlers.SetLibbiDisabledHandler;
import com.amcglynn.myzappi.handlers.SetLibbiEnabledHandler;
import com.amcglynn.myzappi.handlers.SetReminderHandler;
import com.amcglynn.myzappi.handlers.StartBoostHandler;
import com.amcglynn.myzappi.handlers.StartSmartBoostHandler;
import com.amcglynn.myzappi.handlers.StatusSummaryHandler;
import com.amcglynn.myzappi.handlers.StopBoostHandler;
import com.amcglynn.myzappi.handlers.StopEddiBoostHandler;
import com.amcglynn.myzappi.handlers.UnlockZappiHandler;
import com.amcglynn.myzappi.interceptors.ZappiServiceInjectorInterceptor;
import com.amcglynn.myzappi.interceptors.ZoneIdInjectorInterceptor;
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
                .addRequestInterceptor(new ZappiServiceInjectorInterceptor(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestInterceptor(new ZoneIdInjectorInterceptor(userZoneResolver))
                .addRequestHandler(new LaunchHandler())
                .addRequestHandler(new HelpHandler())
                .addRequestHandler(new FallbackHandler())
                .addRequestHandler(new UnlockZappiHandler())
                .addRequestHandler(new StatusSummaryHandler())
                .addRequestHandler(new GetSolarReportHandler())
                .addRequestHandler(new StartBoostHandler())
                .addRequestHandler(new StopBoostHandler())
                .addRequestHandler(new StartSmartBoostHandler())
                .addRequestHandler(new GetPlugStatusHandler())
                .addRequestHandler(new GetChargeModeHandler())
                .addRequestHandler(new GetEnergyUsageHandler())
                .addRequestHandler(new GoGreenHandler())
                .addRequestHandler(new GetChargeRateHandler())
                .addRequestHandler(new ChargeMyCarHandler())
                .addRequestHandler(new GetEnergyCostHandler(serviceManager.getTariffService()))
                .addRequestHandler(new SetChargeModeHandler(serviceManager.getExecutorService()))
                .addRequestHandler(new SetEddiModeToNormalHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new SetEddiModeToStoppedHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new BoostEddiHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new StopEddiBoostHandler(serviceManager.getMyEnergiServiceBuilder(), userIdResolverFactory))
                .addRequestHandler(new SetReminderHandler(reminderServiceFactory, userZoneResolver, userIdResolverFactory,
                        new AlexaToLwaLookUpRepository(serviceManager.getAmazonDynamoDB()),
                        new SchedulerService(SchedulerClient.builder().region(Region.EU_WEST_1).build(),
                                serviceManager.getSchedulerExecutionRoleArn(), serviceManager.getSchedulerTargetLambdaArn())))
                .addRequestHandler(new MessageReceivedHandler(reminderServiceFactory, serviceManager.getMyEnergiServiceBuilder(),
                        new AlexaToLwaLookUpRepository(serviceManager.getAmazonDynamoDB())))
                .addRequestHandler(new QuitHandler())
                .addRequestHandler(new ScheduleJobHandler(serviceManager.getScheduleService(), userIdResolverFactory, userZoneResolver, new Clock()))
                .addRequestHandler(new GetLibbiChargeFromGridEnabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new GetLibbiChargeTargetHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new GetLibbiEnabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new GetLibbiStateOfChargeHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new SetLibbiChargeFromGridEnabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new SetLibbiChargeFromGridDisabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new SetLibbiChargeTargetHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new SetLibbiEnabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addRequestHandler(new SetLibbiDisabledHandler(serviceManager.getMyEnergiServiceBuilder(),
                        userIdResolverFactory))
                .addExceptionHandler(new MyZappiExceptionHandler())
                .build());
    }
}
