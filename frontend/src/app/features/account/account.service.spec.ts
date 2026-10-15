import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { AccountService } from './account.service';

const url = `${API_BASE_URL}/account`;
const account = { id: 1, firstName: 'Sara', lastName: 'Martin', email: 'sara@example.test', role: 'USER', enabled: true, blocked: false, createdAt: '2026-10-15T12:00:00Z' };

describe('AccountService', () => {
  let service: AccountService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AccountService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('gets the authenticated account without a configurable target', () => {
    service.getMe().subscribe(value => expect(value).toEqual(account));
    const request = http.expectOne(`${url}/me`); expect(request.request.method).toBe('GET');
    expect(request.request.params.keys()).toEqual([]); expect(request.request.headers.has('Authorization')).toBe(false); request.flush(account);
  });
  it('updates only profile fields even from an object containing account data', () => {
    service.updateProfile({ ...account, userId: 99 } as typeof account).subscribe(value => expect(value).toEqual(account));
    const request = http.expectOne(`${url}/profile`); expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ firstName: 'Sara', lastName: 'Martin' });
    expect(request.request.headers.has('Authorization')).toBe(false); request.flush(account);
  });
  it('updates email with only the new email and current password', () => {
    const source = { newEmail: 'new@example.test', currentPassword: 'synthetic password', ...account, userId: 99 };
    service.updateEmail(source).subscribe();
    const request = http.expectOne(`${url}/email`); expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ newEmail: source.newEmail, currentPassword: source.currentPassword });
    expect(request.request.headers.has('Authorization')).toBe(false); request.flush(account);
  });
  it('changes password without confirmation or target fields and accepts 204', () => {
    const source = { currentPassword: 'synthetic password', newPassword: 'synthetic new password', confirmation: 'synthetic new password', userId: 99 };
    service.changePassword(source).subscribe();
    const request = http.expectOne(`${url}/password`); expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ currentPassword: source.currentPassword, newPassword: source.newPassword });
    expect(request.request.headers.has('Authorization')).toBe(false); request.flush(null, { status: 204, statusText: 'No Content' });
  });
});
