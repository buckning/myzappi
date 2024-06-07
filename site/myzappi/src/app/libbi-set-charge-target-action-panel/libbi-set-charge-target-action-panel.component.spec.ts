import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LibbiSetChargeTargetActionPanelComponent } from './libbi-set-charge-target-action-panel.component';

describe('LibbiSetChargeTargetActionPanelComponent', () => {
  let component: LibbiSetChargeTargetActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetChargeTargetActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetChargeTargetActionPanelComponent]
    });
    fixture = TestBed.createComponent(LibbiSetChargeTargetActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
