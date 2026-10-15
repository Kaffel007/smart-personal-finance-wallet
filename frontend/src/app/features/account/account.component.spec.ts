import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService, TOKEN_STORAGE_KEY } from '../../core/auth.service';
import { API_BASE_URL } from '../../core/api.config';
import { AccountComponent } from './account.component';
import { Account } from './account.models';

const url = `${API_BASE_URL}/account`;
const account: Account = { id: 1, firstName: 'Sara', lastName: 'Martin', email: 'sara@example.test', role: 'USER', enabled: true, blocked: false, createdAt: '2026-10-15T12:00:00Z' };
const currentPassword = 'synthetic current password';
const newPassword = 'synthetic new password';

describe('AccountComponent', () => {
  let fixture: ComponentFixture<AccountComponent>;
  let component: AccountComponent;
  let http: HttpTestingController;
  let auth: AuthService;
  const text = () => fixture.nativeElement.textContent as string;
  const load = (value = account) => { http.expectOne(`${url}/me`).flush(value); fixture.detectChanges(); };
  const validEmail = () => component.emailForm.setValue({ newEmail: ' new@example.test ', currentPassword });
  const validPassword = () => component.passwordForm.setValue({ currentPassword, newPassword, confirmation: newPassword });
  beforeEach(() => {
    localStorage.clear(); localStorage.setItem(TOKEN_STORAGE_KEY, 'synthetic-account-token');
    TestBed.configureTestingModule({ imports: [AccountComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController); auth = TestBed.inject(AuthService);
    fixture = TestBed.createComponent(AccountComponent); component = fixture.componentInstance; fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); localStorage.clear(); });

  it.each(['USER', 'ADMIN'] as const)('loads and displays the same own-account UI for %s', role => {
    expect(text()).toContain('Chargement de votre compte');
    load({ ...account, role });
    expect(text()).toContain('Sara'); expect(text()).toContain('Martin'); expect(text()).toContain('sara@example.test');
    expect(text()).toContain(role === 'ADMIN' ? 'Administrateur' : 'Utilisateur');
    expect(text()).toContain('Actif'); expect(text()).toContain('15/10/2026');
    expect(fixture.nativeElement.querySelectorAll('section')).toHaveLength(4);
    expect(component.profileForm.getRawValue()).toEqual({ firstName: 'Sara', lastName: 'Martin' });
    expect(auth.currentUser()?.role).toBe(role);
  });
  it.each([
    { enabled: true, blocked: true, status: 'Bloqué' },
    { enabled: false, blocked: false, status: 'Désactivé' },
    { enabled: false, blocked: true, status: 'Bloqué · Désactivé' },
  ])('displays read-only status $status', state => {
    load({ ...account, ...state }); expect(text()).toContain(state.status);
  });
  it('offers no target, role or state controls, including for ADMIN, and does not display extra secrets', () => {
    load({ ...account, role: 'ADMIN', passwordHash: 'synthetic-hidden-marker', accessToken: 'synthetic-hidden-marker' } as Account);
    expect(Object.keys(component.profileForm.controls)).toEqual(['firstName', 'lastName']);
    expect(Object.keys(component.emailForm.controls)).toEqual(['newEmail', 'currentPassword']);
    expect(Object.keys(component.passwordForm.controls)).toEqual(['currentPassword', 'newPassword', 'confirmation']);
    expect(fixture.nativeElement.querySelector('select')).toBeNull(); expect(text()).not.toContain('synthetic-hidden-marker');
    expect(fixture.nativeElement.querySelector('input[formControlName=role]')).toBeNull();
  });
  it('saves a trimmed profile once and synchronizes the account and auth summary without changing the session', () => {
    load(); component.profileForm.setValue({ firstName: ' New ', lastName: ' Name ' });
    component.saveProfile(); component.saveProfile(); fixture.detectChanges();
    expect(component.profileSaving()).toBe(true); expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    const request = http.expectOne(`${url}/profile`); expect(request.request.body).toEqual({ firstName: 'New', lastName: 'Name' });
    request.flush({ ...account, firstName: 'New', lastName: 'Name' }); fixture.detectChanges();
    expect(text()).toContain('Profil mis à jour.'); expect(auth.currentUser()?.firstName).toBe('New');
    expect(Object.keys(auth.currentUser()!).sort()).toEqual(['email', 'firstName', 'id', 'lastName', 'role']);
    expect(component.profileSaving()).toBe(false); expect(auth.getToken()).toBe('synthetic-account-token');
  });
  it.each(['', '   ', 'a'.repeat(101)])('rejects invalid first name %s', name => {
    load(); component.profileForm.controls.firstName.setValue(name); component.saveProfile(); fixture.detectChanges();
    expect(component.profileForm.controls.firstName.invalid).toBe(true); http.expectNone(`${url}/profile`);
    expect(fixture.nativeElement.querySelector('#account-first-name').getAttribute('aria-invalid')).toBe('true');
  });
  it.each(['', '  ', 'a'.repeat(101)])('rejects invalid last name %s', name => {
    load(); component.profileForm.controls.lastName.setValue(name); component.saveProfile(); expect(component.profileForm.controls.lastName.invalid).toBe(true); http.expectNone(`${url}/profile`);
  });
  it('shows a safe profile error and permits another submission', () => {
    load(); component.saveProfile(); http.expectOne(`${url}/profile`).flush({ message: 'internal detail' }, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain('Impossible de mettre à jour le profil.'); expect(text()).not.toContain('internal detail'); expect(component.profileSaving()).toBe(false);
    component.saveProfile(); http.expectOne(`${url}/profile`).flush(account);
  });
  it('saves email once, synchronizes auth, clears credentials immediately and keeps the token', () => {
    load(); validEmail(); validPassword(); component.saveEmail(); component.saveEmail(); component.savePassword(); fixture.detectChanges();
    expect(component.emailSaving()).toBe(true); expect(component.passwordSaving()).toBe(false);
    const request = http.expectOne(`${url}/email`); expect(request.request.body).toEqual({ newEmail: 'new@example.test', currentPassword });
    request.flush({ ...account, email: 'new@example.test' });
    expect(component.emailForm.controls.currentPassword.value).toBe(''); expect(component.passwordForm.controls.currentPassword.value).toBe('');
    fixture.detectChanges(); expect(text()).toContain('Adresse e-mail mise à jour.');
    expect(component.account()?.email).toBe('new@example.test'); expect(auth.currentUser()?.email).toBe('new@example.test');
    expect(auth.getToken()).toBe('synthetic-account-token'); expect(component.emailSaving()).toBe(false);
  });
  it.each(['', 'invalid', 'a'.repeat(255) + '@example.test'])('rejects invalid new email %s', email => {
    load(); validEmail(); component.emailForm.controls.newEmail.setValue(email); component.saveEmail(); expect(component.emailForm.controls.newEmail.invalid).toBe(true); http.expectNone(`${url}/email`);
  });
  it.each(['', '  '])('requires a nonblank current password for both credential forms: %s', password => {
    load(); validEmail(); validPassword(); component.emailForm.controls.currentPassword.setValue(password); component.passwordForm.controls.currentPassword.setValue(password);
    component.saveEmail(); component.savePassword(); expect(component.emailForm.invalid).toBe(true); expect(component.passwordForm.invalid).toBe(true); http.expectNone(req => req.method === 'PUT');
  });
  it('handles duplicate email with a local message and clears its password', () => {
    load(); validEmail(); component.saveEmail(); http.expectOne(`${url}/email`).flush({ message: 'internal' }, { status: 409, statusText: 'Conflict' }); fixture.detectChanges();
    expect(text()).toContain('Cette adresse e-mail est déjà utilisée.'); expect(text()).not.toContain('internal');
    expect(component.emailForm.controls.currentPassword.value).toBe(''); expect(component.emailSaving()).toBe(false);
  });
  it.each(['email', 'password'] as const)('maps incorrect current password for %s without logging out', section => {
    load(); validEmail(); validPassword(); if (section === 'email') component.saveEmail(); else component.savePassword();
    http.expectOne(`${url}/${section}`).flush({ message: 'internal', fieldErrors: { currentPassword: 'internal' } }, { status: 400, statusText: 'Bad Request' }); fixture.detectChanges();
    expect(text()).toContain('Le mot de passe actuel est incorrect.'); expect(text()).not.toContain('internal'); expect(auth.getToken()).toBe('synthetic-account-token');
    expect(section === 'email' ? component.emailForm.controls.currentPassword.value : component.passwordForm.controls.currentPassword.value).toBe('');
  });
  it('shows a generic safe email error', () => {
    load(); validEmail(); component.saveEmail(); http.expectOne(`${url}/email`).flush({ message: 'internal' }, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain("Impossible de modifier l'adresse e-mail."); expect(text()).not.toContain('internal'); expect(component.emailSaving()).toBe(false);
  });
  it('submits exactly two password fields, blocks duplicates and clears all passwords after 204', () => {
    load(); validPassword(); validEmail(); fixture.detectChanges();
    const forms: HTMLFormElement[] = Array.from(fixture.nativeElement.querySelectorAll('form'));
    forms[2].dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); component.savePassword(); fixture.detectChanges();
    expect(component.passwordSaving()).toBe(true); expect(text()).toContain('Modification…');
    const request = http.expectOne(`${url}/password`); expect(request.request.body).toEqual({ currentPassword, newPassword });
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(component.passwordForm.getRawValue()).toEqual({ currentPassword: '', newPassword: '', confirmation: '' }); expect(component.emailForm.controls.currentPassword.value).toBe('');
    fixture.detectChanges(); expect(text()).toContain('Mot de passe mis à jour.'); expect(auth.getToken()).toBe('synthetic-account-token'); expect(component.passwordSaving()).toBe(false);
  });
  it.each(['', 'a'.repeat(11), 'a'.repeat(73), '😀'.repeat(6), 'é'.repeat(37)])('rejects a new password outside the existing Unicode/UTF-8 policy: %s', password => {
    load(); validPassword(); component.passwordForm.patchValue({ newPassword: password, confirmation: password }); component.savePassword();
    expect(component.passwordForm.controls.newPassword.invalid).toBe(true); http.expectNone(`${url}/password`);
  });
  it.each(['a'.repeat(12), 'a'.repeat(72), '😀'.repeat(12), 'é'.repeat(36)])('accepts the existing password boundaries: %s', password => {
    load(); validPassword(); component.passwordForm.patchValue({ newPassword: password, confirmation: password }); component.savePassword();
    http.expectOne(`${url}/password`).flush(null, { status: 204, statusText: 'No Content' });
  });
  it.each(['', 'different confirmation'])('requires matching confirmation: %s', confirmation => {
    load(); validPassword(); component.passwordForm.controls.confirmation.setValue(confirmation); component.savePassword(); fixture.detectChanges();
    expect(component.passwordForm.invalid).toBe(true); http.expectNone(`${url}/password`);
    expect(fixture.nativeElement.querySelector('#account-confirm-password').getAttribute('aria-invalid')).toBe('true');
  });
  it('rejects a new password equal to the current password', () => {
    load(); component.passwordForm.setValue({ currentPassword, newPassword: currentPassword, confirmation: currentPassword }); component.savePassword();
    expect(component.passwordForm.hasError('passwordUnchanged')).toBe(true); http.expectNone(`${url}/password`);
  });
  it.each([400, 500])('shows a local password error and resets all password inputs for HTTP %s', status => {
    load(); validPassword(); component.savePassword(); http.expectOne(`${url}/password`).flush({ message: 'internal', fieldErrors: status === 400 ? { newPassword: 'internal' } : {} }, { status, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).not.toContain('internal'); expect(component.passwordError()).toContain(status === 400 ? 'respecter la politique' : 'Impossible de modifier');
    expect(component.passwordForm.getRawValue()).toEqual({ currentPassword: '', newPassword: '', confirmation: '' }); expect(component.passwordSaving()).toBe(false);
  });
  it('retries a failed account load through the visible button', () => {
    http.expectOne(`${url}/me`).flush({ message: 'internal' }, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain('Impossible de charger votre compte.'); expect(text()).not.toContain('internal');
    fixture.nativeElement.querySelector('button').click(); fixture.detectChanges(); expect(text()).toContain('Chargement de votre compte'); load(); expect(text()).toContain('Informations personnelles');
  });
  it('associates all labels and help, uses proper autocomplete and keeps secrets out of page text', () => {
    load(); validEmail(); validPassword(); fixture.detectChanges();
    const inputs = Array.from(fixture.nativeElement.querySelectorAll('input')) as HTMLInputElement[];
    for (const input of inputs) {
      expect(fixture.nativeElement.querySelector(`label[for="${input.id}"]`)).toBeTruthy();
      for (const id of input.getAttribute('aria-describedby')!.split(' ')) expect(fixture.nativeElement.querySelector(`#${id}`)).toBeTruthy();
      expect(input.autocomplete).toBeTruthy();
    }
    expect(fixture.nativeElement.querySelector('#account-current-password').autocomplete).toBe('current-password');
    expect(fixture.nativeElement.querySelector('#account-new-password').autocomplete).toBe('new-password');
    expect(text()).not.toContain(currentPassword); expect(text()).not.toContain(newPassword);
  });
  it('clears credential controls when leaving the page', () => {
    load(); validEmail(); validPassword(); fixture.destroy();
    expect(component.emailForm.controls.currentPassword.value).toBe(''); expect(component.passwordForm.controls.newPassword.value).toBe('');
  });
});
