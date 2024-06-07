import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LibbiSetChargeFromGridActionPanelComponent } from './libbi-set-charge-from-grid-action-panel.component';

describe('LibbiSetChargeFromGridActionPanelComponent', () => {
  let component: LibbiSetChargeFromGridActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetChargeFromGridActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetChargeFromGridActionPanelComponent]
    });
    fixture = TestBed.createComponent(LibbiSetChargeFromGridActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
