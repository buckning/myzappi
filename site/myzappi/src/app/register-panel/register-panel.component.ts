import { Component, Input, Output, EventEmitter } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'app-register-panel',
  templateUrl: './register-panel.component.html',
  styleUrls: ['./register-panel.component.css']
})
export class RegisterPanelComponent {
  @Input() public bearerToken: any;
  serialNumberText: string = '';
  apiKeyText: string = '';
  buttonDisabled:boolean = false;
  messageText = '';
  registrationComplete = false;
  @Output() public registeredEvent = new EventEmitter();

  constructor(private http: HttpClient) { }

  // could use template reference variables here instead two way binding with ngModel, serialNumberText and apiKeyText fields
  registerZappi() {
    this.buttonDisabled = true;
    
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });

    let options = { headers: headers, withCredentials: true };

    let requestBody = {
      apiKey: this.apiKeyText,
      serialNumber: this.serialNumberText
    }

    this.http.post('https://api.myzappiunofficial.com/hub', requestBody, options)
      .subscribe(data => {
        // TODO set logged-in-content registered = true
        this.messageText = '';
        this.registrationComplete = true;
        console.log('Emitting registered event');
        this.registeredEvent.emit('');
      },
      error => {
        this.buttonDisabled = false;
        this.messageText = "Something went wrong. Please check you entered the correct serial number and API key.";
      });
  }
}
