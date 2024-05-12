import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { LoggedOutContentComponent } from './logged-out-content/logged-out-content.component';
import { LoggedInContentComponent } from './logged-in-content/logged-in-content.component';
import { HttpClientModule } from '@angular/common/http';
import { RegisterPanelComponent } from './register-panel/register-panel.component';
import { TariffPanelComponent } from './tariff-panel/tariff-panel.component';
import { SchedulesPanelComponent } from './schedules-panel/schedules-panel.component';
import { CreateRecurringSchedulePanelComponent } from './create-recurring-schedule-panel/create-recurring-schedule-panel.component';
import { CreateOnetimeSchedulePanelComponent } from './create-onetime-schedule-panel/create-onetime-schedule-panel.component';
import { EnergyCostPanelComponent } from './energy-cost-panel/energy-cost-panel.component';
import { HelpPanelComponent } from './help-panel/help-panel.component';
import { ZappiPanelComponent } from './zappi-panel/zappi-panel.component';
import { LibbiPanelComponent } from './libbi-panel/libbi-panel.component';

@NgModule({
  declarations: [
    AppComponent,
    LoggedOutContentComponent,
    LoggedInContentComponent,
    RegisterPanelComponent,
    TariffPanelComponent,
    SchedulesPanelComponent,
    CreateRecurringSchedulePanelComponent,
    CreateOnetimeSchedulePanelComponent,
    EnergyCostPanelComponent,
    HelpPanelComponent,
    ZappiPanelComponent,
    LibbiPanelComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
