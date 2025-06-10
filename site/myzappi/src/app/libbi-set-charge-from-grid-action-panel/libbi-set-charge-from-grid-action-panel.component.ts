import { Component } from '@angular/core';
import { ScheduleActionComponent } from '../schedule-action.interface';

@Component({
  selector: 'app-libbi-set-charge-from-grid-action-panel',
  templateUrl: './libbi-set-charge-from-grid-action-panel.component.html',
  styleUrls: ['./libbi-set-charge-from-grid-action-panel.component.css']
})
export class LibbiSetChargeFromGridActionPanelComponent implements ScheduleActionComponent {
  type = "setLibbiChargeFromGrid";
  chargeFromGrid = false;
  actionTypeName = "Set Charge From Grid";

  getScheduleAction() {
    return {
      "type": "setLibbiChargeFromGrid",
      "value": this.chargeFromGrid
    };
  }

  scheduleConfigurationStarted() {
    console.log("Libbi schedule configuration started");
  }

  scheduleConfigurationCancelled() {
    console.log("Libbi schedule configuration cancelled");
  }
  
  scheduleConfigurationComplete() {
    console.log("Libbi schedule configuration complete");
  }
}
