import { Injectable } from '@angular/core';
import { DeviceEnergyUsage, EnergySummary } from './energySummary.interface';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EnergyOverviewService {

  solarGenerationKW = 0;
  consumingKW = 0;
  importingKW = 0;
  exportingKW = 0;
  devices: DeviceEnergyUsage[] = [];

  private energySummaryEvent = new Subject<EnergySummary>();
  updateEnergySummaryEvent$ = this.energySummaryEvent.asObservable();

  constructor() { }

  updateEnergy(energySummary: EnergySummary) {
    this.solarGenerationKW = energySummary.solarGenerationKW;
    this.consumingKW = energySummary.consumingKW;
    this.exportingKW = energySummary.exportingKW;
    this.importingKW = energySummary.importingKW;
    
    energySummary.deviceUsage.forEach(deviceFromEvent => {
      this.devices = this.devices.filter(device => device.serialNumber !== deviceFromEvent.serialNumber);
      this.devices.push(deviceFromEvent);
    });

    const newEnergySummary: EnergySummary = {
      solarGenerationKW: this.solarGenerationKW,
      consumingKW: this.consumingKW,
      importingKW: this.importingKW,
      exportingKW: this.exportingKW,
      zappiChargeRate: 0,
      deviceUsage: this.devices
    };

    this.energySummaryEvent.next(newEnergySummary);
  }
}
