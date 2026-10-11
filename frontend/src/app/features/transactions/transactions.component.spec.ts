import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { TransactionsComponent } from './transactions.component';
import { FinancialTransaction } from './transaction.models';
import { browserToday } from './transaction.validators';

const url = `${API_BASE_URL}/transactions`;
const categoryUrl = `${API_BASE_URL}/categories`;
const expense = { id: 1, name: 'Transport', type: 'EXPENSE' as const };
const income = { id: 2, name: 'Salaire', type: 'INCOME' as const };
const transaction: FinancialTransaction = { id: 3, categoryId: 1, categoryName: 'Transport', type: 'EXPENSE', amount: 12.5, transactionDate: '2026-01-01', description: null };

describe('TransactionsComponent', () => {
  let fixture: ComponentFixture<TransactionsComponent>;
  let component: TransactionsComponent;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const listRequest = () => http.expectOne(req => req.url === url && req.method === 'GET');
  const load = (items = [transaction], categories = [expense, income]) => {
    http.expectOne(categoryUrl).flush(categories);
    listRequest().flush(items);
    fixture.detectChanges();
  };
  const completeReload = (items = [transaction]) => { listRequest().flush(items); fixture.detectChanges(); };
  const validForm = () => { component.openCreate(); component.form.patchValue({ categoryId: 1, amount: 12.5, transactionDate: browserToday(), description: ' trajet ' }); };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TransactionsComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransactionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads categories and transactions and displays presentation-only signs and server order', () => {
    expect(text()).toContain('Chargement des transactions');
    const revenue = { ...transaction, id: 4, type: 'INCOME' as const, categoryId: 2, categoryName: 'Salaire', amount: 100 };
    load([revenue, transaction]);
    const list = fixture.nativeElement.querySelector('ul[aria-label=Transactions]');
    expect(list.children[0].textContent).toContain('Salaire');
    expect(list.children[1].textContent).toContain('Transport');
    expect(text()).toContain('01/01/2026');
    expect(text()).toContain('—');
    expect(text()).toContain('+ 100,00 unités');
    expect(text()).toContain('− 12,50 unités');
    expect(component.transactions()[1].amount).toBe(12.5);
  });
  it('shows empty state with a create action', () => {
    load([]);
    expect(text()).toContain('Aucune transaction pour le moment.');
    fixture.nativeElement.querySelector('.page-heading button').click();
    fixture.detectChanges();
    expect(component.showForm()).toBe(true);
    expect(component.form.controls.transactionDate.value).toBe(browserToday());
  });
  it('sends combined backend filters and resets them with a single request', () => {
    load();
    component.filters.setValue({ type: 'EXPENSE', categoryId: 1 });
    const filtered = listRequest();
    expect(filtered.request.params.get('type')).toBe('EXPENSE');
    expect(filtered.request.params.get('categoryId')).toBe('1');
    filtered.flush([]);
    fixture.detectChanges();
    expect(text()).toContain('Aucune transaction ne correspond aux filtres.');
    component.resetFilters();
    const reset = listRequest();
    expect(reset.request.params.keys()).toEqual([]);
    reset.flush([transaction]);
    fixture.detectChanges();
    expect(component.hasFilters).toBe(false);
  });
  it('filters categories by visual type and clears the previous category selection', () => {
    load();
    validForm();
    expect(component.categoryOptions()).toEqual([expense]);
    component.form.controls.type.setValue('INCOME');
    fixture.detectChanges();
    expect(component.categoryOptions()).toEqual([income]);
    expect(component.form.controls.categoryId.value).toBeNull();
    const options: HTMLOptionElement[] = Array.from(fixture.nativeElement.querySelectorAll('#transaction-category option'));
    expect(options.map(option => option.textContent)).toEqual(['Choisir une catégorie', 'Salaire']);
  });
  it('links to categories when the selected type has no categories', () => {
    load([], [income]);
    component.openCreate();
    fixture.detectChanges();
    expect(text()).toContain('Aucune catégorie disponible.');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/categories');
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
  });
  it('creates an expense with an exact DTO, resets the form and refreshes without navigation', () => {
    load([]);
    validForm();
    component.submit();
    component.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    const request = http.expectOne(req => req.url === url && req.method === 'POST');
    expect(request.request.body).toEqual({ categoryId: 1, amount: 12.5, transactionDate: browserToday(), description: 'trajet' });
    expect(Object.keys(request.request.body).sort()).toEqual(['amount', 'categoryId', 'description', 'transactionDate']);
    request.flush(transaction);
    completeReload();
    expect(text()).toContain('Transaction créée.');
    expect(component.showForm()).toBe(false);
    expect(component.saving()).toBe(false);
  });
  it('creates an income using its category and never sends type', () => {
    load([]);
    validForm();
    component.form.controls.type.setValue('INCOME');
    component.form.controls.categoryId.setValue(2);
    component.submit();
    const request = http.expectOne(req => req.method === 'POST');
    expect(request.request.body.categoryId).toBe(2);
    expect(request.request.body).not.toHaveProperty('type');
    request.flush({ ...transaction, type: 'INCOME', categoryId: 2 });
    completeReload();
  });
  it.each([null, 0, -1, 0.00001, 1e15])('rejects an invalid amount %s', amount => {
    load();
    validForm();
    component.form.controls.amount.setValue(amount);
    component.submit();
    fixture.detectChanges();
    expect(component.form.controls.amount.invalid).toBe(true);
    expect(fixture.nativeElement.querySelector('#transaction-amount').getAttribute('aria-invalid')).toBe('true');
    http.expectNone(req => req.method === 'POST');
  });
  it.each(['', '2999-01-01', '2026-02-30'])('rejects an invalid or future date %s', date => {
    load();
    validForm();
    component.form.controls.transactionDate.setValue(date);
    component.submit();
    expect(component.form.controls.transactionDate.invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it('rejects a description over 255 characters', () => {
    load();
    validForm();
    component.form.controls.description.setValue('a'.repeat(256));
    component.submit();
    expect(component.form.controls.description.invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it('rejects a category belonging to a different visual type', () => {
    load();
    validForm();
    component.form.controls.categoryId.setValue(2);
    component.submit();
    expect(component.form.controls.categoryId.invalid).toBe(true);
    http.expectNone(req => req.method === 'POST');
  });
  it('prefills all fields and updates without adding internal fields', () => {
    load();
    component.openEdit(transaction);
    fixture.detectChanges();
    expect(component.form.getRawValue()).toEqual({ type: 'EXPENSE', categoryId: 1, amount: 12.5, transactionDate: '2026-01-01', description: '' });
    expect(fixture.nativeElement.querySelector('#transaction-category').selectedOptions[0].textContent).toBe('Transport');
    component.form.controls.amount.setValue(18.1234);
    component.submit();
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ categoryId: 1, amount: 18.1234, transactionDate: '2026-01-01', description: null });
    request.flush({ ...transaction, amount: 18.1234 });
    completeReload();
    expect(text()).toContain('Transaction mise à jour.');
  });
  it('requires deletion confirmation and supports cancellation', () => {
    load();
    component.askDelete(transaction);
    fixture.detectChanges();
    expect(text()).toContain('Supprimer cette transaction ?');
    http.expectNone(`${url}/3`);
    component.cancelDelete();
    component.confirmDelete();
    http.expectNone(`${url}/3`);
  });
  it('deletes after confirmation, blocks duplicate clicks and announces success', () => {
    load();
    component.askDelete(transaction);
    component.confirmDelete();
    component.confirmDelete();
    expect(component.deleting()).toBe(true);
    const request = http.expectOne(`${url}/3`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    completeReload([]);
    expect(text()).toContain('Transaction supprimée.');
    expect(component.pendingDelete()).toBeNull();
  });
  it('shows a safe save failure and preserves the form for retry', () => {
    load();
    validForm();
    component.submit();
    http.expectOne(req => req.method === 'POST').flush({ message: 'internal error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain("Impossible d'enregistrer la transaction.");
    expect(text()).not.toContain('internal error');
    expect(component.saving()).toBe(false);
    expect(component.showForm()).toBe(true);
  });
  it('shows a safe delete failure and keeps the confirmation open', () => {
    load();
    component.askDelete(transaction);
    component.confirmDelete();
    http.expectOne(`${url}/3`).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain('Impossible de supprimer la transaction.');
    expect(component.deleting()).toBe(false);
    expect(component.pendingDelete()).toEqual(transaction);
  });
  it('handles a missing transaction or category', () => {
    load();
    component.openEdit(transaction);
    component.submit();
    http.expectOne(`${url}/3`).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(text()).toContain('Transaction ou catégorie introuvable.');
  });
  it('retries after a list error', () => {
    http.expectOne(categoryUrl).flush([expense]);
    listRequest().flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les transactions.');
    component.load();
    completeReload();
    expect(text()).not.toContain('Impossible de charger');
  });
  it('handles category loading failure separately and allows retry', () => {
    http.expectOne(categoryUrl).flush({}, { status: 500, statusText: 'Server Error' });
    listRequest().flush([]);
    fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les catégories.');
    component.loadCategories();
    http.expectOne(categoryUrl).flush([expense]);
    fixture.detectChanges();
    expect(text()).not.toContain('Impossible de charger');
  });
});
