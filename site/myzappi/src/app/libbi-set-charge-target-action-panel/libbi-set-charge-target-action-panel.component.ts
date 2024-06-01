import { Component } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';

@Component({
  selector: 'app-libbi-set-charge-target-action-panel',
  templateUrl: './libbi-set-charge-target-action-panel.component.html',
  styleUrls: ['./libbi-set-charge-target-action-panel.component.css']
})
export class LibbiSetChargeTargetActionPanelComponent implements ScheduleActionComponent {

  chargeTarget = 100;
  disabled = false;


  getScheduleAction() {
    return `{
      "type": "setLibbiChargeTarget",
      "value": "100",
      "target": "30000001"
    }`;
  }

  saveChargeTarget() {
    console.log("Saving charge target to " + this.chargeTarget);
  }

  scheduleConfigurationStarted() {

  }

  scheduleConfigurationCancelled() {

  }
  
  scheduleConfigurationComplete() {

  }

}
