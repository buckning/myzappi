import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { LoggedOutContentComponent } from './logged-out-content/logged-out-content.component';
import { LoggedInContentComponent } from './logged-in-content/logged-in-content.component';
import { HttpClientModule } from '@angular/common/http';
import { RegisterPanelComponent } from './register-panel/register-panel.component';

@NgModule({
  declarations: [
    AppComponent,
    LoggedOutContentComponent,
    LoggedInContentComponent,
    RegisterPanelComponent
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
