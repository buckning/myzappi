import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateRecurringSchedulePanelComponent } from './create-recurring-schedule-panel.component';

describe('CreateRecurringSchedulePanelComponent', () => {
  let component: CreateRecurringSchedulePanelComponent;
  let fixture: ComponentFixture<CreateRecurringSchedulePanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CreateRecurringSchedulePanelComponent]
    });
    fixture = TestBed.createComponent(CreateRecurringSchedulePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
