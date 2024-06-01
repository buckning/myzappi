import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InlineSchedulePanelComponent } from './inline-schedule-panel.component';

describe('InlineSchedulePanelComponent', () => {
  let component: InlineSchedulePanelComponent;
  let fixture: ComponentFixture<InlineSchedulePanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InlineSchedulePanelComponent]
    });
    fixture = TestBed.createComponent(InlineSchedulePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
