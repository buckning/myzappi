import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EnergyOverviewService } from '../energy-overview.service';
import { EnergySummary, DeviceEnergyUsage } from '../energySummary.interface';
import { Device } from '../device.interface';

interface EddiStatus {
  serialNumber: string;
  type: string;
  energy: {
    solarGenerationKW: number;
    consumingKW: number;
    importingKW: number;
    exportingKW: number;
  };
  state: string;
  activeHeater: string;
  consumedThisSessionKWh: string;
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
  refreshInterval = 15000;
  isActive: boolean = false;
  tank1Name: string = 'Tank 1';
  tank2Name: string = 'Tank 2';
  
  constructor(private http: HttpClient, private energyOverviewService: EnergyOverviewService) {}

  ngOnInit(): void {
    this.loadDeviceInfo();
    this.loadDeviceStatus();
  }
  
  loadDeviceInfo() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
    this.http.get<Device[]>('https://api.myzappiunofficial.com/v2/hub', options)
      .subscribe(devices => {
        // Find the eddi device matching our serial number
        const eddiDevice = devices.find(device => 
          device.serialNumber === this.serialNumber && device.deviceClass === 'EDDI');
          
        if (eddiDevice) {
          this.tank1Name = eddiDevice.tank1Name || 'Tank 1';
          this.tank2Name = eddiDevice.tank2Name || 'Tank 2';
        }
      },
      error => {
        console.log("Failed to get device info: " + error.status);
      });
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
        this.isActive = this.state !== 'PAUSED' && this.state !== 'STOPPED';

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
      usageRateKW: 0  // Eddi doesn't have a direct chargeRateKw equivalent, could calculate from other data if needed
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
