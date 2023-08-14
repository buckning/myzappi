import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoggedInContentComponent } from './logged-in-content.component';

describe('LoggedInContentComponent', () => {
  let component: LoggedInContentComponent;
  let fixture: ComponentFixture<LoggedInContentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LoggedInContentComponent]
    });
    fixture = TestBed.createComponent(LoggedInContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
