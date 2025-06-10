import { Component } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';

@Component({
  selector: 'app-libbi-set-enabled-action-panel',
  templateUrl: './libbi-set-enabled-action-panel.component.html',
  styleUrls: ['./libbi-set-enabled-action-panel.component.css']
})
export class LibbiSetEnabledActionPanelComponent implements ScheduleActionComponent {
  enabled = false;
  actionTypeName = "Set Libbi Enabled";

  getScheduleAction() {
    return {
      "type": "setLibbiEnabled",
      "value": this.enabled
    };
  }

  scheduleConfigurationStarted() {

  }

  scheduleConfigurationCancelled() {

  }
  
  scheduleConfigurationComplete() {

  }
}
