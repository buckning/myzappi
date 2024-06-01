import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { LibbiSetChargeTargetActionPanelComponent } from '../libbi-set-charge-target-action-panel/libbi-set-charge-target-action-panel.component';
import { LibbiSetChargeFromGridActionPanelComponent } from '../libbi-set-charge-from-grid-action-panel/libbi-set-charge-from-grid-action-panel.component';
interface SetChargeMode {
  mode: string;
}

interface AccountSummary {
  hubRegistered: boolean,
  myaccountRegistered: boolean
}

interface SetChargeFromGrid {
  chargeFromGrid: boolean;
}

interface LibbiSummary {
  serialNumber: string,
  stateOfChargePercentage: number,
  batterySizeKWh: string,
  chargeFromGridEnabled: boolean,
  energyTargetKWh: string;
}

@Component({
  selector: 'app-libbi-panel',
  templateUrl: './libbi-panel.component.html',
  styleUrls: ['./libbi-panel.component.css']
})
export class LibbiPanelComponent {
  @Input() public serialNumber: any;
  @Input() public bearerToken: any;

  libbiSetChargeTargetActionPanelComponent = LibbiSetChargeTargetActionPanelComponent;
  libbiSetChargeFromGridActionPanelComponent = LibbiSetChargeFromGridActionPanelComponent;
  registrationComplete = false;
  emailAddressText: string = '';
  passwordText: string = '';
  registerButtonDisabled:boolean = false;
  messageText = '';
  stateOfCharge: number = -1;
  batterySize = '';
  energyTargetKWh = '';

  mode: any;
  changeModeEnabled = true;
  myenergiAccountEmail: string = '';
  myenergiAccountPassword: string = '';
  chargeFromGrid = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.readAccountSettings();
    this.getStatus();
  }

  getStatus() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.get<LibbiSummary>('https://api.myzappiunofficial.com/devices/' + this.serialNumber + '/status', options)
      .subscribe(data => {
        this.chargeFromGrid = data.chargeFromGridEnabled;
        this.batterySize = data.batterySizeKWh;
        this.stateOfCharge = data.stateOfChargePercentage;
        this.energyTargetKWh = data.energyTargetKWh;
      });
  }

  readAccountSettings() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.get<AccountSummary>('https://api.myzappiunofficial.com/account/summary', options)
      .subscribe(data => {
        this.registrationComplete = data.myaccountRegistered;
      });
  }

  registerMyEnergiAccount() {
    this.registerButtonDisabled = true;
    
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });

    let options = { headers: headers, withCredentials: true };

    let requestBody = {
      email: this.emailAddressText,
      password: this.passwordText
    }

    this.http.post('https://api.myzappiunofficial.com/account/register', requestBody, options)
      .subscribe(data => {
        this.registrationComplete = true;
      },
      error => {
        this.registerButtonDisabled = false;
        this.messageText = "Something went wrong. Please check you entered the correct email and password.";
      });
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

  enableChargeFromGrid() {
    this.setChargeFromGrid(true);
  }

  disableChargeFromGrid() {
    this.setChargeFromGrid(false);
  }

  setChargeFromGrid(_chargeFromGrid: boolean) {
    let request: SetChargeFromGrid = {
      chargeFromGrid: _chargeFromGrid
    };

    var requestBody = JSON.stringify(request);

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.put<void>('https://api.myzappiunofficial.com/devices/' + this.serialNumber + '/charge-from-grid', requestBody, options)
      .subscribe(data => {
        console.log("Switched charge from grid to : " + _chargeFromGrid);
      },
      error => {
        console.log("failed to switch charge from grid " + error.status);
      });
  }

  updateDeviceMode(newMode: string) {

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
}
