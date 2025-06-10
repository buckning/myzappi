import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EddiPanelComponent } from './eddi-panel.component';

describe('EddiPanelComponent', () => {
  let component: EddiPanelComponent;
  let fixture: ComponentFixture<EddiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EddiPanelComponent]
    });
    fixture = TestBed.createComponent(EddiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
