import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../../app.routes';
import { authGuard } from '../../core/auth.guard';
import { AuthService, TOKEN_STORAGE_KEY } from '../../core/auth.service';
import { authInterceptor } from '../../core/auth.interceptor';
import { API_BASE_URL } from '../../core/api.config';
import { AdminService } from '../admin/admin.service';
import { DashboardService } from '../dashboard/dashboard.service';
import { summaryFixture } from '../dashboard/dashboard.test-data';
import { LoginComponent } from '../auth/login.component';
import { Account } from './account.models';

const url = `${API_BASE_URL}/account`;
const account: Account = { id: 1, firstName: 'Sara', lastName: 'Martin', email: 'sara@example.test', role: 'USER', enabled: true, blocked: false, createdAt: '2026-10-15T12:00:00Z' };
const token = 'synthetic-account-navigation-token';

describe('Account route and shared navigation', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: DashboardService, useValue: { getSummary: () => of(summaryFixture) } },
      { provide: AdminService, useValue: { getStats: () => of({ totalUsers: 1, activeUsers: 1, blockedUsers: 0, adminUsers: 1 }), getUsers: () => of([]) } },
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.clear(); });
  it('redirects anonymous account visits to login', async () => {
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/account', LoginComponent);
    expect(TestBed.inject(Router).url).toBe('/login'); http.expectNone(`${url}/me`);
  });
  it('uses only authGuard on the shared account route', () => {
    const route = routes.find(route => route.path === '')!.children!.find(route => route.path === 'account')!;
    expect(route.canActivate).toEqual([authGuard]);
  });
  it.each(['USER', 'ADMIN'] as const)('navigates to the same account component for %s and updates its header without reconnecting', async role => {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    const profile = { ...account, role };
    TestBed.inject(AuthService).syncCurrentUser(profile);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl(role === 'ADMIN' ? '/admin' : '/dashboard'); harness.detectChanges();
    const links = Array.from(harness.routeNativeElement!.querySelectorAll('nav a')).map(link => link.getAttribute('href'));
    expect(links).toEqual(role === 'ADMIN' ? ['/admin', '/account'] : ['/dashboard', '/transactions', '/categories', '/budgets', '/savings-goals', '/account']);
    const link = harness.routeNativeElement!.querySelector('nav a[href="/account"]') as HTMLAnchorElement;
    expect(link.textContent).toBe('Mon compte'); link.click(); await harness.fixture.whenStable();
    const request = http.expectOne(`${url}/me`); expect(request.request.headers.get('Authorization')).toBe(`Bearer ${token}`); request.flush(profile);
    harness.detectChanges(); expect(TestBed.inject(Router).url).toBe('/account');
    expect(harness.routeNativeElement!.querySelector('app-account')).toBeTruthy(); expect(link.getAttribute('aria-current')).toBe('page');
    const input = harness.routeNativeElement!.querySelector('#account-first-name') as HTMLInputElement;
    input.value = 'Updated'; input.dispatchEvent(new Event('input'));
    harness.routeNativeElement!.querySelector('app-account form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const update = http.expectOne(`${url}/profile`); expect(update.request.body).toEqual({ firstName: 'Updated', lastName: 'Martin' });
    update.flush({ ...profile, firstName: 'Updated' }); harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.workspace-header')!.textContent).toContain('Updated');
    expect(TestBed.inject(AuthService).getToken()).toBe(token); http.expectNone(`${API_BASE_URL}/auth/login`);
    http.expectNone(`${API_BASE_URL}/auth/me`);
  });
  it.each(['USER', 'ADMIN'] as const)('supports direct /account reload for %s with the existing profile-loading layout', async role => {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/account');
    http.expectOne(`${API_BASE_URL}/auth/me`).flush({ ...account, role }); harness.detectChanges();
    http.expectOne(`${url}/me`).flush({ ...account, role }); harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/account'); expect(harness.routeNativeElement!.querySelector('app-account')).toBeTruthy();
    expect(harness.routeNativeElement!.textContent).toContain(role === 'ADMIN' ? 'Administrateur' : 'Utilisateur');
  });
  it('keeps the existing interceptor behavior for expired account sessions', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, token); TestBed.inject(AuthService).syncCurrentUser(account);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/account');
    http.expectOne(`${url}/me`).flush({}, { status: 401, statusText: 'Unauthorized' }); await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login'); expect(TestBed.inject(AuthService).currentUser()).toBeNull();
  });
});
