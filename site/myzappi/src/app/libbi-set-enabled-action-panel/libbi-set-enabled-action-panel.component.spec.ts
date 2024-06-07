import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LibbiSetEnabledActionPanelComponent } from './libbi-set-enabled-action-panel.component';

describe('LibbiSetEnabledActionPanelComponent', () => {
  let component: LibbiSetEnabledActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetEnabledActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetEnabledActionPanelComponent]
    });
    fixture = TestBed.createComponent(LibbiSetEnabledActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
