import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';

import { EspectaculosService } from './espectaculos';

describe('EspectaculosService', () => {
  let service: EspectaculosService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient()],
    });
    service = TestBed.inject(EspectaculosService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
