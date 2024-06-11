import { Component, EventEmitter, Input, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Device } from '../device.interface';
import { RecurringSchedule } from '../schedule.interface';

@Component({
  selector: 'app-create-recurring-schedule-panel',
  templateUrl: './create-recurring-schedule-panel.component.html',
  styleUrls: ['./create-recurring-schedule-panel.component.css']
})
export class CreateRecurringSchedulePanelComponent {
  @Input() public bearerToken: any;
  @Input() public eddiEnabled: any;
  @Input() public hubDetails: Device[] = [];
  @Output() public viewListSchedulesScreen = new EventEmitter();
  recurringTime: string = '';
  selectedDays: { [key: string]: boolean } = {};
  recurringAction: string = 'setChargeMode';
  recurringValue: string = 'ECO_PLUS';
  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  saveButtonDisabled = false;
  cancelButtonVisible = true;
  targetDeviceClass: string = "unknown";
  targetSerialNumber: string = "unknown";
  deviceTypes = new Set<string>();

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.hubDetails.forEach(device => {
      if (!this.deviceTypes.has(device.deviceClass)) {
        this.deviceTypes.add(device.deviceClass);
      }
    });
    this.deviceSelected(this.hubDetails[0].deviceClass);
    this.targetDeviceClass = this.hubDetails[0].deviceClass.toLowerCase();
    this.targetSerialNumber = this.hubDetails[0].serialNumber;
  }

  deviceSelected(deviceType: string) {
    if (deviceType === "EDDI") {
      this.eddiSelected();
    } else if (deviceType === "ZAPPI") {
      this.zappiSelected();
    }
  }

  hasMultipleDeviceClasses() : boolean {
    return this.deviceTypes.size > 1;
  }

  getSerialNumbers() {
    return this.getDevices(this.targetDeviceClass).map(device => device.serialNumber);
  }

  getDevices(targetDeviceClass: string) {
    return this.hubDetails.filter(device => device.deviceClass.toLowerCase() === targetDeviceClass.toLowerCase());
  }

  hasDevice(deviceClass: string) {
    return this.deviceTypes.has(deviceClass);
  }

  toggleDay(day: string) {
    this.selectedDays[day] = !this.selectedDays[day];
  }

  eddiSelected() {
    this.recurringValue = "NORMAL";
    this.targetDeviceClass = "eddi";
    this.targetSerialNumber = this.getSerialNumbers()[0];
  }

  zappiSelected() {
    this.recurringValue = "ECO_PLUS";
    this.targetDeviceClass = "zappi";
    this.targetSerialNumber = this.getSerialNumbers()[0];
  }

  saveSchedule() {
    this.saveButtonDisabled = true;
    this.cancelButtonVisible = false;
    let newSchedule: RecurringSchedule = {
      zoneId: this.getZoneId(),
      recurrence: {
        timeOfDay: this.recurringTime,
        daysOfWeek: this.convertDaysOfWeekToNumbers()
      },
      action: {
        type: this.getRecurringAction(),
        target: this.targetSerialNumber,
        value: this.recurringValue
      }
    };

    var requestBody = JSON.stringify(newSchedule);
    console.log("Creating new schedule: " + requestBody);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
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
        });

  }

  getRecurringAction(): string {
    return this.targetDeviceClass === 'zappi' ? this.recurringAction : 'setEddiMode';
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
