import { Component, Inject, OnInit, ViewChild, ViewContainerRef } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ScheduleActionComponent } from '../../schedule-action.interface';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SchedulerService } from '../../scheduler.service';
import { Schedule } from '../../schedule.interface';

@Component({
  selector: 'app-schedule-dialog',
  templateUrl: './schedule-dialog.component.html',
  styleUrls: ['./schedule-dialog.component.css']
})
export class ScheduleDialogComponent implements OnInit {
  @ViewChild('actionContainer', { read: ViewContainerRef, static: true }) actionContainer!: ViewContainerRef;
  
  startDateTime: string = '';
  createScheduleButtonEnabled = false;
  selectedScheduleOption: 'one-time' | 'recurring' = 'recurring';

  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  recurringTime: string = '';
  selectedDays: { [key: string]: boolean } = {};
  actionComponent!: ScheduleActionComponent;
  
  // Properties to display context information in the dialog header
  deviceName: string = '';
  actionType: string = '';
  
  // Flag to prevent multiple submissions
  isSubmitting: boolean = false;
  
  // Error message to display when schedule creation fails
  errorMessage: string = '';

  constructor(
    public dialogRef: MatDialogRef<ScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private http: HttpClient,
    private schedulerService: SchedulerService
  ) {}

  ngOnInit() {
    // Set the device name from the serial number or a friendly name if provided
    this.deviceName = this.data.deviceName || `Device ${this.data.serialNumber}`;
    
    // Create the action component
    setTimeout(() => {
      const componentFactory = this.data.componentFactoryResolver.resolveComponentFactory(this.data.actionComponentType);
      const componentRef = this.actionContainer.createComponent(componentFactory);
      this.actionComponent = componentRef.instance as ScheduleActionComponent;
      
      // Get the action type directly from the component instance
      this.actionType = this.actionComponent.actionTypeName || 'Action';
      
      this.actionComponent.scheduleConfigurationStarted();
    });
  }
  
  // getActionTypeFromComponentName method removed - we now use actionTypeName from the component

  cancel() {
    this.actionComponent.scheduleConfigurationCancelled();
    this.errorMessage = '';  // Clear any error messages
    this.dialogRef.close();
  }

  createSchedule() {
    // Prevent multiple submissions
    if (this.isSubmitting) {
      return;
    }
    
    // Set the submitting flag to true to disable the button
    this.isSubmitting = true;
    
    const result = this.actionComponent.getScheduleAction();
    console.log('Scheduling result:', this.getRequestBody());
    this.actionComponent.scheduleConfigurationComplete();

    var requestBody = JSON.stringify(this.getRequestBody());
    console.log("Creating new schedule: " + requestBody);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.data.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
    this.http.post('https://api.myzappiunofficial.com/schedules', requestBody, options)
      .subscribe(
        data => {
          console.log("Success saving schedule");
          this.errorMessage = '';
          this.dialogRef.close(true); // Close with success result
          this.schedulerService.reloadSchedulePanel('');
        },
        error => {
          console.log("error saving schedule " + JSON.stringify(requestBody));
          // Reset the submitting flag if there's an error, so the user can try again
          this.isSubmitting = false;
          
          // Set the error message instead of closing the dialog
          if (error.status === 401) {
            this.errorMessage = 'Authentication error. Please check your login credentials.';
          } else if (error.status === 400) {
            this.errorMessage = 'Invalid schedule data. Please check your inputs.';
          } else {
            this.errorMessage = 'Failed to create schedule. Please try again later.';
          }
        }
      );
  }

  getRequestBody() {
    if (this.selectedScheduleOption === 'recurring') {
      return this.getRecurringScheduleBody();
    } else {
      return this.getOneTimeScheduleBody();
    }
  }

  getOneTimeScheduleBody(): object {
    let action = this.actionComponent.getScheduleAction();
    action.target = this.data.serialNumber;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      startDateTime: this.startDateTime,
      action: action
    }
    return newSchedule;
  }

  getRecurringScheduleBody(): object {
    let action = this.actionComponent.getScheduleAction();
    action.target = this.data.serialNumber;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      recurrence: {
        timeOfDay: this.recurringTime,
        daysOfWeek: this.convertDaysOfWeekToNumbers()
      },
      action: action
    };
    return newSchedule;
  }

  isScheduleValid(): boolean {
    if (this.selectedScheduleOption === 'recurring') {
      return this.isValidRecurringSchedule();
    }
    return this.isValidOneTimeSchedule();
  }

  isValidOneTimeSchedule(): boolean {
    return this.startDateTime !== '' && this.isDateTimeValid();
  }

  isValidRecurringSchedule(): boolean {
    return this.convertDaysOfWeekToNumbers().length > 0 && this.recurringTime !== '';
  }

  isDateTimeValid(): boolean {
    return new Date() < new Date(this.startDateTime)
  }

  toggleDay(day: string) {
    this.selectedDays[day] = !this.selectedDays[day];
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

  getZoneId(): string {
    return Intl.DateTimeFormat().resolvedOptions().timeZone;
  }
}
