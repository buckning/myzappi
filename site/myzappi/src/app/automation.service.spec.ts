import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AutomationService } from './automation.service';

describe('AutomationService', () => {
  let service: AutomationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AutomationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates an automation through the service', () => {
    service.create('Bearer token', { name: 'Solar export' }).subscribe();

    const request = httpMock.expectOne('https://api.myzappiunofficial.com/automations');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBeTrue();
    expect(request.request.headers.get('Authorization')).toBe('Bearer token');
  });

  it('sends normalized ordered ids after drag and drop', () => {
    service.reorder('Bearer token', ['b', 'a']).subscribe();

    const request = httpMock.expectOne('https://api.myzappiunofficial.com/automations/priorities');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ automationIds: ['b', 'a'] });
  });
});
