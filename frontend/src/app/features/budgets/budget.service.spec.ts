import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { BudgetService } from './budget.service';
import { BudgetFilters } from './budget.models';

const url = `${API_BASE_URL}/budgets`;
const dto = { categoryId: 1, year: 2026, month: 9, amount: 300.1234 };
const budget = { id: 3, ...dto, categoryName: 'Transport', spent: 250, remaining: 50.1234, usagePercent: 83.3 };

describe('BudgetService', () => {
  let service: BudgetService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BudgetService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it.each([
    [{} as BudgetFilters, {}],
    [{ year: 2026 }, { year: '2026' }],
    [{ month: 9 }, { month: '9' }],
    [{ categoryId: 1 }, { categoryId: '1' }],
    [{ year: 2026, month: 9, categoryId: 1 }, { year: '2026', month: '9', categoryId: '1' }],
  ])('gets the list with supplied filters %j', (filters, params) => {
    service.getAll(filters).subscribe(value => expect(value).toEqual([budget]));
    const request = http.expectOne(req => req.url === url);
    expect(request.request.method).toBe('GET');
    expect(Object.fromEntries(request.request.params.keys().map(key => [key, request.request.params.get(key)]))).toEqual(params);
    request.flush([budget]);
  });
  it('gets one budget', () => {
    service.getOne(3).subscribe(value => expect(value).toEqual(budget));
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('GET');
    request.flush(budget);
  });
  it.each(['create', 'update'] as const)('sends only request fields for %s even from a response object', operation => {
    (operation === 'create' ? service.create(budget) : service.update(3, budget)).subscribe();
    const request = http.expectOne(operation === 'create' ? url : `${url}/3`);
    expect(request.request.method).toBe(operation === 'create' ? 'POST' : 'PUT');
    expect(request.request.body).toEqual(dto);
    expect(Object.keys(request.request.body).sort()).toEqual(['amount', 'categoryId', 'month', 'year']);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(budget);
  });
  it('deletes a budget', () => {
    service.delete(3).subscribe();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
