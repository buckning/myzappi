import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { EnergyStatsPanelComponent } from './energy-stats-panel.component';

describe('EnergyStatsPanelComponent', () => {
  let component: EnergyStatsPanelComponent;
  let fixture: ComponentFixture<EnergyStatsPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnergyStatsPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(EnergyStatsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
