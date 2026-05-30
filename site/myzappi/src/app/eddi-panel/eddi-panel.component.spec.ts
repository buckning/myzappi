import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { EddiPanelComponent } from './eddi-panel.component';

describe('EddiPanelComponent', () => {
  let component: EddiPanelComponent;
  let fixture: ComponentFixture<EddiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EddiPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(EddiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
