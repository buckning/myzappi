import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { CreateOnetimeSchedulePanelComponent } from './create-onetime-schedule-panel.component';

describe('CreateOnetimeSchedulePanelComponent', () => {
  let component: CreateOnetimeSchedulePanelComponent;
  let fixture: ComponentFixture<CreateOnetimeSchedulePanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CreateOnetimeSchedulePanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(CreateOnetimeSchedulePanelComponent);
    component = fixture.componentInstance;
    component.bearerToken = 'Bearer token';
    component.hubDetails = [{ serialNumber: '10000001', deviceClass: 'ZAPPI', tank1Name: '', tank2Name: '' }];
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
