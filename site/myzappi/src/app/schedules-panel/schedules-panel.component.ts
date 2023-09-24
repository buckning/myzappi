import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface Schedules {
  schedules: {

    id?: string;
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
  selectedOption: 'one-time' | 'recurring' | 'none' = 'none';
  loaded: boolean = false;
  scheduleRows: any[] = [];
  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  chargeModeMapping: { [key: string]: string } = {
    'ECO_PLUS': 'Eco+',
    'ECO': 'Eco',
    'FAST': 'Fast',
    'STOP': 'Stop'
  };

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    // make rest call to schedules api
    this.readSchedules();
  }

  createRecurringSchedule() {
    this.selectedOption = 'recurring';
  }

  viewListSchedulesScreen() {
    this.selectedOption = 'none';
    this.loaded = false;
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

  readSchedules() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers };
    this.http.get<Schedules>('https://api.myzappiunofficial.com/schedules', options)
      .subscribe(data => {
        this.scheduleRows = data.schedules.filter(schedule => 
          schedule.recurrence !== undefined && schedule.recurrence !== null
        );
        
        this.loaded = true;
      },
        error => {
          console.log("failed to get schedules " + error.status);
          this.loaded = true;
        });
  }
}
