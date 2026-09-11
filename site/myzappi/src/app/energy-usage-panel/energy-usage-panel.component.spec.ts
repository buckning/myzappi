import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { COMMON_TESTING_IMPORTS, COMMON_TESTING_SCHEMAS } from 'src/app/testing/common-testing.module';
import { EnergyUsagePanelComponent } from './energy-usage-panel.component';

describe('EnergyUsagePanelComponent', () => {
  let component: EnergyUsagePanelComponent;
  let fixture: ComponentFixture<EnergyUsagePanelComponent>;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EnergyUsagePanelComponent],
      imports: COMMON_TESTING_IMPORTS,
      schemas: COMMON_TESTING_SCHEMAS
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EnergyUsagePanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads minute graph data from the energy usage API', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.querySelector('canvas')).toBeTruthy();

    const request = httpTestingController.expectOne(request => request.url === 'https://api.myzappiunofficial.com/energy-usage');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('date')).toBe(component.selectedDate);
    expect(request.request.params.get('resolution')).toBe('minute');
    expect(request.request.params.get('zoneId')).toBeTruthy();
    request.flush({
      date: component.selectedDate,
      zoneId: request.request.params.get('zoneId'),
      resolution: 'minute',
      readings: [{
        timestamp: '2026-09-06T12:00:00Z',
        solarGenerationKWh: 2,
        importedKWh: 0.5,
        exportedKWh: 1,
        consumedKWh: 1.5,
        importedKW: 0.5,
        exportedKW: 1,
        consumedKW: 1.5
      }]
    });

    expect(component.energyUsage?.readings.length).toBe(1);
  });

  it('disables moving forward when the selected day is today', () => {
    expect(component.isTodaySelected()).toBeTrue();
    fixture.detectChanges();

    const nextButton = fixture.nativeElement.querySelector('[aria-label="Next day"]') as HTMLButtonElement;
    expect(nextButton.disabled).toBeTrue();
  });
});
