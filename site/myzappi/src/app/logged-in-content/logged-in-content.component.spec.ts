import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LoggedInContentComponent } from './logged-in-content.component';

describe('LoggedInContentComponent', () => {
  let component: LoggedInContentComponent;
  let fixture: ComponentFixture<LoggedInContentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LoggedInContentComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LoggedInContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
