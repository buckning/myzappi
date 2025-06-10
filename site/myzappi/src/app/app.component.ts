import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';
import { EnergyOverviewService } from './energy-overview.service'; // Added
import { Subscription } from 'rxjs'; // Added

// Define Amazon SDK interface to avoid TypeScript errors
declare global {
  interface Window {
    amazon?: {
      Login?: {
        logout?: () => void;
      }
    }
  }
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'My Zappi';
  loggedIn = false;
  public bearerToken = "";
  public energyCost: string = "loading..."; // Added
  public costOrCredit: string = "cost"; // Added
  public logoUrl: string = "assets/images/myzappi-icon-floating-bar.png"; // Updated to use local asset
  private energyCostSubscription: Subscription | undefined; // Added
  public hubRegistered: boolean = false; // Added to track hub registration status
  public tariffsAvailable: boolean = false; // Track whether tariffs have been configured

  constructor(private http: HttpClient, private cookieService: CookieService, private energyOverviewService: EnergyOverviewService) {   } // Modified

  ngOnInit(): void {
    if (this.getCookie("amazon_Login_state_cache") === "") {
        console.log("Logged out");
        this.loggedIn = false; // Ensure loggedIn is false
        document.body.classList.remove('user-logged-in'); // Added
    } else {
            var cookie = JSON.parse(decodeURIComponent(this.getCookie("amazon_Login_state_cache")));
            var expiration_date = cookie.expiration_date;
            this.bearerToken = "Bearer " + cookie.access_token;
            this.loggedIn = true;

            if (Date.now() < expiration_date) {
                console.log("Logged in");
                this.subscribeToEnergyCost();
                document.body.classList.add('user-logged-in'); // Added
            } else {
                console.log("Logged out: expired");
                this.loggedIn = false;
                document.body.classList.remove('user-logged-in'); // Added
            }
        }
  }

  // Added method to subscribe to energy cost updates
  subscribeToEnergyCost(): void {
    // Assuming EnergyCostPanelComponent's logic for fetching energy cost will be centralized or accessible via EnergyOverviewService
    // For now, let's replicate parts of EnergyCostPanelComponent's readEnergyCost logic or adapt if a service method is available.
    // This is a placeholder. Ideally, EnergyOverviewService would fetch and provide this.
    this.fetchEnergyCost(); // Initial fetch

    // If EnergyOverviewService emits energy cost updates, subscribe to them.
    // For demonstration, let's assume such an observable exists or will be added to EnergyOverviewService:
    // this.energyCostSubscription = this.energyOverviewService.energyCost$.subscribe(costData => {
    // this.energyCost = costData.cost;
    // this.costOrCredit = costData.costOrCredit;
    // });
  }

  // Handle registration status change from LoggedInContentComponent
  onRegisteredStatusChange(isRegistered: boolean): void {
    this.hubRegistered = isRegistered;
    if (isRegistered) {
      this.fetchEnergyCost(); // Only fetch energy cost when hub is registered
    } else {
      this.energyCost = 'Not Available';
      this.costOrCredit = 'cost';
      this.tariffsAvailable = false; // Reset tariff status when hub isn't registered
    }
  }
  
  // Handle tariff updates from LoggedInContentComponent
  onTariffUpdated(updated: boolean): void {
    console.log('Tariffs updated, refreshing energy cost data');
    if (this.hubRegistered) {
      this.fetchEnergyCost(); // Refresh energy cost data when tariffs are updated
    }
  }

  // Added method to fetch energy cost (simplified from EnergyCostPanelComponent)
  fetchEnergyCost() {
    console.log('Fetching energy cost data...');
    
    // Reset state to loading
    this.energyCost = "loading...";
    this.costOrCredit = "cost";
    
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
    
    // Add a timestamp parameter to prevent caching
    const timestamp = Date.now();
    const url = `https://api.myzappiunofficial.com/energy-cost?_t=${timestamp}`;
    
    this.http.get<any>(url, options)
      .subscribe(data => {
        console.log('Energy cost data received:', data);
        
        if (data && data.currency && data.totalCost !== undefined) {
          const currencySymbol = new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: data.currency,
          }).formatToParts(1)[0].value;
          
          this.energyCost = currencySymbol + Math.abs(data.totalCost).toFixed(2);
          if (data.totalCost < 0) {
            this.costOrCredit = "Credit";
          } else {
            this.costOrCredit = "Cost";
          }
          this.tariffsAvailable = true; // Tariffs exist if we get valid data
        } else {
          console.warn('Received invalid energy cost data format');
          this.energyCost = "N/A";
          this.tariffsAvailable = false;
        }
      },
      error => {
        console.error("Could not fetch energy cost for header", error);
        if (error.status === 404) {
          this.energyCost = "N/A"; // No tariffs
          this.tariffsAvailable = false; // No tariffs available
        } else {
          this.energyCost = "Error";
          this.tariffsAvailable = false; // Error state, don't show
        }
      });
  }

  // Added ngOnDestroy to unsubscribe
  ngOnDestroy(): void {
    if (this.energyCostSubscription) {
      this.energyCostSubscription.unsubscribe();
    }
  }

  logOut() {
    console.log("Logging you out and should display the login screen");
    this.loggedIn = false;
    document.body.classList.remove('user-logged-in'); // Added

    // Clear the Amazon Login cookie
    this.cookieService.delete('amazon_Login_state_cache', '/');
    // Also call Amazon's logout function if available
    if (window.amazon && window.amazon.Login && window.amazon.Login.logout) {
      window.amazon.Login.logout();
    }

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
    this.http.get('https://api.myzappiunofficial.com/logout', options)
      .subscribe(data => {
        console.log('Logged out successfully');
        // Force page reload to ensure all state is cleared
        window.location.reload();
      },
      error => {
        console.log("Could not log out");
        // Force page reload even if API call fails
        window.location.reload();
      });
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
