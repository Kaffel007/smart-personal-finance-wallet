import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../app.routes';
import { AuthService, TOKEN_STORAGE_KEY } from './auth.service';
import { UserSummary } from './auth.models';
import { API_BASE_URL } from './api.config';
import { authInterceptor } from './auth.interceptor';
import { LoginComponent } from '../features/auth/login.component';
import { AdminService } from '../features/admin/admin.service';
import { DashboardService } from '../features/dashboard/dashboard.service';
import { summaryFixture } from '../features/dashboard/dashboard.test-data';

const user: UserSummary = { id: 1, firstName: 'Test', lastName: 'Member', email: 'test@example.test', role: 'USER' };
const admin: UserSummary = { ...user, role: 'ADMIN' };
const testToken = 'synthetic-admin-test-token';
const meUrl = `${API_BASE_URL}/auth/me`;

describe('Admin roles, guard and navigation', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: AdminService, useValue: { getStats: () => of({ totalUsers: 1, activeUsers: 1, blockedUsers: 0, adminUsers: 1 }), getUsers: () => of([]) } },
      { provide: DashboardService, useValue: { getSummary: () => of(summaryFixture) } },
    ] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.clear(); });

  const cacheProfile = (profile: UserSummary) => {
    TestBed.inject(AuthService).login({ email: profile.email, password: 'synthetic password' }).subscribe();
    http.expectOne(`${API_BASE_URL}/auth/login`).flush({ accessToken: testToken, tokenType: 'Bearer', expiresInSeconds: 3600, user: profile });
  };
  it('redirects an anonymous admin visit to login without loading any profile or admin API', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin', LoginComponent);
    expect(TestBed.inject(Router).url).toBe('/login'); http.expectNone(meUrl);
  });
  it('denies a cached USER and preserves personal navigation with account access', async () => {
    cacheProfile(user);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/admin'); harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/dashboard');
    const links = Array.from(harness.routeNativeElement!.querySelectorAll('nav a')).map(link => link.getAttribute('href'));
    expect(links).toEqual(['/dashboard', '/transactions', '/categories', '/budgets', '/savings-goals', '/account']);
    expect(harness.routeNativeElement?.querySelector('a[href="/admin"]')).toBeNull(); http.expectNone(meUrl);
  });
  it.each([user, admin])('loads the profile on direct /admin entry for role $role', async profile => {
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    const harness = await RouterTestingHarness.create();
    const navigation = harness.navigateByUrl('/admin');
    const request = await vi.waitFor(() => http.expectOne(meUrl));
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${testToken}`);
    expect(harness.routeNativeElement?.querySelector('app-admin')).toBeFalsy();
    request.flush(profile); await navigation; harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe(profile.role === 'ADMIN' ? '/admin' : '/dashboard');
    http.expectNone(meUrl);
  });
  it('shows only admin navigation and logs out from the reused layout', async () => {
    cacheProfile(admin);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/admin'); harness.detectChanges();
    const navigation = harness.routeNativeElement!.querySelector('nav')!;
    expect(navigation.querySelectorAll('a')).toHaveLength(2);
    expect(navigation.querySelector('a[href="/account"]')?.textContent).toBe('Mon compte');
    expect(navigation.querySelector('a')?.getAttribute('href')).toBe('/admin');
    expect(navigation.querySelector('a')?.getAttribute('aria-current')).toBe('page');
    expect(harness.routeNativeElement?.textContent).toContain('Espace administrateur');
    expect(harness.routeNativeElement?.textContent).not.toMatch(/Transactions|Budgets|Objectifs d'épargne|Tableau de bord/);
    (harness.routeNativeElement!.querySelector('button.logout') as HTMLButtonElement).click();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull(); expect(TestBed.inject(AuthService).currentUser()).toBeNull();
  });
  it.each([user, admin])('redirects login according to /me role $role rather than the login summary', async profile => {
    const harness = await RouterTestingHarness.create();
    const login = await harness.navigateByUrl('/login', LoginComponent);
    login.form.setValue({ email: user.email, password: 'synthetic password' }); login.submit();
    const opposite = profile.role === 'ADMIN' ? user : admin;
    http.expectOne(`${API_BASE_URL}/auth/login`).flush({ accessToken: testToken, tokenType: 'Bearer', expiresInSeconds: 3600, user: opposite });
    http.expectOne(meUrl).flush(profile); await harness.fixture.whenStable(); harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe(profile.role === 'ADMIN' ? '/admin' : '/dashboard');
    expect(TestBed.inject(AuthService).currentUser()?.role).toBe(profile.role); http.expectNone(meUrl);
  });
  it('keeps a logged-in admin out of public guest pages', async () => {
    cacheProfile(admin);
    const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/login');
    expect(TestBed.inject(Router).url).toBe('/admin');
  });
  it('fails closed when the guard cannot load the profile and does not render admin content', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    const harness = await RouterTestingHarness.create(); const navigation = harness.navigateByUrl('/admin');
    const request = await vi.waitFor(() => http.expectOne(meUrl)); request.flush({ message: 'internal' }, { status: 500, statusText: 'Error' });
    await navigation; await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login'); expect(TestBed.inject(AuthService).isAuthenticated()).toBe(false);
    expect(harness.routeNativeElement?.textContent).not.toContain('internal');
  });
  it('leaves protected admin 401 handling to the existing interceptor', async () => {
    cacheProfile(admin); const harness = await RouterTestingHarness.create(); await harness.navigateByUrl('/admin');
    TestBed.inject(HttpClient).get(`${API_BASE_URL}/admin/stats`).subscribe({ error: () => {} });
    const request = http.expectOne(`${API_BASE_URL}/admin/stats`);
    expect(request.request.headers.get('Authorization')).toBe(`Bearer ${testToken}`);
    request.flush({}, { status: 401, statusText: 'Unauthorized' }); await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login'); expect(TestBed.inject(AuthService).currentUser()).toBeNull();
  });
});
