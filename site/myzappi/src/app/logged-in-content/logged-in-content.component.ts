import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';

declare const amazon: any;

interface Device {
  serialNumber: string;
  deviceClass: string;
}

@Component({
  selector: 'app-logged-in-content',
  templateUrl: './logged-in-content.component.html',
  styleUrls: ['./logged-in-content.component.css']
})
export class LoggedInContentComponent implements OnInit {
  @Input() public bearerToken: any;
  hubDetails: any;
  registered: any;
  devices: any[] = [];
  loadingDevices = true;
  registeredThisSession = false;
  tariffsRegistered = false;
  @Output() public logoutEvent = new EventEmitter();

  constructor(private http: HttpClient, private cookieService: CookieService) { }

  ngOnInit(): void {
    this.getDeploymentDetails();
  }

  logOut() {
    console.log("Logging you out from LWA and myzappi");
    amazon.Login.logout();
    this.logoutEvent.emit('');
  }

  getDeploymentDetails() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers };
    this.http.get<Device[]>('https://api.myzappiunofficial.com/v2/hub', options)
      .subscribe(data => {
        this.loadingDevices = false;
        this.devices = data;
        this.registered = this.devices.length > 0;
        this.hubDetails = {};
        this.hubDetails.eddiSerialNumber = null;
        
        for (const device of this.devices) {
          if (device.deviceClass === "ZAPPI") {
            this.hubDetails.zappiSerialNumber = device.serialNumber;
          } else if (device.deviceClass === "EDDI") {
            this.hubDetails.eddiSerialNumber = device.serialNumber;
          }
        }
      },
      error => {
        this.loadingDevices = false;
        if (error.status === 401) {
          console.log("You are not logged in")
          this.logoutEvent.emit('');
        }
      });
  }

  registeredEvent() {
    console.log('Received register event');
    this.registered = true;
    this.registeredThisSession = true;
    this.getDeploymentDetails();
  }

  refreshDeploymentDetails() {
    console.log("Refreshing...");
    this.loadingDevices = true;
    this.getDeploymentDetails();
  }

  tariffChangeEvent() {
    console.log("Tariffs were changed");
    this.tariffsRegistered = true;
  }

  deleteZappi() {
    this.loadingDevices = true;
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers };
    this.http.delete('https://api.myzappiunofficial.com/hub', options)
      .subscribe(data => {
        console.log("Deleted Zappi");
        this.hubDetails = null;
        this.registered = false;
      });
  }
}
