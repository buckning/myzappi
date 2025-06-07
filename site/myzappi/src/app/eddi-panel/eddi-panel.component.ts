import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-eddi-panel',
  templateUrl: './eddi-panel.component.html',
  styleUrls: ['./eddi-panel.component.css']
})
export class EddiPanelComponent {
  @Input() public serialNumber: any;
  @Input() public bearerToken: any;
}
