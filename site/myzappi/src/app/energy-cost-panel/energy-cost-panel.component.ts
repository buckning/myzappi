import { Component, Input } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

interface EnergyCost {
  currency: string;
  importCost: number,
  exportCost: number,
  solarConsumed: number,
  totalCost: number
}

@Component({
  selector: 'app-energy-cost-panel',
  templateUrl: './energy-cost-panel.component.html',
  styleUrls: ['./energy-cost-panel.component.css']
})
export class EnergyCostPanelComponent {
  @Input() public bearerToken: any;
  energyCost: string = "loading...";
  costOrCredit: string = "cost";
  isReadingEnergyCost = false;
  tariffsRegistered: boolean | undefined;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.readEnergyCost();
  }

  readEnergyCost() {
    this.isReadingEnergyCost = true;
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken
    });
    let options = { headers: headers, withCredentials: true };
    this.http.get<EnergyCost>('https://api.myzappiunofficial.com/energy-cost', options)
      .subscribe(data => {
        console.log("Got energyCost details: " + data);
        this.tariffsRegistered = true;
        // convert currency code to symbol
        const currencySymbol = this.getCurrencySymbol(data);
        console.log("Got energyCost details: " + currencySymbol + data.totalCost);
        this.energyCost = currencySymbol + Math.abs(data.totalCost).toFixed(2);
        if (data.totalCost < 0) {
          this.costOrCredit = "Credit";
        } else {
          this.costOrCredit = "Cost";
        }
        this.isReadingEnergyCost = false;
      },
        error => {
          this.isReadingEnergyCost = false;

          if (error.status === 404) {
            console.log("No tariffs registered");
            this.tariffsRegistered = false;
          }
        });
  }

  getCurrencySymbol(data: EnergyCost) {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: data.currency,
    }).formatToParts(1)[0].value;
  }
}
