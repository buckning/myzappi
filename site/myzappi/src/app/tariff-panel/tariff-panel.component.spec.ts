import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { TariffPanelComponent } from './tariff-panel.component';

describe('TariffPanelComponent', () => {
  let component: TariffPanelComponent;
  let fixture: ComponentFixture<TariffPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TariffPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(TariffPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
