import { Component } from '@angular/core';

declare const amazon: any;

@Component({
  selector: 'app-logged-out-content',
  templateUrl: './logged-out-content.component.html',
  styleUrls: ['./logged-out-content.component.css']
})
export class LoggedOutContentComponent {

  lwaLogin() {
    console.log("Doing LWA login...");
    var options = {
      scope: 'profile',
      scope_data: {
        profile: { essential: false }
      }
    };

    amazon.Login.setClientId('amzn1.application-oa2-client.d5625b3b0b334a388b07e0f70895ab84');
      // amazon.Login.authorize(options,
      //       'https://www.myzappiunofficial.com');

        amazon.Login.authorize(options,
            'http://localhost:4200');
  }
}
