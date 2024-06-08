import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnergyStatsPanelComponent } from './energy-stats-panel.component';

describe('EnergyStatsPanelComponent', () => {
  let component: EnergyStatsPanelComponent;
  let fixture: ComponentFixture<EnergyStatsPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnergyStatsPanelComponent]
    });
    fixture = TestBed.createComponent(EnergyStatsPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
