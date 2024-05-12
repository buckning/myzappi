import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface SetChargeMode {
  mode: string;
}

@Component({
  selector: 'app-libbi-panel',
  templateUrl: './libbi-panel.component.html',
  styleUrls: ['./libbi-panel.component.css']
})
export class LibbiPanelComponent {
  @Input() public serialNumber: any;
  @Input() public bearerToken: any;

  mode: any;
  changeModeEnabled = true;

  constructor(private http: HttpClient) {}

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
