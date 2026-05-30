import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { InlineSchedulePanelComponent } from './inline-schedule-panel.component';

describe('InlineSchedulePanelComponent', () => {
  let component: InlineSchedulePanelComponent;
  let fixture: ComponentFixture<InlineSchedulePanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InlineSchedulePanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(InlineSchedulePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
