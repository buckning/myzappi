import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SchedulerService {

  constructor() { }

  private scheduleEvent = new Subject<string>();
  reloadSchedulePanelEvent$ = this.scheduleEvent.asObservable();

  reloadSchedulePanel(message: string) {
    this.scheduleEvent.next(message);
  }
}
