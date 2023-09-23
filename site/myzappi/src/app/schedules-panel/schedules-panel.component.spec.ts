import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SchedulesPanelComponent } from './schedules-panel.component';

describe('SchedulesPanelComponent', () => {
  let component: SchedulesPanelComponent;
  let fixture: ComponentFixture<SchedulesPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SchedulesPanelComponent]
    });
    fixture = TestBed.createComponent(SchedulesPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
