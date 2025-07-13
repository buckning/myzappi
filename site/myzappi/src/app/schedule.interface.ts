// export interface RecurringSchedule {
//     id?: string;
//     zoneId: string;
//     recurrence: {
//         timeOfDay: string;
//         daysOfWeek: number[];
//     }
//     action: {
//         type: string;
//         target: string;
//         value: string;
//     }
// }

export interface Schedule {
    id?: string;
    startDateTime?: string;
    zoneId: string;
    active?: boolean;
    recurrence?: {
        timeOfDay: string;
        daysOfWeek: number[];
    }
    action: {
        type: string;
        target: string;
        value: string;
    }
}

export interface Schedules {
    schedules: Schedule[];
}
