import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LibbiSetChargeTargetActionPanelComponent } from './libbi-set-charge-target-action-panel.component';

describe('LibbiSetChargeTargetActionPanelComponent', () => {
  let component: LibbiSetChargeTargetActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetChargeTargetActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetChargeTargetActionPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LibbiSetChargeTargetActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
