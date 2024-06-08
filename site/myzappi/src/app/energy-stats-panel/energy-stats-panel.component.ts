import { Component } from '@angular/core';
import { EnergyOverviewService } from '../energy-overview.service';
import { DeviceEnergyUsage, EnergySummary } from '../energySummary.interface';

@Component({
  selector: 'app-energy-stats-panel',
  templateUrl: './energy-stats-panel.component.html',
  styleUrls: ['./energy-stats-panel.component.css']
})
export class EnergyStatsPanelComponent {

  solarGenerationKW = 0;
  consumingKW = 0;
  importingKW = 0;
  exportingKW = 0;
  zappis: DeviceEnergyUsage[] = [];
  eddis: DeviceEnergyUsage[] = [];
  libbis: DeviceEnergyUsage[] = [];

  constructor(private energyOverviewService: EnergyOverviewService) {}

  ngOnInit() {
    this.energyOverviewService.updateEnergySummaryEvent$.subscribe((energySummary) => {
      this.refresh(energySummary);
    })
  }

  refresh(energySummary: EnergySummary) {
    this.solarGenerationKW = energySummary.solarGenerationKW;
    this.consumingKW = energySummary.consumingKW;
    this.importingKW = energySummary.importingKW;
    this.exportingKW = energySummary.exportingKW;

    energySummary.deviceUsage.forEach(device => {
      if (device.deviceClass === "zappi") {
        this.zappis = this.zappis.filter(zappi => zappi.serialNumber !== device.serialNumber);
        this.zappis.push(device);
      } else if (device.deviceClass === "eddi") {
        this.eddis = this.eddis.filter(eddi => eddi.serialNumber !== device.serialNumber);
        this.eddis.push(device);
      } else if (device.deviceClass === "libbi") {
        this.libbis = this.libbis.filter(libbi => libbi.serialNumber !== device.serialNumber);
        this.libbis.push(device);
      } else {
        console.log("Device class not found " + device.deviceClass);
      }
    });
  }
}
