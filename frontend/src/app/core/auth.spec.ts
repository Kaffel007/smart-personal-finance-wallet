import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FormControl } from '@angular/forms';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { API_BASE_URL } from './api.config';
import { AuthService, TOKEN_STORAGE_KEY } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { passwordValidator } from './auth.validators';
import { routes } from '../app.routes';
import { LoginComponent } from '../features/auth/login.component';
import { RegisterComponent } from '../features/auth/register.component';
import { of } from 'rxjs';
import { DashboardService } from '../features/dashboard/dashboard.service';
import { summaryFixture } from '../features/dashboard/dashboard.test-data';

const user = { id: 1, firstName: 'Test', lastName: 'User', email: 'test@example.test', role: 'USER' as const };
const testToken = 'synthetic-test-token';

describe('Authentication HTTP contract', () => {
  let http: HttpTestingController;
  let auth: AuthService;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });
  afterEach(() => { http.verify(); localStorage.clear(); });

  it('stores only the access token after login and loads the profile with Bearer', () => {
    auth.login({ email: user.email, password: 'synthetic password' }).subscribe();
    const login = http.expectOne(`${API_BASE_URL}/auth/login`);
    expect(login.request.method).toBe('POST');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ accessToken: testToken, tokenType: 'Bearer', expiresInSeconds: 3600, user });
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe(testToken);
    expect(localStorage.length).toBe(1);
    auth.getCurrentUser().subscribe();
    const profile = http.expectOne(`${API_BASE_URL}/auth/me`);
    expect(profile.request.headers.get('Authorization')).toBe(`Bearer ${testToken}`);
    profile.flush(user);
    expect(auth.currentUser()).toEqual(user);
  });

  it('does not send a token to registration or another origin', () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    auth.register({ ...user, password: 'synthetic password' }).subscribe();
    const registration = http.expectOne(`${API_BASE_URL}/auth/register`);
    expect(registration.request.headers.has('Authorization')).toBe(false);
    registration.flush(user);
    TestBed.inject(HttpClient).get('https://example.test/api/private').subscribe();
    const external = http.expectOne('https://example.test/api/private');
    expect(external.request.headers.has('Authorization')).toBe(false);
    external.flush({});
  });

  it('omits Authorization without a token', () => {
    auth.getCurrentUser().subscribe();
    const request = http.expectOne(`${API_BASE_URL}/auth/me`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush(user);
  });

  it('clears the session and redirects after a protected 401', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    auth.getCurrentUser().subscribe({ error: () => {} });
    http.expectOne(`${API_BASE_URL}/auth/me`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.isAuthenticated()).toBe(false);
    expect(auth.currentUser()).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/login');
  });

  it('does not redirect on a public login 401', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl');
    auth.login({ email: user.email, password: 'synthetic password' }).subscribe({ error: () => {} });
    http.expectOne(`${API_BASE_URL}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(navigate).not.toHaveBeenCalled();
  });

  it('logout removes the token and profile', () => {
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    auth.logout();
    expect(auth.getToken()).toBeNull();
    expect(auth.currentUser()).toBeNull();
  });
});

describe('Password policy matches backend Unicode limits', () => {
  it('requires 12 Unicode code points', () => {
    expect(passwordValidator(new FormControl('a'.repeat(11)))).not.toBeNull();
    expect(passwordValidator(new FormControl('a'.repeat(12)))).toBeNull();
    expect(passwordValidator(new FormControl('😀'.repeat(6)))).not.toBeNull();
    expect(passwordValidator(new FormControl('😀'.repeat(12)))).toBeNull();
  });
  it('limits UTF-8 length to 72 bytes and rejects blank passwords', () => {
    expect(passwordValidator(new FormControl('é'.repeat(36)))).toBeNull();
    expect(passwordValidator(new FormControl('é'.repeat(37)))).not.toBeNull();
    expect(passwordValidator(new FormControl(' '.repeat(12)))).not.toBeNull();
  });
});

describe('Private routes and forms', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
      { provide: DashboardService, useValue: { getSummary: () => of(summaryFixture) } },
    ] });
  });
  afterEach(() => { TestBed.inject(HttpTestingController).verify(); localStorage.clear(); });

  it('redirects an anonymous dashboard visit to login', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard', LoginComponent);
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it('redirects the root and unknown URLs coherently', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', LoginComponent);
    await harness.navigateByUrl('/unknown', LoginComponent);
    expect(TestBed.inject(Router).url).toBe('/login');
  });
  it('loads only the profile for an authenticated dashboard visit', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, testToken);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dashboard');
    TestBed.inject(HttpTestingController).expectOne(`${API_BASE_URL}/auth/me`).flush(user);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Bienvenue Test');
    expect(harness.routeNativeElement?.textContent).toContain('Tableau de bord');
  });
  it('blocks invalid login submissions', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/login', LoginComponent);
    component.submit();
    expect(component.form.invalid).toBe(true);
    TestBed.inject(HttpTestingController).expectNone(`${API_BASE_URL}/auth/login`);
  });
  it('logs in, fetches the current user and displays the private dashboard', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/login', LoginComponent);
    component.form.setValue({ email: user.email, password: 'synthetic password' });
    component.submit();
    expect(component.loading()).toBe(true);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${API_BASE_URL}/auth/login`).flush({ accessToken: testToken, tokenType: 'Bearer', expiresInSeconds: 3600, user });
    http.expectOne(`${API_BASE_URL}/auth/me`).flush(user);
    await harness.fixture.whenStable();
    http.expectOne(`${API_BASE_URL}/auth/me`).flush(user);
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toBe('/dashboard');
    expect(harness.routeNativeElement?.textContent).toContain('Bienvenue Test');
  });
  it('renders a generic login error and allows another attempt', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/login', LoginComponent);
    component.form.setValue({ email: user.email, password: 'synthetic password' });
    component.submit();
    TestBed.inject(HttpTestingController).expectOne(`${API_BASE_URL}/auth/login`).flush({ message: 'internal detail' }, { status: 401, statusText: 'Unauthorized' });
    expect(component.loading()).toBe(false);
    expect(component.error()).toContain('Connexion impossible');
    expect(component.error()).not.toContain('internal detail');
    expect(component.form.controls.password.value).toBe('');
  });
  it('registers then redirects to login without storing credentials', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/register', RegisterComponent);
    component.form.setValue({ firstName: 'Test', lastName: 'User', email: user.email, password: 'synthetic password' });
    component.submit();
    TestBed.inject(HttpTestingController).expectOne(`${API_BASE_URL}/auth/register`).flush(user);
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(localStorage.length).toBe(0);
  });
});
