import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SavingsGoalsComponent } from './savings-goals.component';
import { SavingsGoal } from './savings-goal.models';
import { API_BASE_URL } from '../../core/api.config';

const url = `${API_BASE_URL}/savings-goals`;
const goal: SavingsGoal = { id: 3, name: 'Voiture', targetAmount: 30000, savedAmount: 8000, remainingAmount: 22000, progressPercent: 26.67, targetDate: '2027-06-30' };

describe('SavingsGoalsComponent', () => {
  let fixture: ComponentFixture<SavingsGoalsComponent>;
  let component: SavingsGoalsComponent;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const listRequest = () => http.expectOne(req => req.url === url && req.method === 'GET');
  const load = (goals: SavingsGoal[] = [goal]) => { listRequest().flush(goals); fixture.detectChanges(); };
  const validForm = () => { component.openCreate(); component.form.patchValue({ name: ' Voyage ', targetAmount: 1000.1234 }); };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [SavingsGoalsComponent], providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SavingsGoalsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('shows initial loading then displays server values and French date', () => {
    expect(text()).toContain("Chargement des objectifs d'épargne");
    load();
    expect(text()).toContain('Voiture');
    expect(text().replace(/\s/g, ' ')).toContain('30 000,00 unités');
    expect(text().replace(/\s/g, ' ')).toContain('8 000,00 unités');
    expect(text().replace(/\s/g, ' ')).toContain('22 000,00 unités');
    expect(text()).toContain('30/06/2027');
    expect(text()).not.toContain('Chargement');
  });
  it('shows empty state and opens a form from its create button', () => {
    load([]);
    expect(text()).toContain("Aucun objectif d'épargne pour le moment.");
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find(button => button.textContent === 'Créer un objectif')!.click();
    fixture.detectChanges();
    expect(component.showForm()).toBe(true);
    expect(component.form.controls.savedAmount.value).toBe(0);
    expect(fixture.nativeElement.querySelector('#goal-saved').value).toBe('0');
  });
  it('uses backend progress without recalculation and exposes accessible values', () => {
    load([{ ...goal, progressPercent: 42.12 }]);
    expect(text()).toContain('42,12 %');
    const progress = fixture.nativeElement.querySelector('[role=progressbar]');
    expect(progress.getAttribute('aria-valuenow')).toBe('42.12');
    expect(progress.getAttribute('aria-valuetext')).toBe('42,12 %');
    expect(progress.firstElementChild.style.width).toBe('42.12%');
    expect(text()).not.toContain('Objectif atteint');
  });
  it.each([100, 120])('marks a goal as achieved at %s percent and caps only visual progress', progressPercent => {
    load([{ ...goal, targetAmount: 1000, savedAmount: progressPercent === 100 ? 1000 : 1200, remainingAmount: progressPercent === 100 ? 0 : -200, progressPercent }]);
    expect(text()).toContain('Objectif atteint');
    expect(text()).toContain(`${progressPercent} %`);
    const progress = fixture.nativeElement.querySelector('[role=progressbar]');
    expect(progress.getAttribute('aria-valuenow')).toBe('100');
    expect(progress.getAttribute('aria-valuetext')).toBe(`${progressPercent} %`);
    expect(progress.firstElementChild.style.width).toBe('100%');
    expect(fixture.nativeElement.querySelector('.achieved')).toBeTruthy();
    if (progressPercent > 100) expect(text()).toContain('-200,00 unités');
  });
  it('displays no target date when absent', () => {
    load([{ ...goal, targetDate: null }]); expect(text()).toContain('Aucune date cible');
  });
  it('creates an exact DTO, blocks double submits, resets and reloads with feedback', () => {
    load([]); validForm();
    fixture.detectChanges();
    const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    component.submit(); fixture.detectChanges();
    expect(component.saving()).toBe(true);
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    expect(text()).toContain('Enregistrement…');
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body).toEqual({ name: 'Voyage', targetAmount: 1000.1234, savedAmount: 0, targetDate: null });
    request.flush(goal);
    expect(component.loading()).toBe(true);
    load();
    expect(component.showForm()).toBe(false);
    expect(component.form.controls.name.value).toBe('');
    expect(component.form.controls.savedAmount.value).toBe(0);
    expect(text()).toContain('Objectif créé.');
    expect(fixture.nativeElement.querySelector('.feedback').getAttribute('role')).toBe('status');
  });
  it('normalizes a blank creation saved amount to zero', () => {
    load(); validForm(); component.form.controls.savedAmount.setValue(null); component.submit();
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body.savedAmount).toBe(0); request.flush(goal); load();
  });
  it.each(['', '   ', 'a'.repeat(121)])('rejects invalid name %s', name => {
    load(); validForm(); component.form.controls.name.setValue(name); component.submit(); fixture.detectChanges();
    expect(component.form.controls.name.invalid).toBe(true);
    expect(fixture.nativeElement.querySelector('#goal-name').getAttribute('aria-invalid')).toBe('true');
    http.expectNone(req => req.method === 'POST');
  });
  it.each([null, 0, -1, 0.00001, 1e15, Infinity])('rejects invalid target %s', targetAmount => {
    load(); validForm(); component.form.controls.targetAmount.setValue(targetAmount); component.submit();
    expect(component.form.controls.targetAmount.invalid).toBe(true); http.expectNone(req => req.method === 'POST');
  });
  it.each([-1, 0.00001, 1e15, Infinity])('rejects invalid saved amount %s', savedAmount => {
    load(); validForm(); component.form.controls.savedAmount.setValue(savedAmount); component.submit();
    expect(component.form.controls.savedAmount.invalid).toBe(true); http.expectNone(req => req.method === 'POST');
  });
  it('accepts four decimals and saved amount above the target', () => {
    load(); validForm(); component.form.controls.savedAmount.setValue(1200.1234); component.submit();
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body.savedAmount).toBe(1200.1234); request.flush(goal); load();
  });
  it.each(['', '2000-01-01', '2099-06-30'])('accepts an optional, past or future target date %s', targetDate => {
    load(); validForm(); component.form.controls.targetDate.setValue(targetDate); component.submit();
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body.targetDate).toBe(targetDate || null); request.flush(goal); load();
  });
  it.each(['2026-02-30', 'invalid', '0000-01-01'])('rejects an invalid calendar date %s', targetDate => {
    load(); validForm(); component.form.controls.targetDate.setValue(targetDate); component.submit();
    expect(component.form.controls.targetDate.invalid).toBe(true); http.expectNone(req => req.method === 'POST');
  });
  it('prefills all fields, updates and uses refreshed computed values', () => {
    load(); component.openEdit(goal); fixture.detectChanges();
    expect(component.form.getRawValue()).toEqual({ name: goal.name, targetAmount: 30000, savedAmount: 8000, targetDate: '2027-06-30' });
    component.form.patchValue({ name: 'Voiture neuve', savedAmount: 9000.1234, targetDate: '' }); component.submit();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Voiture neuve', targetAmount: 30000, savedAmount: 9000.1234, targetDate: null });
    request.flush(goal);
    load([{ ...goal, savedAmount: 9000.1234, remainingAmount: 20999.8766, progressPercent: 30 }]);
    expect(text()).toContain('Objectif mis à jour.'); expect(text()).toContain('30 %');
    expect(component.goals()[0].remainingAmount).toBe(20999.8766);
  });
  it('requires saved amount on update and allows it to be blank again on create', () => {
    load(); component.openEdit({ ...goal, targetDate: null });
    expect(component.form.controls.targetDate.value).toBe('');
    component.form.controls.savedAmount.setValue(null); component.submit();
    expect(component.form.controls.savedAmount.invalid).toBe(true); http.expectNone(req => req.method === 'PUT');
    component.openCreate(); component.form.controls.savedAmount.setValue(null);
    expect(component.form.controls.savedAmount.valid).toBe(true);
  });
  it('cancels the form without a mutation', () => {
    load(); validForm(); component.cancelForm(); expect(component.showForm()).toBe(false);
    http.expectNone(req => req.method === 'POST');
  });
  it('requires explicit confirmation and supports cancellation', () => {
    load(); component.askDelete(goal); fixture.detectChanges();
    expect(text()).toContain('Supprimer cet objectif ?'); expect(text()).toContain('Cette action est définitive.');
    http.expectNone(`${url}/3`); component.cancelDelete(); component.confirmDelete(); http.expectNone(`${url}/3`);
  });
  it('deletes once after explicit confirmation, shows loading and refreshes after 204', () => {
    load();
    fixture.nativeElement.querySelector('button.danger').click(); fixture.detectChanges();
    fixture.nativeElement.querySelector('.confirmation button.danger').click(); component.confirmDelete(); fixture.detectChanges();
    expect(component.deleting()).toBe(true); expect(text()).toContain('Suppression…');
    expect(fixture.nativeElement.querySelector('.confirmation button.danger').disabled).toBe(true);
    const request = http.expectOne(`${url}/3`); expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' }); load([]);
    expect(text()).toContain('Objectif supprimé.'); expect(component.pendingDelete()).toBeNull(); expect(component.deleting()).toBe(false);
  });
  it('shows a safe save failure and preserves values for retry', () => {
    load(); validForm(); component.submit();
    http.expectOne(req => req.method === 'POST').flush({ message: 'internal detail' }, { status: 500, statusText: 'Server Error' }); fixture.detectChanges();
    expect(text()).toContain("Impossible d'enregistrer l'objectif."); expect(text()).not.toContain('internal detail');
    expect(component.showForm()).toBe(true); expect(component.saving()).toBe(false); expect(component.form.controls.name.value).toBe(' Voyage ');
    component.submit(); http.expectOne(req => req.method === 'POST').flush(goal); load();
  });
  it.each([400, 404])('handles save HTTP %s with a local message', status => {
    load(); component.openEdit(goal); component.submit();
    http.expectOne(`${url}/3`).flush({ message: 'internal detail' }, { status, statusText: 'Error' }); fixture.detectChanges();
    expect(text()).toContain(status === 404 ? 'Objectif introuvable.' : 'Vérifiez le nom, les montants et la date cible.');
    expect(text()).not.toContain('internal detail'); expect(component.saving()).toBe(false);
  });
  it('keeps the confirmation open after a deletion failure', () => {
    load(); component.askDelete(goal); component.confirmDelete();
    http.expectOne(`${url}/3`).flush({ message: 'internal' }, { status: 500, statusText: 'Server Error' }); fixture.detectChanges();
    expect(text()).toContain("Impossible de supprimer l'objectif."); expect(text()).not.toContain('internal');
    expect(component.pendingDelete()).toEqual(goal); expect(component.deleting()).toBe(false);
  });
  it('shows a list error then retries via the visible button', () => {
    listRequest().flush({ message: 'internal' }, { status: 500, statusText: 'Server Error' }); fixture.detectChanges();
    expect(text()).toContain("Impossible de charger les objectifs d'épargne."); expect(text()).not.toContain('internal');
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find(button => button.textContent === 'Réessayer les objectifs')!.click(); fixture.detectChanges();
    expect(text()).toContain("Chargement des objectifs d'épargne"); load(); expect(text()).not.toContain('Impossible de charger');
  });
});
