import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LibbiPanelComponent } from './libbi-panel.component';

describe('LibbiPanelComponent', () => {
  let component: LibbiPanelComponent;
  let fixture: ComponentFixture<LibbiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiPanelComponent]
    });
    fixture = TestBed.createComponent(LibbiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
