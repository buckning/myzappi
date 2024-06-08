import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';
import { Device } from '../device.interface';

declare const amazon: any;

@Component({
  selector: 'app-logged-in-content',
  templateUrl: './logged-in-content.component.html',
  styleUrls: ['./logged-in-content.component.css']
})
export class LoggedInContentComponent implements OnInit {
  @Input() public bearerToken: any;
  hubDetails: any;
  registered: any;
  devices: Device[] = [];
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
    let options = { headers: headers, withCredentials: true };
    this.http.get<Device[]>('https://api.myzappiunofficial.com/v2/hub', options)
      .subscribe(data => {
        this.loadingDevices = false;
        this.devices = data;
        this.registered = this.devices.length > 0;
        this.hubDetails = {};
        this.hubDetails = this.devices;
        
        for (const device of this.devices) {
          if (device.deviceClass === "ZAPPI") {
            this.hubDetails.zappiSerialNumber = device.serialNumber;
          } else if (device.deviceClass === "EDDI") {
            this.hubDetails.eddiSerialNumber = device.serialNumber;
          } else if (device.deviceClass === "LIBBI") {
            this.hubDetails.libbiSerialNumber = device.serialNumber;
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

  triggerRefresh() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers, withCredentials: true };
    this.http.post<Device[]>('https://api.myzappiunofficial.com/hub/refresh', '', options)
      .subscribe(data => {
        this.getDeploymentDetails();
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
    this.triggerRefresh();
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
    let options = { headers: headers, withCredentials: true };
    this.http.delete('https://api.myzappiunofficial.com/hub', options)
      .subscribe(data => {
        console.log("Deleted Zappi");
        this.hubDetails = null;
        this.registered = false;
      });
  }
}
