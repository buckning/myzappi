import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { EnergyCostPanelComponent } from './energy-cost-panel.component';

describe('EnergyCostPanelComponent', () => {
  let component: EnergyCostPanelComponent;
  let fixture: ComponentFixture<EnergyCostPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnergyCostPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(EnergyCostPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
