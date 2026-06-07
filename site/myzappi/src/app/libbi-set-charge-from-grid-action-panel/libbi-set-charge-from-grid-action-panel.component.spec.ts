import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LibbiSetChargeFromGridActionPanelComponent } from './libbi-set-charge-from-grid-action-panel.component';

describe('LibbiSetChargeFromGridActionPanelComponent', () => {
  let component: LibbiSetChargeFromGridActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetChargeFromGridActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetChargeFromGridActionPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LibbiSetChargeFromGridActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
