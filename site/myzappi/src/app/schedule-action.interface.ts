export interface ScheduleActionComponent {
    getScheduleAction(): any;
    scheduleConfigurationStarted(): void;
    scheduleConfigurationCancelled(): void;
    scheduleConfigurationComplete(): void;
}