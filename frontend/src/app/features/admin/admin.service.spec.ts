import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { AdminService } from './admin.service';

const url = `${API_BASE_URL}/admin`;
const user = { id: 2, firstName: 'Sara', lastName: 'Martin', email: 'sara@example.test', role: 'USER', enabled: true, blocked: false, createdAt: '2026-10-15T12:00:00Z' };
const stats = { totalUsers: 3, activeUsers: 2, blockedUsers: 1, adminUsers: 1 };

describe('AdminService', () => {
  let service: AdminService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('gets account statistics', () => {
    service.getStats().subscribe(value => expect(value).toEqual(stats));
    const request = http.expectOne(`${url}/stats`);
    expect(request.request.method).toBe('GET'); request.flush(stats);
  });
  it.each([undefined, '', '   ', ' Sara & Martin '])('gets users with normalized optional search %s', search => {
    service.getUsers(search).subscribe(value => expect(value).toEqual([user]));
    const request = http.expectOne(req => req.url === `${url}/users`);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual(search?.trim() ? ['search'] : []);
    expect(request.request.params.get('search')).toBe(search?.trim() || null);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush([user]);
  });
  it('gets one account', () => {
    service.getUser(2).subscribe(value => expect(value).toEqual(user));
    const request = http.expectOne(`${url}/users/2`);
    expect(request.request.method).toBe('GET'); request.flush(user);
  });
  it.each(['block', 'unblock'] as const)('uses PUT %s without account fields or manual authorization', action => {
    (action === 'block' ? service.blockUser(2) : service.unblockUser(2)).subscribe(value => expect(value).toEqual(user));
    const request = http.expectOne(`${url}/users/2/${action}`);
    expect(request.request.method).toBe('PUT'); expect(request.request.body).toBeNull();
    expect(request.request.headers.has('Authorization')).toBe(false); request.flush(user);
  });
});
