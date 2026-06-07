import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { ZappiPanelComponent } from './zappi-panel.component';

describe('ZappiPanelComponent', () => {
  let component: ZappiPanelComponent;
  let fixture: ComponentFixture<ZappiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZappiPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(ZappiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
