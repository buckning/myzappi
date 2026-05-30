import { NO_ERRORS_SCHEMA } from '@angular/core';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSliderModule } from '@angular/material/slider';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTabsModule } from '@angular/material/tabs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

export const COMMON_TESTING_IMPORTS = [
  DragDropModule,
  FormsModule,
  HttpClientTestingModule,
  MatButtonToggleModule,
  MatDialogModule,
  MatDividerModule,
  MatExpansionModule,
  MatSlideToggleModule,
  MatSliderModule,
  MatStepperModule,
  MatTabsModule,
  NoopAnimationsModule
];

export const COMMON_TESTING_SCHEMAS = [NO_ERRORS_SCHEMA];
