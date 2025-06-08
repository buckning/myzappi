import { Component } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';

@Component({
  selector: 'app-zappi-set-charge-mode-action-panel',
  templateUrl: './zappi-set-charge-mode-action-panel.component.html',
  styleUrls: ['./zappi-set-charge-mode-action-panel.component.css']
})
export class ZappiSetChargeModeActionPanelComponent implements ScheduleActionComponent {

  chargeMode = "ECO_PLUS";
  actionTypeName = "Set Charge Mode";
  
  getScheduleAction() {
    return {
      "type": "setChargeMode",
      "value": this.chargeMode,
      "target": "10000001"
    };
  }

  changeMode(mode: string) {
    this.chargeMode = mode;
  }

  isModeActive(mode: string): boolean {
    return this.chargeMode === mode;
  }
  
  scheduleConfigurationStarted() {

  }

  scheduleConfigurationCancelled() {

  }
  
  scheduleConfigurationComplete() {

  }
}
