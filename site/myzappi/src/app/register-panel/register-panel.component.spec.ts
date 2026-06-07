import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { RegisterPanelComponent } from './register-panel.component';

describe('RegisterPanelComponent', () => {
  let component: RegisterPanelComponent;
  let fixture: ComponentFixture<RegisterPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RegisterPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(RegisterPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
