import { Component, Output, Input, EventEmitter } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface Schedule {

  id?: string;
  zoneId: string;
  startDateTime: string;
  action: {
    type: string;
    value: string;
  }
}

@Component({
  selector: 'app-create-onetime-schedule-panel',
  templateUrl: './create-onetime-schedule-panel.component.html',
  styleUrls: ['./create-onetime-schedule-panel.component.css']
})
export class CreateOnetimeSchedulePanelComponent {
  @Input() public bearerToken: any;
  @Output() public viewListSchedulesScreen = new EventEmitter();
  startDateTime: string = '';
  scheduleType: string = 'setBoostKwh';
  scheduleActionValue: string = '';
  cancelButtonVisible = true;
  saveButtonDisabled = false;

  constructor(private http: HttpClient) { }

  cancel() {
    // TODO set logged-in-content registered = true
    console.log("Cancel clicked");
    this.viewListSchedulesScreen.emit('');
  }

  saveSchedule() {
    this.saveButtonDisabled = true;
    this.cancelButtonVisible = false;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      startDateTime: this.startDateTime,
      action: {
        type: this.scheduleType,
        value: this.transformActionValue()
      }
    };

    var requestBody = JSON.stringify(newSchedule);
    console.log("Creating new schedule: " + requestBody);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers };
    this.http.post('https://api.myzappiunofficial.com/schedules', requestBody, options)
      .subscribe(data => {
        // TODO set logged-in-content registered = true
        console.log("Success saving schedule");
        this.viewListSchedulesScreen.emit('');
        this.cancelButtonVisible = true;
      },
        error => {
          console.log("error saving schedule " + JSON.stringify(requestBody));
          this.cancelButtonVisible = true;
          this.saveButtonDisabled = false;
        });
  }

  transformActionValue(): string {
    if (this.scheduleType === 'setBoostFor') {
      return "PT" + this.scheduleActionValue + "H";
    }
    if (this.scheduleType === 'setEddiBoostFor') {
      return "PT" + this.scheduleActionValue + "M";
    }
    return this.scheduleActionValue;
  }

  onScheduleTypeChange() {
    if (this.scheduleType === "setChargeMode") {
      this.scheduleActionValue = 'ECO_PLUS';
    } else {
      this.scheduleActionValue = '';
    }
  }

  isScheduleValid(): boolean {
    return this.startDateTime !== '' && this.isDateTimeValid() && this.isValidActionValue();
  }

  isValidActionValue(): boolean {
    return this.scheduleActionValue !== '';
  }

  isDateTimeValid(): boolean {
    return new Date() < new Date(this.startDateTime)
  }

  getZoneId(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
}
