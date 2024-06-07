import { Component, ComponentFactoryResolver, EventEmitter, Input, OnInit, Output, ViewChild, ViewContainerRef } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';
import { RecurringSchedule, Schedule } from '../schedule.interface';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { SchedulerService } from '../scheduler.service';

@Component({
  selector: 'app-inline-schedule-panel',
  templateUrl: './inline-schedule-panel.component.html',
  styleUrls: ['./inline-schedule-panel.component.css']
})
export class InlineSchedulePanelComponent implements OnInit {
  @ViewChild('actionContainer', { read: ViewContainerRef, static: true }) actionContainer!: ViewContainerRef;
  @Input() actionComponentType: any;
  @Input() public bearerToken: any;
  @Input() serialNumber: any;

  actionComponent!: ScheduleActionComponent;
  schedulePanelExpanded = false;
  startDateTime: string = '';
  createScheduleButtonEnabled = false;
  selectedScheduleOption: 'one-time' | 'recurring' = 'recurring';

  daysOfWeek: string[] = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  recurringTime: string = '';
  selectedDays: { [key: string]: boolean } = {};

  constructor(private componentFactoryResolver: ComponentFactoryResolver, private http: HttpClient, private schedulerService: SchedulerService) {}

  ngOnInit() {
    const componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.actionComponentType);
    const componentRef = this.actionContainer.createComponent(componentFactory);
    this.actionComponent = componentRef.instance as ScheduleActionComponent;
  }

  cancel() {
    this.schedulePanelExpanded = false;
    this.actionComponent.scheduleConfigurationCancelled();
  }

  toggleShowScheduling() {
    this.schedulePanelExpanded = !this.schedulePanelExpanded;
    if (this.schedulePanelExpanded) {
      this.actionComponent.scheduleConfigurationStarted();
    } else {
      this.actionComponent.scheduleConfigurationCancelled();
    }
  }

  createSchedule() {
    const result = this.actionComponent.getScheduleAction();
    console.log('Scheduling result:', this.getRequestBody());
    this.actionComponent.scheduleConfigurationComplete();


    var requestBody = JSON.stringify(this.getRequestBody());
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
        this.schedulePanelExpanded = false;
        this.schedulerService.reloadSchedulePanel('');
      },
        error => {
          console.log("error saving schedule " + JSON.stringify(requestBody));
        });
    // Example API call
    // this.httpClient.post('api/schedule', result).subscribe(response => {
    //   console.log('API response:', response);
    // });
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
    action.target = this.serialNumber;
    let newSchedule: Schedule = {
      zoneId: this.getZoneId(),
      startDateTime: this.startDateTime,
      action: action
    }
    return newSchedule;
  }

  getRecurringScheduleBody(): object {
    let action = this.actionComponent.getScheduleAction();
    action.target = this.serialNumber;
    let newSchedule: RecurringSchedule = {
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
