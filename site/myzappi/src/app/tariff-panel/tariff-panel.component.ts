import { Component, Input } from '@angular/core';

import { HttpClient, HttpHeaders } from '@angular/common/http';

interface TariffData {
  currency: string;
  tariffs: {
    start: string;
    end: string;
    name: string;
    importCostPerKwh: number;
    exportCostPerKwh: number;
  }[];
}

interface Tariff {
  start: string;
  end: string;
  name: string;
  importCostPerKwh: number;
  exportCostPerKwh: number;
}

@Component({
  selector: 'app-tariff-panel',
  templateUrl: './tariff-panel.component.html',
  styleUrls: ['./tariff-panel.component.css']
})
export class TariffPanelComponent {
  @Input() public bearerToken: any;
  tariffCounter: number = 0;
  tariffCurrency = "EUR";
  tariffRows: any[] = [];
  editable = true;
  submitButtonDisabled:boolean = false;
  messageText = '';
  successMessageText = '';
  loaded:boolean = false;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    // make rest call to tariff api
    this.readTariffs();
    this.updateAddTariffButton();
  }

  readTariffs() {
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers };
    this.http.get<TariffData>('https://api.myzappiunofficial.com/tariff', options)
      .subscribe(data => {
        console.log("Got tariff details: " + data);
        this.tariffRows = data.tariffs;
        this.tariffCurrency = data.currency;
        this.loaded = true;
      },
      error => {
        console.log("failed to get tariff details " + error.status);
          // if 404 none are set so it should be editable

          // if not 404, there is something wrong and it should not be editable
        this.editable = true;
        this.loaded = true;
      });
  }

  addTariffRow(): void {
    this.tariffRows.push({
      start: "",
      end: "",
      name: '',
      importCostPerKwh: 0,
      exportCostPerKwh: 0
    });
  }

  deleteTariffRow(index: number): void {
    this.tariffRows.splice(index, 1);
  }

  updateAddTariffButton(): void {
    console.log("Update tariff button");
  }

  onSubmit(): void {
    console.log("On submit");
    this.messageText = "";
    this.successMessageText = "";

    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': this.bearerToken });
    let options = { headers: headers };

    var requestBody = {
      currency: this.tariffCurrency,
      tariffs: this.tariffRows
    };
    this.submitButtonDisabled = true;
    this.http.post('https://api.myzappiunofficial.com/tariff', requestBody, options)
      .subscribe(data => {
        // TODO set logged-in-content registered = true
        console.log("Success saving tariffs");
        this.submitButtonDisabled = false;
        this.successMessageText = "Tariffs saved!";
      },
      error => {
        console.log("error saving tariffs " + JSON.stringify(requestBody));
        this.submitButtonDisabled = false;
        this.messageText = "Tariff Saving Error: Please ensure you've configured tariffs to cover the entire day. Each tariff must be set on at least a minimum 30-minute basis.";
      });
  }

  isValidTime(time: string): boolean {
    const timePattern = /^(00|01|02|03|04|05|06|07|08|09|1[0-9]|2[0-4]):([03]0)$/;
    return timePattern.test(time);
  }

  isValidTariff(tariff: number): boolean {
    return tariff >= 0;
  }

  isFormValid(): boolean {
    for (const row of this.tariffRows) {
      if (!this.isValidTime(row.start) || !this.isValidTime(row.end) ||
          !this.isValidTariff(row.importCostPerKwh) || !this.isValidTariff(row.exportCostPerKwh)) {
        return false;
      }
    }
    return true;
  }
  

  getTariffName(row: any): string {
    return row.name;
  }
  toggleEdit(): void {
    console.log("Toggle edit");
    this.editable = true;
  }
}