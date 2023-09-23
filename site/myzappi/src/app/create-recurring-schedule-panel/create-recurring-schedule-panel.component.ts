import { Component, EventEmitter, Input, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface Schedule {

  id?: string;
  zoneId: string;
  recurrence: {
    timeOfDay: string;
    daysOfWeek: number[];
  }
  action: {
    type: string;
    value: string;
  }
}

@Component({
  selector: 'app-create-recurring-schedule-panel',
  templateUrl: './create-recurring-schedule-panel.component.html',
  styleUrls: ['./create-recurring-schedule-panel.component.css']
})
export class CreateRecurringSchedulePanelComponent {
  @Input() public bearerToken: any;
  @Output() public viewListSchedulesScreen = new EventEmitter();
  recurringTime: string = '';
  selectedDays: { [key: string]: boolean } = {};
  recurringAction: string = 'setChargeMode';
  recurringValue: string = 'ECO_PLUS';
  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  saveButtonDisabled = false;

  constructor(private http: HttpClient) { }

  toggleDay(day: string) {
    this.selectedDays[day] = !this.selectedDays[day];
  }

  saveSchedule() {
    this.saveButtonDisabled = true;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      recurrence: {
        timeOfDay: this.recurringTime,
        daysOfWeek: this.convertDaysOfWeekToNumbers()
      },
      action: {
        type: this.recurringAction,
        value: this.recurringValue
      }
    };

    var requestBody = JSON.stringify(newSchedule);
    console.log("Creating new schedule: " + requestBody);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers };
    this.http.post('https://api.myzappiunofficial.com/schedule', requestBody, options)
      .subscribe(data => {
        // TODO set logged-in-content registered = true
        console.log("Success saving schedule");
        this.viewListSchedulesScreen.emit('');
      },
        error => {
          console.log("error saving schedule " + JSON.stringify(requestBody));
        });

  }

  cancel() {
    // TODO set logged-in-content registered = true
    console.log("Cancel clicked");
    this.viewListSchedulesScreen.emit('');
  }

  isScheduleValid(): boolean {
    return this.recurringValue !== '' && this.convertDaysOfWeekToNumbers().length > 0 && this.recurringTime !== '';
  }

  convertDaysOfWeekToNumbers(): number[] {
    let selectedDaysArray: number[] = [];
    for (let day in this.selectedDays) {
      if (this.selectedDays[day]) {
        selectedDaysArray.push(this.daysOfWeek.indexOf(day) + 1);
      }
    }
    return selectedDaysArray;
  }

  // get zone ID from browser
  getZoneId(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }

}
