import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../../core/auth.service';
import { API_BASE_URL } from '../../core/api.config';
import { AdminComponent } from './admin.component';
import { AdminStats, AdminUser } from './admin.models';

const url = `${API_BASE_URL}/admin`;
const admin: AdminUser = { id: 1, firstName: 'Admin', lastName: 'Platform', email: 'admin@example.test', role: 'ADMIN', enabled: true, blocked: false, createdAt: '2026-10-15T12:00:00Z' };
const active: AdminUser = { ...admin, id: 2, firstName: 'Sara', lastName: 'Martin', email: 'sara@example.test', role: 'USER' };
const blocked: AdminUser = { ...active, id: 3, firstName: 'Ali', email: 'ali@example.test', blocked: true };
const stats: AdminStats = { totalUsers: 3, activeUsers: 2, blockedUsers: 1, adminUsers: 1 };

describe('AdminComponent', () => {
  let fixture: ComponentFixture<AdminComponent>;
  let component: AdminComponent;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const usersRequest = () => http.expectOne(req => req.url === `${url}/users`);
  const statsRequest = () => http.expectOne(`${url}/stats`);
  const load = (users = [admin, active, blocked]) => {
    statsRequest().flush(stats); usersRequest().flush(users); fixture.detectChanges();
  };
  const finishMutation = (users = [admin, { ...active, blocked: true }, blocked]) => {
    usersRequest().flush(users); statsRequest().flush({ ...stats, activeUsers: 1, blockedUsers: 2 }); fixture.detectChanges();
  };
  const cards = (): HTMLElement[] => Array.from(fixture.nativeElement.querySelectorAll('.account-card'));
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: [provideHttpClient(), provideHttpClientTesting(),
      { provide: AuthService, useValue: { currentUser: signal(admin) } },
    ] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminComponent); component = fixture.componentInstance; fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads statistics independently of users and displays all backend counters', () => {
    expect(text()).toContain('Chargement des statistiques'); expect(text()).toContain('Chargement des utilisateurs');
    statsRequest().flush(stats); fixture.detectChanges();
    const values = Array.from(fixture.nativeElement.querySelectorAll('.stats-grid dd')) as HTMLElement[];
    expect(values.map(value => value.textContent)).toEqual(['3', '2', '1', '1']);
    expect(text()).toContain('Comptes actifs'); expect(text()).toContain('Comptes bloqués'); expect(text()).toContain('Administrateurs');
    expect(text()).toContain('Chargement des utilisateurs');
    usersRequest().flush([admin]); fixture.detectChanges(); expect(text()).not.toContain('Chargement');
  });
  it('can display users while statistics are still loading', () => {
    usersRequest().flush([active]); fixture.detectChanges();
    expect(text()).toContain('Sara Martin'); expect(text()).toContain('Chargement des statistiques'); statsRequest().flush(stats);
  });
  it('shows account identity, roles, status, creation date and eligible actions in backend order', () => {
    load();
    expect(cards()).toHaveLength(3);
    expect(cards()[0].textContent).toContain('ADMIN · Administrateur');
    expect(cards()[0].querySelector('button')).toBeNull();
    expect(cards()[1].textContent).toContain('Sara Martin'); expect(cards()[1].textContent).toContain('sara@example.test');
    expect(cards()[1].textContent).toContain('USER · Utilisateur'); expect(cards()[1].textContent).toContain('Actif');
    expect(cards()[1].textContent).toContain('15/10/2026'); expect(cards()[1].querySelector('button')?.textContent).toBe('Bloquer');
    expect(cards()[2].textContent).toContain('Bloqué'); expect(cards()[2].querySelector('button')?.textContent).toBe('Débloquer');
  });
  it('shows disabled status including accounts that are both disabled and blocked', () => {
    load([{ ...active, enabled: false }, { ...blocked, enabled: false }]);
    expect(cards()[0].textContent).toContain('Désactivé'); expect(cards()[0].textContent).not.toContain('Actif');
    expect(cards()[1].textContent).toContain('Bloqué'); expect(cards()[1].textContent).toContain('Désactivé');
  });
  it('never renders admin block buttons and refuses programmatic actions on admins or self', () => {
    const other = { ...admin, id: 4, blocked: true };
    load([admin, other]);
    expect(fixture.nativeElement.querySelectorAll('.account-card button')).toHaveLength(0);
    component.askBlock(admin); component.askBlock({ ...active, id: admin.id }); component.unblock(other);
    expect(component.pendingBlock()).toBeNull(); http.expectNone(req => req.method === 'PUT');
  });
  it('does not display extra sensitive fields even if a response includes them', () => {
    const extra = { ...active, passwordHash: 'synthetic-sensitive-marker', accessToken: 'synthetic-sensitive-marker', balance: 'synthetic-sensitive-marker' };
    load([extra]);
    expect(text()).not.toContain('synthetic-sensitive-marker');
    expect(text()).not.toMatch(/Transactions|Budgets|Objectifs d'épargne|Revenus|Dépenses|Solde|password|JWT/);
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });
  it('submits search through the backend without requests for individual keystrokes', () => {
    load();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('#admin-search');
    input.value = ' Sara '; input.dispatchEvent(new Event('input'));
    http.expectNone(req => req.url === `${url}/users`);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); fixture.detectChanges();
    expect(text()).toContain('Recherche des utilisateurs'); expect(cards()).toHaveLength(0);
    const request = usersRequest(); expect(request.request.params.get('search')).toBe('Sara');
    request.flush([active]); fixture.detectChanges(); expect(cards()).toHaveLength(1);
    expect(component.searchControl.value).toBe('Sara');
  });
  it('resets search and reloads the full list without a search parameter', () => {
    load(); component.searchControl.setValue('Sara'); component.search(); usersRequest().flush([active]);
    component.resetSearch();
    const request = usersRequest(); expect(request.request.params.keys()).toEqual([]); request.flush([admin, active]);
    expect(component.searchControl.value).toBe(''); expect(component.submittedSearch()).toBe('');
  });
  it('cancels an obsolete search so a late response cannot replace the latest results', () => {
    load(); component.searchControl.setValue('Sara'); component.search(); const previous = usersRequest();
    component.searchControl.setValue('Ali'); component.search(); expect(previous.cancelled).toBe(true);
    usersRequest().flush([blocked]); fixture.detectChanges(); expect(cards()[0].textContent).toContain('Ali');
  });
  it.each(['', 'missing'])('shows an appropriate empty state for search %s', search => {
    load([]);
    if (search) { component.searchControl.setValue(search); component.search(); usersRequest().flush([]); fixture.detectChanges(); }
    expect(text()).toContain(search ? 'Aucun utilisateur ne correspond à votre recherche.' : 'Aucun utilisateur à afficher.');
  });
  it('requires block confirmation and supports cancellation without a request', () => {
    load(); cards()[1].querySelector('button')!.click(); fixture.detectChanges();
    expect(text()).toContain('Bloquer ce compte ?'); expect(text()).toContain("L'utilisateur ne pourra plus se connecter.");
    http.expectNone(req => req.method === 'PUT'); component.cancelBlock(); component.confirmBlock();
    expect(component.pendingBlock()).toBeNull(); http.expectNone(req => req.method === 'PUT');
  });
  it('blocks once, prevents concurrent actions and reloads users plus backend stats', () => {
    load(); component.askBlock(active); fixture.detectChanges();
    fixture.nativeElement.querySelector('.confirmation button.danger').click(); component.confirmBlock(); component.unblock(blocked);
    fixture.detectChanges(); expect(component.busy()).toBe(true); expect(text()).toContain('Blocage…');
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    const request = http.expectOne(`${url}/users/2/block`); expect(request.request.method).toBe('PUT'); request.flush({ ...active, blocked: true });
    expect(component.usersLoading()).toBe(true); expect(component.statsLoading()).toBe(true);
    finishMutation(); expect(text()).toContain('Utilisateur bloqué.'); expect(component.pendingBlock()).toBeNull();
    expect(component.busy()).toBe(false); expect(component.stats()?.blockedUsers).toBe(2);
    expect(fixture.nativeElement.querySelector('.feedback').getAttribute('role')).toBe('status');
  });
  it('unblocks once and refreshes the current submitted search plus statistics', () => {
    load(); component.searchControl.setValue('Ali'); component.search(); usersRequest().flush([blocked]); fixture.detectChanges();
    cards()[0].querySelector('button')!.click(); component.unblock(blocked); fixture.detectChanges();
    expect(text()).toContain('Déblocage…'); expect(component.busy()).toBe(true);
    http.expectOne(`${url}/users/3/unblock`).flush({ ...blocked, blocked: false });
    const reload = usersRequest(); expect(reload.request.params.get('search')).toBe('Ali'); reload.flush([{ ...blocked, blocked: false }]);
    statsRequest().flush({ ...stats, activeUsers: 3, blockedUsers: 0 }); fixture.detectChanges();
    expect(text()).toContain('Utilisateur débloqué.'); expect(component.stats()?.blockedUsers).toBe(0); expect(component.busy()).toBe(false);
  });
  it('retries a statistics failure while retaining the loaded users', () => {
    statsRequest().flush({ message: 'internal' }, { status: 500, statusText: 'Error' }); usersRequest().flush([active]); fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les statistiques.'); expect(text()).toContain('Sara Martin'); expect(text()).not.toContain('internal');
    component.loadStats(); statsRequest().flush(stats); fixture.detectChanges(); expect(text()).not.toContain('Impossible de charger');
  });
  it('retries a users failure independently of statistics', () => {
    statsRequest().flush(stats); usersRequest().flush({ message: 'internal' }, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les utilisateurs.'); expect(fixture.nativeElement.querySelector('.stats-grid')).toBeTruthy();
    component.loadUsers(); usersRequest().flush([active]); fixture.detectChanges(); expect(text()).not.toContain('Impossible de charger');
  });
  it.each(['block', 'unblock'] as const)('handles a failed %s safely and releases the mutation lock', action => {
    load(); if (action === 'block') { component.askBlock(active); component.confirmBlock(); } else component.unblock(blocked);
    http.expectOne(`${url}/users/${action === 'block' ? 2 : 3}/${action}`).flush({ message: 'internal' }, { status: 500, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain(action === 'block' ? 'Impossible de bloquer cet utilisateur.' : 'Impossible de débloquer cet utilisateur.');
    expect(text()).not.toContain('internal'); expect(component.busy()).toBe(false);
    http.expectNone(req => req.url === `${url}/stats` || req.url === `${url}/users`);
  });
  it.each([403, 404, 409])('maps mutation HTTP %s to a local message', status => {
    load(); component.askBlock(active); component.confirmBlock();
    http.expectOne(`${url}/users/2/block`).flush({ message: 'internal' }, { status, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).not.toContain('internal'); expect(component.actionError()).toBeTruthy(); expect(component.busy()).toBe(false);
    expect(component.pendingBlock()).toEqual(active);
  });
  it('associates a visible search label and named action buttons with a responsive card structure', () => {
    load();
    expect(fixture.nativeElement.querySelector('label[for=admin-search]').textContent).toBe('Rechercher par nom ou e-mail');
    expect(fixture.nativeElement.querySelector('ul.account-grid').getAttribute('aria-label')).toBe('Comptes utilisateurs');
    expect(cards()[1].querySelector('button')?.getAttribute('aria-label')).toBe('Bloquer Sara Martin');
    expect(component.formatDate('invalid')).toBe('—');
  });
});
