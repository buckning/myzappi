import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZappiSetChargeModeActionPanelComponent } from './zappi-set-charge-mode-action-panel.component';

describe('ZappiSetChargeModeActionPanelComponent', () => {
  let component: ZappiSetChargeModeActionPanelComponent;
  let fixture: ComponentFixture<ZappiSetChargeModeActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZappiSetChargeModeActionPanelComponent]
    });
    fixture = TestBed.createComponent(ZappiSetChargeModeActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
