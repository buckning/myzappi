import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { SchedulesPanelComponent } from './schedules-panel.component';

describe('SchedulesPanelComponent', () => {
  let component: SchedulesPanelComponent;
  let fixture: ComponentFixture<SchedulesPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [SchedulesPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(SchedulesPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
