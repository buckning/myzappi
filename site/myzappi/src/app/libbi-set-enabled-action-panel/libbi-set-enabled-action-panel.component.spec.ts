import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LibbiSetEnabledActionPanelComponent } from './libbi-set-enabled-action-panel.component';

describe('LibbiSetEnabledActionPanelComponent', () => {
  let component: LibbiSetEnabledActionPanelComponent;
  let fixture: ComponentFixture<LibbiSetEnabledActionPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiSetEnabledActionPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LibbiSetEnabledActionPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
