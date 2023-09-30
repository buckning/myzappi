import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateOnetimeSchedulePanelComponent } from './create-onetime-schedule-panel.component';

describe('CreateOnetimeSchedulePanelComponent', () => {
  let component: CreateOnetimeSchedulePanelComponent;
  let fixture: ComponentFixture<CreateOnetimeSchedulePanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CreateOnetimeSchedulePanelComponent]
    });
    fixture = TestBed.createComponent(CreateOnetimeSchedulePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
