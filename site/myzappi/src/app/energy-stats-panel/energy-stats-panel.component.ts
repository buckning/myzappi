import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { EnergyOverviewService } from '../energy-overview.service';
import { DeviceEnergyUsage, EnergySummary } from '../energySummary.interface';

@Component({
    selector: 'app-energy-stats-panel',
    templateUrl: './energy-stats-panel.component.html',
    styleUrls: ['./energy-stats-panel.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EnergyStatsPanelComponent {

  @Input() public bearerToken: string | null = null;
  solarGenerationKW = 0;
  consumingKW = 0;
  importingKW = 0;
  exportingKW = 0;
  zappis: DeviceEnergyUsage[] = [];
  eddis: DeviceEnergyUsage[] = [];
  libbis: DeviceEnergyUsage[] = [];
  refreshInterval = 15000;

  constructor(private readonly http: HttpClient, private readonly energyOverviewService: EnergyOverviewService) {}

  ngOnInit() {
    this.energyOverviewService.updateEnergySummaryEvent$.subscribe((energySummary) => {
      this.refresh(energySummary);
    });

    this.loadEnergySummary();
  }

  loadEnergySummary() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken ?? '' });
    let options = { headers: headers, withCredentials: true };
    this.http.get<EnergySummary>('https://api.myzappiunofficial.com/energy-summary', options)
      .subscribe(data => {
        this.solarGenerationKW = data.solarGenerationKW;
        this.consumingKW = data.consumingKW;
        this.importingKW = data.importingKW;
        this.exportingKW = data.exportingKW;

        setTimeout(() => {
          this.loadEnergySummary();
        }, this.refreshInterval);
      },
      error => {
        console.log("failed to get device details " + error.status);
        setTimeout(() => {
          this.loadEnergySummary();
        }, this.refreshInterval);
      });
  }

  refresh(energySummary: EnergySummary) {
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
