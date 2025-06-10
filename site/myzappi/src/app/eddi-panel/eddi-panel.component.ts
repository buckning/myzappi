import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EnergyOverviewService } from '../energy-overview.service';
import { EnergySummary, DeviceEnergyUsage } from '../energySummary.interface';

interface EddiStatus {
  serialNumber: string;
  type: string;
  energy: {
    solarGenerationKW: number;
    consumingKW: number;
    importingKW: number;
    exportingKW: number;
  };
  diversionAmountKW: number;
  state: string;
  activeHeater: string;
  consumedThisSessionKWh: string;
  tank1Name: string;
  tank2Name: string;
}

@Component({
  selector: 'app-eddi-panel',
  templateUrl: './eddi-panel.component.html',
  styleUrls: ['./eddi-panel.component.css']
})
export class EddiPanelComponent implements OnInit, OnDestroy {
  @Input() public serialNumber: any;
  @Input() public bearerToken: any;
  
  state: string = '';
  activeHeater: string = '';
  consumedKwh: string = '';
  diversionAmountKW: number = 0;
  refreshInterval = 15000;
  isActive: boolean = false;
  tank1Name: string = 'Tank 1';
  tank2Name: string = 'Tank 2';
  
  constructor(private http: HttpClient, private energyOverviewService: EnergyOverviewService) {}

  ngOnInit(): void {
    this.loadDeviceStatus();
  }

  ngOnDestroy(): void {
    // Clean up if needed
  }

  loadDeviceStatus() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.get<EddiStatus>('https://api.myzappiunofficial.com/devices/' + this.serialNumber + '/status', options)
      .subscribe(data => {
        this.state = data.state;
        this.activeHeater = data.activeHeater;
        this.consumedKwh = data.consumedThisSessionKWh;
        this.diversionAmountKW = data.diversionAmountKW;
        this.isActive = this.state !== 'PAUSED' && this.state !== 'STOPPED';
        this.tank1Name = data.tank1Name;
        this.tank2Name = data.tank2Name;

        this.pushEnergySummary(data);
        setTimeout(() => {
          this.loadDeviceStatus();
        }, this.refreshInterval);
      },
      error => {
        console.log("Failed to get eddi device details " + error.status);
        setTimeout(() => {
          this.loadDeviceStatus();
        }, this.refreshInterval);
      });
  }

  pushEnergySummary(deviceStatus: EddiStatus) {
    const deviceEnergyUsage: DeviceEnergyUsage[] = [];
    const device: DeviceEnergyUsage = {
      deviceClass: deviceStatus.type,
      serialNumber: deviceStatus.serialNumber,
      usageRateKW: deviceStatus.diversionAmountKW || 0  // Now we have the diversionAmountKW field
    };
    deviceEnergyUsage.push(device);
    const energySummary: EnergySummary = {
      solarGenerationKW: deviceStatus.energy.solarGenerationKW,
      consumingKW: deviceStatus.energy.consumingKW,
      importingKW: deviceStatus.energy.importingKW,
      exportingKW: deviceStatus.energy.exportingKW,
      zappiChargeRate: 0,  // Not applicable for eddi
      deviceUsage: deviceEnergyUsage
    };

    this.energyOverviewService.updateEnergy(energySummary);
  }
}
