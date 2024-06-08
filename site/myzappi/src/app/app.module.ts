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
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSliderModule } from '@angular/material/slider';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { InlineSchedulePanelComponent } from './inline-schedule-panel/inline-schedule-panel.component';
import { ZappiSetChargeModeActionPanelComponent } from './zappi-set-charge-mode-action-panel/zappi-set-charge-mode-action-panel.component';
import { LibbiSetChargeTargetActionPanelComponent } from './libbi-set-charge-target-action-panel/libbi-set-charge-target-action-panel.component';
import { LibbiSetChargeFromGridActionPanelComponent } from './libbi-set-charge-from-grid-action-panel/libbi-set-charge-from-grid-action-panel.component';
import { LibbiSetEnabledActionPanelComponent } from './libbi-set-enabled-action-panel/libbi-set-enabled-action-panel.component';
import { EnergyStatsPanelComponent } from './energy-stats-panel/energy-stats-panel.component';

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
    LibbiPanelComponent,
    InlineSchedulePanelComponent,
    ZappiSetChargeModeActionPanelComponent,
    LibbiSetChargeTargetActionPanelComponent,
    LibbiSetChargeFromGridActionPanelComponent,
    LibbiSetEnabledActionPanelComponent,
    EnergyStatsPanelComponent
  ],
  imports: [
    MatSlideToggleModule,
    MatExpansionModule,
    MatSliderModule,
    MatButtonToggleModule,
    BrowserModule,
    HttpClientModule,
    FormsModule,
    BrowserAnimationsModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
