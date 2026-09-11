import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { EnergyUsageService } from './energy-usage.service';

describe('EnergyUsageService', () => {
  let service: EnergyUsageService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(EnergyUsageService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  it('requests the selected date, zone, and resolution', () => {
    service.getEnergyUsage('Bearer token', '2023-01-06', 'Europe/Dublin', 'minute').subscribe();

    const request = httpTestingController.expectOne('https://api.myzappiunofficial.com/energy-usage?date=2023-01-06&zoneId=Europe/Dublin&resolution=minute');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token');
    request.flush({ date: '2023-01-06', zoneId: 'Europe/Dublin', resolution: 'minute', readings: [] });
  });
});
