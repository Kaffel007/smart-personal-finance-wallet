import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { TransactionService } from './transaction.service';
import { TransactionFilters } from './transaction.models';

const url = `${API_BASE_URL}/transactions`;
const dto = { categoryId: 1, amount: 12.3456, transactionDate: '2026-01-01', description: null };
const transaction = { id: 3, ...dto, type: 'EXPENSE' as const, categoryName: 'Transport' };

describe('TransactionService', () => {
  let service: TransactionService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransactionService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it.each([
    [{} as TransactionFilters, {}],
    [{ type: 'INCOME' } as TransactionFilters, { type: 'INCOME' }],
    [{ type: 'EXPENSE' } as TransactionFilters, { type: 'EXPENSE' }],
    [{ categoryId: 7 } as TransactionFilters, { categoryId: '7' }],
    [{ type: 'EXPENSE', categoryId: 7 } as TransactionFilters, { type: 'EXPENSE', categoryId: '7' }],
  ])('gets transactions using only the supplied filters %j', (filters, params) => {
    service.getAll(filters).subscribe(value => expect(value).toEqual([transaction]));
    const request = http.expectOne(req => req.url === url);
    expect(request.request.method).toBe('GET');
    expect(Object.fromEntries(request.request.params.keys().map(key => [key, request.request.params.get(key)]))).toEqual(params);
    request.flush([transaction]);
  });
  it('gets one transaction', () => {
    service.getOne(3).subscribe();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('GET');
    request.flush(transaction);
  });
  it('creates a transaction without type or userId', () => {
    service.create(dto).subscribe();
    const request = http.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(dto);
    expect(Object.keys(request.request.body).sort()).toEqual(['amount', 'categoryId', 'description', 'transactionDate']);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(transaction);
  });
  it('updates a transaction without type or userId', () => {
    service.update(3, dto).subscribe();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(dto);
    expect(request.request.body).not.toHaveProperty('type');
    expect(request.request.body).not.toHaveProperty('userId');
    request.flush(transaction);
  });
  it('deletes a transaction', () => {
    service.delete(3).subscribe();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
