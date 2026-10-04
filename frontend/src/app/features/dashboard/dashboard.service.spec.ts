import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { DashboardService } from './dashboard.service';
import { summaryFixture } from './dashboard.test-data';

describe('DashboardService', () => {
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('requests the real endpoint with year/month and returns the unchanged summary', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(DashboardService);
    let response: unknown;
    service.getSummary(2026, 9).subscribe(value => response = value);
    const request = TestBed.inject(HttpTestingController).expectOne(req => req.url === `${API_BASE_URL}/dashboard/summary`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.params.get('month')).toBe('9');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(summaryFixture);
    expect(response).toEqual(summaryFixture);
  });
});
