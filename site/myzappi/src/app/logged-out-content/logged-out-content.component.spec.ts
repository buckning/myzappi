import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoggedOutContentComponent } from './logged-out-content.component';

describe('LoggedOutContentComponent', () => {
  let component: LoggedOutContentComponent;
  let fixture: ComponentFixture<LoggedOutContentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LoggedOutContentComponent]
    });
    fixture = TestBed.createComponent(LoggedOutContentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
