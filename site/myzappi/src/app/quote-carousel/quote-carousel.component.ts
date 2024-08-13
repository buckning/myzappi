import { Component, ContentChildren, QueryList, AfterContentInit, OnDestroy, TemplateRef } from '@angular/core';

@Component({
  selector: 'app-quote-carousel',
  templateUrl: './quote-carousel.component.html',
  styleUrls: ['./quote-carousel.component.css']
})
export class QuoteCarouselComponent implements AfterContentInit, OnDestroy {
  @ContentChildren('carouselItem') panels!: QueryList<TemplateRef<any>>;
  currentPanelIndex: number = 0;
  autoSlideInterval: any;
  autoSlideDelay: number = 5000; // 5 seconds

  ngAfterContentInit() {
    this.startAutoSlide();
  }

  nextPanel() {
    this.currentPanelIndex = (this.currentPanelIndex + 1) % this.panels.length;
  }

  previousPanel() {
    this.currentPanelIndex = (this.currentPanelIndex - 1 + this.panels.length) % this.panels.length;
  }

  startAutoSlide() {
    this.autoSlideInterval = setInterval(() => {
      this.nextPanel();
    }, this.autoSlideDelay);
  }

  resetAutoSlide() {
    clearInterval(this.autoSlideInterval);
    this.startAutoSlide();
  }

  ngOnDestroy() {
    if (this.autoSlideInterval) {
      clearInterval(this.autoSlideInterval);
    }
  }
}
