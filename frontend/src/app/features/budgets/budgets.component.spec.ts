import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { BudgetsComponent } from './budgets.component';
import { Budget } from './budget.models';
import { Category } from '../categories/category.models';

const url = `${API_BASE_URL}/budgets`;
const categoryUrl = `${API_BASE_URL}/categories`;
const expense: Category = { id: 1, name: 'Transport', type: 'EXPENSE' };
const income: Category = { id: 2, name: 'Salaire', type: 'INCOME' };
const budget: Budget = { id: 3, categoryId: 1, categoryName: 'Transport', year: 2026, month: 9, amount: 300, spent: 250, remaining: 50, usagePercent: 83.33 };

describe('BudgetsComponent', () => {
  let fixture: ComponentFixture<BudgetsComponent>;
  let component: BudgetsComponent;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const listRequest = () => http.expectOne(req => req.url === url && req.method === 'GET');
  const categoryRequest = () => http.expectOne(req => req.url === categoryUrl && req.params.get('type') === 'EXPENSE');
  const load = (items: Budget[] = [budget], categories: Category[] = [expense]) => {
    categoryRequest().flush(categories);
    listRequest().flush(items);
    fixture.detectChanges();
  };
  const validForm = () => { component.openCreate(); component.form.patchValue({ categoryId: 1, amount: 300.1234, year: 2026, month: 9 }); };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BudgetsComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(BudgetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });
  it('loads the current browser period and shows loading before the response', () => {
    const today = new Date();
    const request = listRequest();
    expect(request.request.params.get('year')).toBe(String(today.getFullYear()));
    expect(request.request.params.get('month')).toBe(String(today.getMonth() + 1));
    expect(text()).toContain('Chargement des budgets');
    expect(component.years).toEqual(Array.from({ length: 5 }, (_, index) => today.getFullYear() - 3 + index));
    categoryRequest().flush([expense]);
    request.flush([budget]);
    fixture.detectChanges();
    expect(text()).not.toContain('Chargement des budgets');
  });
  it('displays backend amounts and accessible normal progress', () => {
    load();
    expect(text()).toContain('Transport');
    expect(text()).toContain('300,00 unités');
    expect(text()).toContain('250,00 unités');
    expect(text()).toContain('50,00 unités');
    expect(text()).toContain('83,33 %');
    const progress = fixture.nativeElement.querySelector('[role=progressbar]');
    expect(progress.getAttribute('aria-valuenow')).toBe('83.33');
    expect(progress.getAttribute('aria-valuetext')).toBe('83,33 %');
    expect(progress.firstElementChild.style.width).toBe('83.33%');
    expect(text()).not.toContain('Budget dépassé');
  });
  it('caps progress while keeping the server percentage and negative remaining', () => {
    load([{ ...budget, spent: 375, remaining: -75, usagePercent: 125 }]);
    expect(text()).toContain('125 %');
    expect(text()).toContain('-75,00 unités');
    expect(text()).toContain('Budget dépassé');
    const progress = fixture.nativeElement.querySelector('[role=progressbar]');
    expect(progress.getAttribute('aria-valuenow')).toBe('100');
    expect(progress.getAttribute('aria-valuetext')).toBe('125 %');
    expect(progress.firstElementChild.style.width).toBe('100%');
    expect(fixture.nativeElement.querySelector('.over-budget')).toBeTruthy();
  });
  it('shows an empty state and opens a form using the selected period', () => {
    load([]);
    expect(text()).toContain('Aucun budget défini pour cette période.');
    fixture.nativeElement.querySelector('.page-heading button').click();
    fixture.detectChanges();
    expect(component.showForm()).toBe(true);
    expect(component.form.controls.year.value).toBe(component.filters.controls.year.value);
    expect(component.form.controls.month.value).toBe(component.filters.controls.month.value);
  });
  it.each(['month', 'year'] as const)('reloads when the %s selector changes', field => {
    load();
    const select: HTMLSelectElement = fixture.nativeElement.querySelector(`#filter-${field}`);
    select.selectedIndex = field === 'month' ? (select.selectedIndex + 1) % 12 : 0;
    const selected = select.selectedOptions[0].textContent;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(text()).toContain('Chargement des budgets');
    const request = listRequest();
    expect(request.request.params.get(field)).toBe(String(component.filters.controls[field].value));
    if (field === 'year') expect(request.request.params.get(field)).toBe(selected?.trim());
    request.flush([]);
  });
  it('combines category with period filters and adapts the empty state', () => {
    load();
    component.filters.controls.categoryId.setValue(1);
    const request = listRequest();
    expect(request.request.params.get('categoryId')).toBe('1');
    expect(request.request.params.get('year')).toBe(String(component.filters.controls.year.value));
    expect(request.request.params.get('month')).toBe(String(component.filters.controls.month.value));
    request.flush([]);
    fixture.detectChanges();
    expect(text()).toContain('Aucun budget pour cette catégorie et cette période.');
  });
  it('cancels obsolete period requests and skips unchanged selections', () => {
    load();
    component.filters.controls.month.setValue(1);
    const previous = listRequest();
    component.filters.controls.month.setValue(2);
    expect(previous.cancelled).toBe(true);
    listRequest().flush([budget]);
    component.filters.controls.month.setValue(2);
    http.expectNone(req => req.url === url);
  });
  it('offers only expense categories and rejects an income category', () => {
    load([], [expense, income]);
    validForm();
    fixture.detectChanges();
    const options = Array.from(fixture.nativeElement.querySelectorAll('#budget-category option')) as HTMLOptionElement[];
    expect(options.map(option => option.textContent)).toEqual(['Choisir une catégorie', 'Transport']);
    expect(component.categories()).toEqual([expense]);
    component.form.controls.categoryId.setValue(2);
    component.submit();
    expect(component.form.controls.categoryId.invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it('links to categories and disables saving when no expense category exists', () => {
    load([], [income]);
    component.openCreate();
    fixture.detectChanges();
    expect(text()).toContain('Aucune catégorie de dépense disponible.');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/categories');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
  it('creates using an exact DTO, blocks double submission and follows a different period', () => {
    load([]);
    component.filters.controls.categoryId.setValue(1);
    listRequest().flush([]);
    validForm();
    component.form.patchValue({ year: 2000, month: 1 });
    component.submit();
    component.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body).toEqual({ categoryId: 1, year: 2000, month: 1, amount: 300.1234 });
    request.flush({ ...budget, year: 2000, month: 1 });
    const reload = listRequest();
    expect(reload.request.params.get('year')).toBe('2000');
    expect(reload.request.params.get('month')).toBe('1');
    expect(reload.request.params.has('categoryId')).toBe(false);
    reload.flush([]);
    fixture.detectChanges();
    expect(text()).toContain('Budget créé.');
    expect(component.showForm()).toBe(false);
    expect(component.form.controls.amount.value).toBeNull();
    expect(fixture.nativeElement.querySelector('#filter-year').selectedOptions[0].textContent.trim()).toBe('2000');
  });
  it.each([null, 0, -1, 0.00001, 1e15, Infinity])('rejects invalid amount %s', amount => {
    load(); validForm(); component.form.controls.amount.setValue(amount); component.submit(); fixture.detectChanges();
    expect(component.form.controls.amount.invalid).toBe(true);
    expect(fixture.nativeElement.querySelector('#budget-amount').getAttribute('aria-invalid')).toBe('true');
    http.expectNone(req => req.method === 'POST');
  });
  it('reloads once when saving the selected period and can return to it after another save', () => {
    load();
    const original = component.filters.getRawValue();
    component.openCreate(); component.form.patchValue({ categoryId: 1, amount: 200 }); component.submit();
    http.expectOne(req => req.method === 'POST').flush({ ...budget, year: original.year, month: original.month });
    listRequest().flush([budget]);
    component.openCreate(); component.form.patchValue({ categoryId: 1, amount: 200, year: 2000, month: 1 }); component.submit();
    http.expectOne(req => req.method === 'POST').flush({ ...budget, year: 2000, month: 1 });
    listRequest().flush([]);
    component.filters.setValue(original);
    const request = listRequest();
    expect(request.request.params.get('year')).toBe(String(original.year));
    expect(request.request.params.get('month')).toBe(String(original.month));
    request.flush([budget]);
  });
  it.each([['year', 1999], ['year', 2101], ['year', 2026.5], ['month', 0], ['month', 13], ['month', 1.5]] as const)('rejects invalid %s %s', (field, value) => {
    load(); validForm(); component.form.controls[field].setValue(value); component.submit();
    expect(component.form.controls[field].invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it('requires a category', () => {
    load(); validForm(); component.form.controls.categoryId.setValue(null); component.submit();
    expect(component.form.controls.categoryId.invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it.each(['create', 'update'] as const)('shows a duplicate 409 for %s and preserves the form', operation => {
    load();
    if (operation === 'create') validForm(); else component.openEdit(budget);
    component.submit();
    http.expectOne(req => req.method === (operation === 'create' ? 'POST' : 'PUT')).flush({ message: 'internal' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(text()).toContain('Un budget existe déjà pour cette catégorie et cette période.');
    expect(text()).not.toContain('internal');
    expect(component.showForm()).toBe(true);
    expect(component.saving()).toBe(false);
  });
  it('prefills and updates then loads calculated fields from the server in the new period', () => {
    load(); component.openEdit(budget);
    expect(component.form.getRawValue()).toEqual({ categoryId: 1, year: 2026, month: 9, amount: 300 });
    component.form.patchValue({ amount: 400, year: 2027, month: 2 }); component.submit();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ categoryId: 1, year: 2027, month: 2, amount: 400 });
    request.flush({ ...budget, year: 2027, month: 2, amount: 400 });
    const reload = listRequest();
    expect(reload.request.params.get('year')).toBe('2027');
    expect(reload.request.params.get('month')).toBe('2');
    reload.flush([{ ...budget, amount: 400, spent: 10, remaining: 390, usagePercent: 2.5 }]);
    fixture.detectChanges();
    expect(text()).toContain('Budget mis à jour.');
    expect(text()).toContain('390,00 unités');
    expect(text()).toContain('2,5 %');
  });
  it('requires explicit deletion confirmation and supports cancellation', () => {
    load(); component.askDelete(budget); fixture.detectChanges();
    expect(text()).toContain('Supprimer ce budget ?');
    http.expectNone(`${url}/3`);
    component.cancelDelete(); component.confirmDelete(); http.expectNone(`${url}/3`);
  });
  it('deletes once after confirmation and refreshes after 204', () => {
    load(); component.askDelete(budget); component.confirmDelete(); component.confirmDelete();
    fixture.detectChanges();
    expect(text()).toContain('Suppression…');
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    listRequest().flush([]); fixture.detectChanges();
    expect(text()).toContain('Budget supprimé.');
    expect(component.pendingDelete()).toBeNull();
    expect(component.deleting()).toBe(false);
  });
  it('shows a safe saving error and allows retry', () => {
    load(); validForm(); component.submit();
    http.expectOne(req => req.method === 'POST').flush({ message: 'internal' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain("Impossible d'enregistrer le budget.");
    expect(text()).not.toContain('internal');
    expect(component.saving()).toBe(false);
    component.submit(); http.expectOne(req => req.method === 'POST').flush(budget); listRequest().flush([budget]);
  });
  it('shows the known category type error without exposing arbitrary backend details', () => {
    load(); validForm(); component.submit();
    http.expectOne(req => req.method === 'POST').flush({ message: 'Un budget doit utiliser une catégorie de dépense.' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges(); expect(text()).toContain('Un budget doit utiliser une catégorie de dépense.');
  });
  it('keeps confirmation open after a deletion failure', () => {
    load(); component.askDelete(budget); component.confirmDelete();
    http.expectOne(`${url}/3`).flush({}, { status: 500, statusText: 'Server Error' }); fixture.detectChanges();
    expect(text()).toContain('Impossible de supprimer le budget.');
    expect(component.pendingDelete()).toEqual(budget); expect(component.deleting()).toBe(false);
  });
  it('retries the list after a loading error', () => {
    categoryRequest().flush([expense]); listRequest().flush({}, { status: 500, statusText: 'Server Error' }); fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les budgets.');
    component.load(); listRequest().flush([budget]); fixture.detectChanges(); expect(text()).not.toContain('Impossible de charger');
  });
  it('handles category loading failures separately and supports retry', () => {
    categoryRequest().flush({}, { status: 500, statusText: 'Server Error' }); listRequest().flush([]); fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les catégories.');
    component.loadCategories(); categoryRequest().flush([expense]); fixture.detectChanges(); expect(text()).not.toContain('Impossible de charger');
  });
});
