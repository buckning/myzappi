export interface RecurringSchedule {
    id?: string;
    zoneId: string;
    recurrence: {
        timeOfDay: string;
        daysOfWeek: number[];
    }
    action: {
        type: string;
        target: string;
        value: string;
    }
}

export interface Schedule {
    id?: string;
    zoneId: string;
    startDateTime: string;
    action: {
        type: string;
        target: string;
        value: string;
    }
}