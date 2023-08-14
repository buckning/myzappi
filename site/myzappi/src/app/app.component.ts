import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'My Zappi';
  loggedIn = false;
  public bearerToken = "";

  constructor(private http: HttpClient, private cookieService: CookieService) {   }

  logOut() {
    console.log("Logging you out and should display the login screen");
    this.loggedIn = false;
  }

  ngOnInit(): void {

        if (this.getCookie("amazon_Login_state_cache") === "") {
            console.log("Logged out");
        } else {
            var cookie = JSON.parse(decodeURIComponent(this.getCookie("amazon_Login_state_cache")));
            var expiration_date = cookie.expiration_date;
            this.bearerToken = "Bearer " + cookie.access_token;
            this.loggedIn = true;

            if (Date.now() < expiration_date) {
                console.log("Logged in");
            } else {
                console.log("Logged out: expired");
            }
        }
  }

  getCookie(cname: string) {
    let name = cname + "=";
    let ca = document.cookie.split(';');
    for(let i = 0; i < ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
  }
}
