import { ComponentFixture, TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';

import { LibbiPanelComponent } from './libbi-panel.component';

describe('LibbiPanelComponent', () => {
  let component: LibbiPanelComponent;
  let fixture: ComponentFixture<LibbiPanelComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [LibbiPanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    fixture = TestBed.createComponent(LibbiPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
