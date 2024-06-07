import { Component } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';

@Component({
  selector: 'app-zappi-set-charge-mode-action-panel',
  templateUrl: './zappi-set-charge-mode-action-panel.component.html',
  styleUrls: ['./zappi-set-charge-mode-action-panel.component.css']
})
export class ZappiSetChargeModeActionPanelComponent implements ScheduleActionComponent {

  getScheduleAction() {
    return `{
      "type": "setChargeMode",
      "value": "ECO_PLUS",
      "target": "10000001"
    }`;
  }
  
  scheduleConfigurationStarted() {

  }

  scheduleConfigurationCancelled() {

  }
  
  scheduleConfigurationComplete() {

  }
}
