import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { nonBlankValidator } from '../../core/auth.validators';
import { Category, CategoryType } from './category.models';
import { CategoryService } from './category.service';
import { categoryErrorMessage } from './category.errors';

@Component({
  selector: 'app-categories',
  imports: [ReactiveFormsModule],
  templateUrl: './categories.component.html',
  styleUrl: '../../shared/finance-ui.css',
})
export class CategoriesComponent {
  private readonly service = inject(CategoryService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly listError = signal('');
  readonly actionError = signal('');
  readonly feedback = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly busy = computed(() => this.saving() || this.deleting());
  readonly showForm = signal(false);
  readonly editing = signal<Category | null>(null);
  readonly pendingDelete = signal<Category | null>(null);
  readonly sections: { type: CategoryType; label: string }[] = [
    { type: 'INCOME', label: 'Revenus' }, { type: 'EXPENSE', label: 'Dépenses' },
  ];
  readonly form = inject(FormBuilder).nonNullable.group({
    name: ['', [Validators.required, nonBlankValidator, Validators.maxLength(100)]],
    type: ['EXPENSE' as CategoryType, Validators.required],
  });

  constructor() {
    this.reload.pipe(startWith(undefined), switchMap(() => {
      this.loading.set(true);
      this.listError.set('');
      this.categories.set([]);
      return this.service.getAll().pipe(catchError(() => {
        this.listError.set('Impossible de charger les catégories.');
        return of([] as Category[]);
      }));
    }), takeUntilDestroyed()).subscribe(categories => {
      this.categories.set(categories);
      this.loading.set(false);
    });
  }
  load(): void { this.reload.next(); }
  categoriesOfType(type: CategoryType): Category[] { return this.categories().filter(category => category.type === type); }
  openCreate(): void {
    if (this.busy()) return;
    this.editing.set(null);
    this.pendingDelete.set(null);
    this.form.reset({ name: '', type: 'EXPENSE' });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  openEdit(category: Category): void {
    if (this.busy()) return;
    this.editing.set(category);
    this.pendingDelete.set(null);
    this.form.reset({ name: category.name, type: category.type });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  cancelForm(): void { if (!this.busy()) { this.showForm.set(false); this.actionError.set(''); } }
  submit(): void {
    if (this.busy()) return;
    this.form.controls.name.setValue(this.form.controls.name.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    this.actionError.set('');
    const editing = this.editing();
    const request = this.form.getRawValue();
    const operation = editing ? this.service.update(editing.id, request) : this.service.create(request);
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.showForm.set(false);
        this.feedback.set(editing ? 'Catégorie mise à jour.' : 'Catégorie créée.');
        this.load();
      },
      error: error => this.actionError.set(categoryErrorMessage(error, "Impossible d'enregistrer la catégorie.")),
    });
  }
  askDelete(category: Category): void {
    if (this.busy()) return;
    this.showForm.set(false);
    this.actionError.set('');
    this.feedback.set('');
    this.pendingDelete.set(category);
  }
  cancelDelete(): void { if (!this.busy()) { this.pendingDelete.set(null); this.actionError.set(''); } }
  confirmDelete(): void {
    const category = this.pendingDelete();
    if (!category || this.busy()) return;
    this.deleting.set(true);
    this.actionError.set('');
    this.service.delete(category.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.deleting.set(false))).subscribe({
      next: () => {
        this.pendingDelete.set(null);
        this.feedback.set('Catégorie supprimée.');
        this.load();
      },
      error: error => this.actionError.set(categoryErrorMessage(error, 'Impossible de supprimer la catégorie.')),
    });
  }
}
