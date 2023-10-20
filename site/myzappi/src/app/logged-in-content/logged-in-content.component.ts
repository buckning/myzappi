import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';

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
  registeredThisSession = false;
  public eddiEnabled = false;
  @Output() public logoutEvent = new EventEmitter();

  constructor(private http: HttpClient, private cookieService: CookieService) { }

  ngOnInit(): void {
    this.getHubDetails();
  }

  logOut() {
    console.log("Logging you out from LWA and myzappi");
    amazon.Login.logout();
    this.logoutEvent.emit('');
  }
  
  getHubDetails() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers };
    this.http.get('https://api.myzappiunofficial.com/hub', options)
      .subscribe(data => {
        console.log("Got zappi details: " + data);
        this.registered = true;
        this.hubDetails = data;
        if (this.hubDetails.eddiSerialNumber !== null && this.hubDetails.eddiSerialNumber !== undefined) {
          this.eddiEnabled = true;
        }
      },
      error => {
        if (error.status === 404) {
          console.log("Hub not registered");
          this.registered = false;
        }

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
    this.getHubDetails();
  }

  deleteZappi() {
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
