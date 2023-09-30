import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface Schedules {
  schedules: {

    id?: string;
    startDateTime: string;
    zoneId: string;
    recurrence?: {
      timeOfDay: string;
      daysOfWeek: number[];
    }
    action: {
      type: string;
      value: string;
    }
  }[];
}

@Component({
  selector: 'app-schedules-panel',
  templateUrl: './schedules-panel.component.html',
  styleUrls: ['./schedules-panel.component.css']
})
export class SchedulesPanelComponent {
  @Input() public bearerToken: any;
  // selectedOption: 'one-time' | 'recurring' = 'one-time';
  selectedOption: 'one-time' | 'recurring' = 'recurring';
  createRecurringScheduleVisible = false;
  createOneTimeScheduleVisible = false;
  listSchedulesVisible = false;
  loaded: boolean = false;
  recurringScheduleRows: any[] = [];
  oneTimeScheduleRows: any[] = [];
  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  chargeModeMapping: { [key: string]: string } = {
    'ECO_PLUS': 'Eco+',
    'ECO': 'Eco',
    'FAST': 'Fast',
    'STOP': 'Stop'
  };

  scheduleTypeMapping: { [key: string]: string } = {
    'setBoostKwh': 'Boosting kilowatt hours',
    'setBoostFor': 'Boost for duration',
    'setBoostUntil': 'Boosting until time',
    'setChargeMode': 'Set charge mode'
  };

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    // make rest call to schedules api
    this.readSchedules();
  }

  createSchedule() {
    if (this.selectedOption === 'recurring') {
      this.createRecurringScheduleVisible = true;
      this.listSchedulesVisible = false;
    } else {
      this.createOneTimeScheduleVisible = true;
      this.listSchedulesVisible = false;
    }
  }

  viewListSchedulesScreen() {
    this.loaded = false;
    this.createRecurringScheduleVisible = false;
    this.createOneTimeScheduleVisible = false;
    this.readSchedules();
  }

  calculateDisplayString(row: any): string {
    return row.recurrence.daysOfWeek.map((day: number) => this.daysOfWeek[day - 1].slice(0, 3)).join(', ');
  }

  deleteSchedule(row: any) {
    let id = row.id;
    this.loaded = false;
    console.log("Deleting schedule with id: " + id);
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers };
    this.http.delete('https://api.myzappiunofficial.com/schedules/' + id, options)
      .subscribe(data => {
        console.log("Deleted schedule with id: " + id);
        this.readSchedules();
      },
        error => {
          console.log("failed to delete schedule " + error.status);
        });
  }

  convertChargeMode(input: string): string {
    if (input in this.chargeModeMapping) {
      return this.chargeModeMapping[input];
    }
    
    return input;
  }

  convertScheduleType(input: string): string {
    if (input in this.scheduleTypeMapping) {
      return this.scheduleTypeMapping[input];
    }
    
    return input;
  }

  convertOneTimeScheduleValue(input: any) {
    if (input.action.type === 'setBoostKwh') {
      return input.action.value + " kWh";
    }
    if (input.action.type === 'setBoostFor') {
      return this.convertDuration(input.action.value);
    }
    if (input.action.type === 'setChargeMode') {
      return this.convertChargeMode(input.action.value);
    }
    return input.action.value;
  }

  convertDuration(isoDuration: string) {
    function formatDuration(isoDuration: string): string {
      const durationRegex = /^PT(?:(\d+)H)?(?:(\d+)M)?$/;
      const match = isoDuration.match(durationRegex);
    
      if (!match) {
        return 'Invalid duration';
      }
    
      const [, hours, minutes] = match;
      const parts = [];
    
      if (hours) {
        parts.push(`${hours} hours`);
      }
    
      if (minutes) {
        parts.push(`${minutes} minutes`);
      }
    
      return parts.join(' ');
    }
    
    
    return formatDuration(isoDuration);
  }

  convertDateTime(input: string): string {
    return input.replace("T", ' ');
  }

  buildOneTimeScheduleString(input: any) {
    return this.convertScheduleType(input.action.type);
  }

  readSchedules() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers };
    this.http.get<Schedules>('https://api.myzappiunofficial.com/schedules', options)
      .subscribe(data => {
        this.recurringScheduleRows = data.schedules.filter(schedule => 
          schedule.recurrence !== undefined && schedule.recurrence !== null
        );

        this.oneTimeScheduleRows = data.schedules.filter(schedule => 
          schedule.startDateTime !== undefined && schedule.startDateTime !== null
        );
        
        this.loaded = true;
        this.listSchedulesVisible = true;
      },
        error => {
          console.log("failed to get schedules " + error.status);
          this.loaded = true;
        });
  }
}
