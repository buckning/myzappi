import { Component, Output, Input, EventEmitter } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Device } from '../device.interface';
import { Schedule } from '../schedule.interface';

@Component({
  selector: 'app-create-onetime-schedule-panel',
  templateUrl: './create-onetime-schedule-panel.component.html',
  styleUrls: ['./create-onetime-schedule-panel.component.css']
})
export class CreateOnetimeSchedulePanelComponent {
  @Input() public bearerToken: any;
  @Input() public hubDetails: Device[] = [];
  @Output() public viewListSchedulesScreen = new EventEmitter();

  deviceTypes = new Set<string>();
  startDateTime: string = '';
  scheduleType: string = 'setBoostKwh';
  scheduleActionValue: string = '';
  scheduleActionTankValue: string = '';
  selectedTank: any;
  cancelButtonVisible = true;
  saveButtonDisabled = false;
  eddiTanks: any[] = [];
  targetDeviceClass: string = "unknown";
  targetSerialNumber: string = "unknown";

  zappiOptions: { value: string, label: string }[] = [
    { value: 'setBoostKwh', label: 'Boost until kilowatt hours reached' },
    { value: 'setBoostFor', label: 'Boost for duration (hours)' },
    { value: 'setBoostUntil', label: 'Boost until time' },
    { value: 'setChargeMode', label: 'Set charge mode' }
  ];

  options: { value: string, label: string }[] = this.zappiOptions;

  eddiOptions: { value: string, label: string }[] = [
    { value: 'setEddiMode', label: 'Set Eddi mode' },
    { value: 'setEddiBoostFor', label: 'Boost Eddi for duration (minutes)' }
  ];

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

  hasMultipleDeviceClasses() : boolean {
    return this.deviceTypes.size > 1;
  }

  hasDevice(deviceClass: string) {
    return this.deviceTypes.has(deviceClass);
  }

  getSerialNumbers() {
    return this.getDevices(this.targetDeviceClass).map(device => device.serialNumber);
  }

  getDevices(targetDeviceClass: string) {
    return this.hubDetails.filter(device => device.deviceClass.toLowerCase() === targetDeviceClass.toLowerCase());
  }

  cancel() {
    // TODO set logged-in-content registered = true
    console.log("Cancel clicked");
    this.viewListSchedulesScreen.emit('');
  }

  deviceSelected(deviceType: string) {
    if (deviceType === "EDDI") {
      this.eddiSelected();
    } else if (deviceType === "ZAPPI") {
      this.zappiSelected();
    }
  }

  eddiSelected() {
    this.scheduleType = 'setEddiMode';

    for (let device of Object.values(this.hubDetails) as any[]) {
      if (device.deviceClass === 'EDDI') {
        this.eddiTanks = [device.tank1Name, device.tank2Name];
        this.selectedTank = this.eddiTanks[0];
      }
    }

    this.options = this.eddiOptions;
    this.targetDeviceClass = "eddi";
    this.targetSerialNumber = this.getSerialNumbers()[0];
  }

  zappiSelected() {
    this.scheduleType = 'setBoostKwh';
    this.options = this.zappiOptions;
    this.targetDeviceClass = "zappi";
    this.targetSerialNumber = this.getSerialNumbers()[0];
  }

  saveSchedule() {
    this.saveButtonDisabled = true;
    this.cancelButtonVisible = false;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      startDateTime: this.startDateTime,
      action: {
        type: this.scheduleType,
        target: this.targetSerialNumber,
        value: this.transformActionValue()
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
          this.saveButtonDisabled = false;
        });
  }

  transformActionValue(): string {
    if (this.scheduleType === 'setBoostFor') {
      return "PT" + this.scheduleActionValue + "H";
    }
    if (this.scheduleType === 'setEddiBoostFor') {
      console.log("selected tank = " + this.selectedTank);
      for (const [index, tank] of this.eddiTanks.entries()) {
        if (tank === this.selectedTank) {
            console.log('Index:', index);
            return "PT" + this.scheduleActionValue + "M;tank=" + (index + 1);
        }
      }
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
