import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnergyCostPanelComponent } from './energy-cost-panel.component';

describe('EnergyCostPanelComponent', () => {
  let component: EnergyCostPanelComponent;
  let fixture: ComponentFixture<EnergyCostPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnergyCostPanelComponent]
    });
    fixture = TestBed.createComponent(EnergyCostPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
