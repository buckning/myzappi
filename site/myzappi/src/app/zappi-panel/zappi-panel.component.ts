import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { ZappiSetChargeModeActionPanelComponent } from '../zappi-set-charge-mode-action-panel/zappi-set-charge-mode-action-panel.component';
import { EnergyOverviewService } from '../energy-overview.service';
import { EnergySummary, DeviceEnergyUsage } from '../energySummary.interface';
interface DeviceStatus {
  serialNumber: string;
  type: string;
  firmware: string;
  energy: {
    solarGenerationKW: number;
    consumingKW: number;
    importingKW: number;
    exportingKW: number;
  };
  mode: string;
  chargeAddedKwh: string;
  connectionStatus: string;
  chargeStatus: string;
  chargeRateKw: number;
  lockStatus: string;
}

interface SetChargeMode {
  mode: string;
}

@Component({
  selector: 'app-zappi-panel',
  templateUrl: './zappi-panel.component.html',
  styleUrls: ['./zappi-panel.component.css']
})
export class ZappiPanelComponent {
  @Input() public serialNumber: any;
  @Input() public bearerToken: any;
  zappiSetChargeModeActionPanelComponent = ZappiSetChargeModeActionPanelComponent;
  chargeAddedKwh = '';
  chargeRate = 0;
  mode: any;
  changeModeEnabled = true;
  refreshInterval = 15000;

  constructor(private http: HttpClient, private energyOverviewService: EnergyOverviewService) {}

  ngOnInit(): void {
    this.loadDeviceStatus();
  }

  loadDeviceStatus() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.get<DeviceStatus>('https://api.myzappiunofficial.com/devices/' + this.serialNumber + '/status', options)
      .subscribe(data => {
        this.chargeAddedKwh = data.chargeAddedKwh;
        this.chargeRate = data.chargeRateKw;
        this.mode = data.mode;
        this.pushEnergySummary(data);
        setTimeout(() => {
          this.loadDeviceStatus();
        }, this.refreshInterval);
      },
      error => {
        console.log("failed to get device details " + error.status);
        setTimeout(() => {
          this.loadDeviceStatus();
        }, this.refreshInterval);
      });
  }

  pushEnergySummary(deviceStatus: DeviceStatus) {
    const deviceEnergyUsage: DeviceEnergyUsage[] = [];
    const device: DeviceEnergyUsage = {
      deviceClass: deviceStatus.type,
      serialNumber: deviceStatus.serialNumber,
      usageRateKW: deviceStatus.chargeRateKw
    };
    deviceEnergyUsage.push(device);
    const energySummary: EnergySummary = {
      solarGenerationKW: deviceStatus.energy.solarGenerationKW,
      consumingKW: deviceStatus.energy.consumingKW,
      importingKW: deviceStatus.energy.importingKW,
      exportingKW: deviceStatus.energy.exportingKW,
      zappiChargeRate: deviceStatus.chargeRateKw,
      deviceUsage: deviceEnergyUsage
    };

    this.energyOverviewService.updateEnergy(energySummary);
  }

  updateDeviceMode(newMode: string) {
    this.changeModeEnabled = false;
    setTimeout(() => {
      this.enabledChangeMode();
    }, 20000);

    let request: SetChargeMode = {
      mode: newMode
    };

    var requestBody = JSON.stringify(request);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.put<void>('https://api.myzappiunofficial.com/devices/' + this.serialNumber + '/mode', requestBody, options)
      .subscribe(data => {
        console.log("Switched device to status details: " + newMode);
      },
      error => {
        console.log("failed to switch device to new mod " + error.status);
      });
  }

  enabledChangeMode() {
    this.changeModeEnabled = true;
  }

  isModeActive(mode: string): boolean {
    return this.mode === mode;
  }

  isChangeModeEnabled(): boolean {
    return this.changeModeEnabled;
  }

  changeMode(newMode: string): void {
    // disable the buttons for 10 seconds
    console.log("Switching to " + newMode);
    // You can add any additional logic here before updating the mode
    this.mode = newMode; // Update the mode based on the button clicked
    // Optionally, you can also make an API call to update the mode on the server
    this.updateDeviceMode(newMode);
  }
}
