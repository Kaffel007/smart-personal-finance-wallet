import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../../core/api.config';
import { CategoriesComponent } from './categories.component';

const url = `${API_BASE_URL}/categories`;
const category = { id: 1, name: 'Transport', type: 'EXPENSE' as const };
const income = { id: 2, name: 'Salaire', type: 'INCOME' as const };

describe('CategoriesComponent', () => {
  let fixture: ComponentFixture<CategoriesComponent>;
  let component: CategoriesComponent;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const load = (items = [category, income]) => { http.expectOne(url).flush(items); fixture.detectChanges(); };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CategoriesComponent], providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CategoriesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads and displays separate income and expense sections', () => {
    expect(text()).toContain('Chargement des catégories');
    load();
    expect(text()).toContain('Transport');
    expect(text()).toContain('Salaire');
    expect(component.categoriesOfType('EXPENSE')).toEqual([category]);
    expect(component.categoriesOfType('INCOME')).toEqual([income]);
  });
  it('shows an empty state and a creation action', () => {
    load([]);
    expect(text()).toContain('Aucune catégorie créée.');
    fixture.nativeElement.querySelector('.panel button').click();
    fixture.detectChanges();
    expect(text()).toContain('Créer une catégorie');
  });
  it('creates, blocks duplicate submits, reloads and announces success', () => {
    load([]);
    component.openCreate();
    component.form.setValue({ name: '  Transport  ', type: 'EXPENSE' });
    component.submit();
    component.submit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button[type=submit]').disabled).toBe(true);
    const request = http.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Transport', type: 'EXPENSE' });
    request.flush(category);
    load([category]);
    expect(component.showForm()).toBe(false);
    expect(text()).toContain('Catégorie créée.');
  });
  it.each(['', '   ', 'a'.repeat(101)])('rejects invalid category names', name => {
    load([]);
    component.openCreate();
    component.form.controls.name.setValue(name);
    component.submit();
    fixture.detectChanges();
    expect(component.form.invalid).toBe(true);
    expect(fixture.nativeElement.querySelector('#category-name').getAttribute('aria-invalid')).toBe('true');
    http.expectNone(url);
  });
  it('prefills and updates both name and type', () => {
    load();
    component.openEdit(category);
    expect(component.form.getRawValue()).toEqual({ name: 'Transport', type: 'EXPENSE' });
    component.form.setValue({ name: 'Prime', type: 'INCOME' });
    component.submit();
    const request = http.expectOne(`${url}/1`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Prime', type: 'INCOME' });
    request.flush({ ...category, name: 'Prime', type: 'INCOME' });
    load([]);
    expect(text()).toContain('Catégorie mise à jour.');
  });
  it('requires confirmation and cancellation sends no DELETE', () => {
    load();
    component.askDelete(category);
    fixture.detectChanges();
    expect(text()).toContain('Supprimer la catégorie Transport ?');
    http.expectNone(`${url}/1`);
    component.cancelDelete();
    expect(component.pendingDelete()).toBeNull();
    http.expectNone(`${url}/1`);
  });
  it('deletes only after confirmation and reloads the list', () => {
    load();
    component.askDelete(category);
    component.confirmDelete();
    component.confirmDelete();
    fixture.detectChanges();
    expect(component.deleting()).toBe(true);
    const request = http.expectOne(`${url}/1`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    load([income]);
    expect(text()).toContain('Catégorie supprimée.');
    expect(component.pendingDelete()).toBeNull();
  });
  it('maps duplicate 409 to a clear local message', () => {
    load();
    component.openCreate();
    component.form.setValue({ name: category.name, type: category.type });
    component.submit();
    http.expectOne(url).flush({ message: 'Cette catégorie existe déjà.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(text()).toContain('Une catégorie de ce nom et de ce type existe déjà.');
    expect(component.showForm()).toBe(true);
    expect(component.saving()).toBe(false);
  });
  it.each(['update', 'delete'])('handles a used category 409 on %s without hiding the operation', action => {
    load();
    if (action === 'update') {
      component.openEdit(category);
      component.form.controls.type.setValue('INCOME');
      component.submit();
    } else {
      component.askDelete(category);
      component.confirmDelete();
    }
    http.expectOne(`${url}/1`).flush({ message: 'Cette catégorie est utilisée par des données financières.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(text()).toContain('Cette catégorie est utilisée et ne peut pas être supprimée ou changer de type.');
    expect(text()).toContain('Son nom peut être modifié.');
    expect(component.busy()).toBe(false);
  });
  it('allows renaming a category without changing its type', () => {
    load();
    component.openEdit(category);
    component.form.controls.name.setValue('Transport public');
    component.submit();
    const request = http.expectOne(`${url}/1`);
    expect(request.request.body).toEqual({ name: 'Transport public', type: 'EXPENSE' });
    request.flush({ ...category, name: 'Transport public' });
    load();
  });
  it('maps a 404 to a safe message', () => {
    load();
    component.askDelete(category);
    component.confirmDelete();
    http.expectOne(`${url}/1`).flush({ message: 'private detail' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(text()).toContain('Cette catégorie est introuvable.');
    expect(text()).not.toContain('private detail');
  });
  it('handles a general submission failure without technical details', () => {
    load();
    component.openEdit(category);
    component.submit();
    http.expectOne(`${url}/1`).flush({ message: 'stack trace' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain("Impossible d'enregistrer la catégorie.");
    expect(text()).not.toContain('stack trace');
    expect(component.saving()).toBe(false);
  });
  it('shows a list error and can retry', () => {
    http.expectOne(url).flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain('Impossible de charger les catégories.');
    component.load();
    load();
    expect(text()).not.toContain('Impossible de charger');
  });
});
