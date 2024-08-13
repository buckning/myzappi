import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuoteCarouselComponent } from './quote-carousel.component';

describe('QuoteCarouselComponent', () => {
  let component: QuoteCarouselComponent;
  let fixture: ComponentFixture<QuoteCarouselComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [QuoteCarouselComponent]
    });
    fixture = TestBed.createComponent(QuoteCarouselComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
