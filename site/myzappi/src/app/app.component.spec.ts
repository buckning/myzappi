import { TestBed } from '@angular/core/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({
    declarations: [AppComponent],
    imports: COMMON_TESTING_IMPORTS,
    schemas: COMMON_TESTING_SCHEMAS
  }));

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have as title 'My Zappi'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('My Zappi');
  });

  it('should render main content', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.content')).not.toBeNull();
  });
});
