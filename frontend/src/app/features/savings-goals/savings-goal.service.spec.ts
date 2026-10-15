import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { SavingsGoalService } from './savings-goal.service';

const url = `${API_BASE_URL}/savings-goals`;
const dto = { name: 'Voiture', targetAmount: 30000.1234, savedAmount: 8000, targetDate: '2027-06-30' };
const goal = { id: 3, ...dto, remainingAmount: 22000.1234, progressPercent: 26.67 };

describe('SavingsGoalService', () => {
  let service: SavingsGoalService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SavingsGoalService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('gets the list without filters', () => {
    service.getAll().subscribe(value => expect(value).toEqual([goal]));
    const request = http.expectOne(url);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]);
    request.flush([goal]);
  });
  it('gets one goal', () => {
    service.getOne(3).subscribe(value => expect(value).toEqual(goal));
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('GET');
    request.flush(goal);
  });
  it.each(['create', 'update'] as const)('sends only writable fields on %s', operation => {
    const source = { ...goal, userId: 99, createdAt: 'internal', updatedAt: 'internal' };
    (operation === 'create' ? service.create(source) : service.update(3, source)).subscribe(value => expect(value).toEqual(goal));
    const request = http.expectOne(operation === 'create' ? url : `${url}/3`);
    expect(request.request.method).toBe(operation === 'create' ? 'POST' : 'PUT');
    expect(request.request.body).toEqual(dto);
    expect(Object.keys(request.request.body).sort()).toEqual(['name', 'savedAmount', 'targetAmount', 'targetDate']);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(goal);
  });
  it.each([undefined, null])('allows optional savedAmount and targetDate in creation: %s', savedAmount => {
    service.create({ name: 'Voyage', targetAmount: 1000, savedAmount }).subscribe();
    const request = http.expectOne(url);
    const serialized = JSON.parse(request.request.serializeBody() as string);
    expect(serialized).toEqual(savedAmount === undefined
      ? { name: 'Voyage', targetAmount: 1000 }
      : { name: 'Voyage', targetAmount: 1000, savedAmount: null });
    request.flush({ ...goal, savedAmount: 0, targetDate: null });
  });
  it('deletes a goal', () => {
    service.delete(3).subscribe();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
