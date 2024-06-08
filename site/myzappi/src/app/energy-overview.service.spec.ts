import { TestBed } from '@angular/core/testing';

import { EnergyOverviewService } from './energy-overview.service';

describe('EnergyOverviewService', () => {
  let service: EnergyOverviewService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EnergyOverviewService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
