import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TariffPanelComponent } from './tariff-panel.component';

describe('TariffPanelComponent', () => {
  let component: TariffPanelComponent;
  let fixture: ComponentFixture<TariffPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TariffPanelComponent]
    });
    fixture = TestBed.createComponent(TariffPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
