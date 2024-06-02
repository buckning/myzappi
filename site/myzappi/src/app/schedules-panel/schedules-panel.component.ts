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
  @Input() public hubDetails: any;
  // selectedOption: 'one-time' | 'recurring' = 'one-time';
  selectedOption: 'one-time' | 'recurring' = 'recurring';
  createRecurringScheduleVisible = false;
  createOneTimeScheduleVisible = false;
  listSchedulesVisible = false;
  loaded: boolean = false;
  screenWidth: number = 1024;
  recurringScheduleRows: any[] = [];
  recurringScheduleEddiRows: any[] = [];
  recurringScheduleLibbiRows: any[] = [];
  oneTimeScheduleRows: any[] = [];
  oneTimeScheduleEddiRows: any[] = [];
  oneTimeScheduleLibbiRows: any[] = [];
  tanks: any[] = [];
  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  chargeModeMapping: { [key: string]: string } = {
    'ECO_PLUS': 'Eco+',
    'ECO': 'Eco',
    'FAST': 'Fast',
    'STOP': 'Stop'
  };

  eddiModeMapping: { [key: string]: string } = {
    'STOPPED': 'Stopped',
    'NORMAL': 'Normal'
  };

  libbiChargeFromGridMapping: { [key: string]: string } = {
    'true': 'On',
    'false': 'Off'
  };

  scheduleTypeMapping: { [key: string]: string } = {
    'setBoostKwh': 'Boosting kWh',
    'setBoostFor': 'Boost for hours',
    'setBoostUntil': 'Boosting until time',
    'setChargeMode': 'Set charge mode',
    'setEddiMode': 'Set Eddi mode',
    'setEddiBoostFor': 'Boost Eddi',
    'setLibbiEnabled': 'Enable Libbi',
    'setLibbiChargeFromGrid': 'Set charge from grid',
    'setLibbiChargeTarget': 'Set charge target'
  };

  scheduleTypeDeviceClassMapping: { [key: string] : string } = {
    'setBoostKwh': 'ZAPPI',
    'setBoostFor': 'ZAPPI',
    'setBoostUntil': 'ZAPPI',
    'setChargeMode': 'ZAPPI',
    'setEddiMode': 'EDDI',
    'setEddiBoostFor': 'EDDI',
    'setLibbiEnabled': 'LIBBI',
    'setLibbiChargeFromGrid': 'LIBBI',
    'setLibbiChargeTarget': 'LIBBI'
  };

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
      for (let device of Object.values(this.hubDetails.devices) as any[]) {
        if (device.deviceClass === 'EDDI') {
          this.tanks = [device.tank1Name, device.tank2Name];
        }
      }
    // make rest call to schedules api
    this.screenWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
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
    if (this.screenWidth <= 768) {
      return row.recurrence.daysOfWeek.map((day: number) => this.daysOfWeek[day - 1].slice(0, 3)).join(',');  
    }
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
    let options = { headers: headers, withCredentials: true };
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

  convertEddiMode(input: string): string {
    if (input in this.eddiModeMapping) {
      return this.eddiModeMapping[input];
    }
    
    return input;
  }

  convertLibbiChargeFromGrid(input: string): string {
    if (input in this.libbiChargeFromGridMapping) {
      return this.libbiChargeFromGridMapping[input];
    }
    
    return input;
  }

  convertScheduleType(input: any): string {
    if (input.action.type in this.scheduleTypeMapping) {
      if (input.action.type === 'setEddiBoostFor') {
        var tokens = input.action.value.split(";");
        var tank = "";
        if (tokens.length > 1) {
          tank = tokens[1].split("=")[1];
        }
  
        return this.scheduleTypeMapping[input.action.type] + " (" + this.tanks[parseInt(tank) - 1] + ")";
      }
      return this.scheduleTypeMapping[input.action.type];
    }
    
    return input;
  }

  convertOneTimeScheduleValue(input: any) {
    if (input.action.type === 'setBoostKwh') {
      return input.action.value + "kWh";
    }
    if (input.action.type === 'setBoostFor') {
      return this.convertDuration(input.action.value);
    }
    if (input.action.type === 'setChargeMode') {
      return this.convertChargeMode(input.action.value);
    }
    if (input.action.type === 'setEddiMode') {
      return this.convertEddiMode(input.action.value);
    }
    if (input.action.type === 'setLibbiChargeFromGrid') {
      return this.convertLibbiChargeFromGrid(input.action.value);
    }
    if (input.action.type === 'setEddiBoostFor') {
      return this.convertDuration(input.action.value);
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
        parts.push(`${minutes} mins`);
      }
    
      return parts.join(' ');
    }
    
    var tokens = isoDuration.split(";");
    
    return formatDuration(tokens[0]);
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
    let options = { headers: headers, withCredentials: true };
    this.http.get<Schedules>('https://api.myzappiunofficial.com/schedules', options)
      .subscribe(data => {
        this.recurringScheduleRows = [];
        this.recurringScheduleEddiRows = [];
        this.recurringScheduleLibbiRows = [];

        this.oneTimeScheduleRows = [];
        this.oneTimeScheduleEddiRows = [];
        this.oneTimeScheduleLibbiRows = [];

        data.schedules.forEach(schedule => {
          var deviceClass = this.scheduleTypeDeviceClassMapping[schedule.action.type];

          var isRecurring = schedule.recurrence !== undefined && schedule.recurrence !== null;

          console.log("Found device class " + deviceClass + " for " + schedule.action.type + " recurring = " + isRecurring);

          if (deviceClass === 'ZAPPI') {
            if (isRecurring) {
              this.recurringScheduleRows.push(schedule);
            } else {
              this.oneTimeScheduleRows.push(schedule);
            }
          }
          if (deviceClass === 'EDDI') {
            if (isRecurring) {
              this.recurringScheduleEddiRows.push(schedule);
            } else {
              this.oneTimeScheduleEddiRows.push(schedule);
            }
          }
          if (deviceClass === 'LIBBI') {
            if (isRecurring) {
              this.recurringScheduleLibbiRows.push(schedule);
            } else {
              this.oneTimeScheduleLibbiRows.push(schedule);
            }
          }
        });
        
        this.loaded = true;
        this.listSchedulesVisible = true;
      },
        error => {
          console.log("failed to get schedules " + error.status);
          this.loaded = true;
        });
  }
}
