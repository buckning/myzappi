import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZappiPanelComponent } from './zappi-panel.component';

describe('ZappiPanelComponent', () => {
  let component: ZappiPanelComponent;
  let fixture: ComponentFixture<ZappiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZappiPanelComponent]
    });
    fixture = TestBed.createComponent(ZappiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
