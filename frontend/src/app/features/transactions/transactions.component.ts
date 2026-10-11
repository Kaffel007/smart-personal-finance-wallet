import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { Category, CategoryType } from '../categories/category.models';
import { CategoryService } from '../categories/category.service';
import { FinancialAmountPipe } from '../dashboard/financial-amount.pipe';
import { FinancialTransaction, CreateFinancialTransactionRequest } from './transaction.models';
import { TransactionService } from './transaction.service';
import { amountValidator, browserToday, transactionDateValidator } from './transaction.validators';

@Component({
  selector: 'app-transactions',
  imports: [ReactiveFormsModule, RouterLink, FinancialAmountPipe],
  templateUrl: './transactions.component.html',
  styleUrl: '../../shared/finance-ui.css',
})
export class TransactionsComponent {
  private readonly service = inject(TransactionService);
  private readonly categoryService = inject(CategoryService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  private readonly categoryReload = new Subject<void>();
  readonly transactions = signal<FinancialTransaction[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly categoriesLoading = signal(true);
  readonly categoriesError = signal('');
  readonly listError = signal('');
  readonly actionError = signal('');
  readonly feedback = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly busy = computed(() => this.saving() || this.deleting());
  readonly showForm = signal(false);
  readonly editing = signal<FinancialTransaction | null>(null);
  readonly pendingDelete = signal<FinancialTransaction | null>(null);
  readonly selectedType = signal<CategoryType>('EXPENSE');
  readonly categoryOptions = computed(() => this.categories().filter(category => category.type === this.selectedType()));
  private readonly builder = inject(FormBuilder);
  readonly filters = this.builder.group({
    type: this.builder.nonNullable.control<CategoryType | ''>(''),
    categoryId: this.builder.control<number | null>(null),
  });
  readonly form = this.builder.group({
    type: this.builder.nonNullable.control<CategoryType>('EXPENSE', Validators.required),
    categoryId: this.builder.control<number | null>(null, [Validators.required, Validators.min(1)]),
    amount: this.builder.control<number | null>(null, [Validators.required, amountValidator]),
    transactionDate: this.builder.nonNullable.control(browserToday(), [Validators.required, transactionDateValidator]),
    description: this.builder.nonNullable.control('', Validators.maxLength(255)),
  });
  get today(): string { return browserToday(); }
  get hasFilters(): boolean { return !!this.filters.controls.type.value || this.filters.controls.categoryId.value !== null; }

  constructor() {
    this.form.controls.type.valueChanges.pipe(takeUntilDestroyed()).subscribe(type => {
      this.selectedType.set(type);
      this.form.controls.categoryId.reset();
    });
    this.filters.valueChanges.pipe(distinctUntilChanged((a, b) => a.type === b.type && a.categoryId === b.categoryId), takeUntilDestroyed())
      .subscribe(() => { this.pendingDelete.set(null); this.load(); });
    this.categoryReload.pipe(startWith(undefined), switchMap(() => {
      this.categoriesLoading.set(true);
      this.categoriesError.set('');
      return this.categoryService.getAll().pipe(catchError(() => {
        this.categoriesError.set('Impossible de charger les catégories.');
        return of([] as Category[]);
      }));
    }), takeUntilDestroyed()).subscribe(categories => {
      this.categories.set(categories);
      this.categoriesLoading.set(false);
    });
    this.reload.pipe(startWith(undefined), switchMap(() => {
      this.loading.set(true);
      this.listError.set('');
      this.transactions.set([]);
      const { type, categoryId } = this.filters.getRawValue();
      return this.service.getAll({ type: type || undefined, categoryId: categoryId ?? undefined }).pipe(catchError(() => {
        this.listError.set('Impossible de charger les transactions.');
        return of([] as FinancialTransaction[]);
      }));
    }), takeUntilDestroyed()).subscribe(transactions => {
      this.transactions.set(transactions);
      this.loading.set(false);
    });
  }
  load(): void { this.reload.next(); }
  loadCategories(): void { this.categoryReload.next(); }
  resetFilters(): void { if (!this.busy() && this.hasFilters) this.filters.reset({ type: '', categoryId: null }); }
  openCreate(): void {
    if (this.busy()) return;
    this.editing.set(null);
    this.pendingDelete.set(null);
    this.form.reset({ type: 'EXPENSE', categoryId: null, amount: null, transactionDate: browserToday(), description: '' });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  openEdit(transaction: FinancialTransaction): void {
    if (this.busy()) return;
    this.editing.set(transaction);
    this.pendingDelete.set(null);
    this.form.reset({ type: transaction.type, categoryId: transaction.categoryId, amount: transaction.amount,
      transactionDate: transaction.transactionDate, description: transaction.description ?? '' });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  cancelForm(): void { if (!this.busy()) { this.showForm.set(false); this.actionError.set(''); } }
  submit(): void {
    if (this.busy() || this.categoriesLoading() || this.categoriesError()) return;
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (!this.categoryOptions().some(category => category.id === value.categoryId)) {
      this.form.controls.categoryId.setErrors({ unavailable: true });
    }
    // Refresh date validation if the page remained open across midnight.
    this.form.controls.transactionDate.updateValueAndValidity();
    if (this.form.invalid || value.categoryId === null || value.amount === null) return;
    const request: CreateFinancialTransactionRequest = {
      categoryId: value.categoryId, amount: value.amount, transactionDate: value.transactionDate,
      description: value.description.trim() || null,
    };
    const editing = this.editing();
    this.saving.set(true);
    this.actionError.set('');
    const operation = editing ? this.service.update(editing.id, request) : this.service.create(request);
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.showForm.set(false);
        this.feedback.set(editing ? 'Transaction mise à jour.' : 'Transaction créée.');
        this.load();
      },
      error: error => this.actionError.set(this.errorMessage(error, "Impossible d'enregistrer la transaction.")),
    });
  }
  askDelete(transaction: FinancialTransaction): void {
    if (this.busy()) return;
    this.showForm.set(false);
    this.actionError.set('');
    this.feedback.set('');
    this.pendingDelete.set(transaction);
  }
  cancelDelete(): void { if (!this.busy()) { this.pendingDelete.set(null); this.actionError.set(''); } }
  confirmDelete(): void {
    const transaction = this.pendingDelete();
    if (!transaction || this.busy()) return;
    this.deleting.set(true);
    this.actionError.set('');
    this.service.delete(transaction.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.deleting.set(false))).subscribe({
      next: () => {
        this.pendingDelete.set(null);
        this.feedback.set('Transaction supprimée.');
        this.load();
      },
      error: error => this.actionError.set(this.errorMessage(error, 'Impossible de supprimer la transaction.')),
    });
  }
  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.status === 404) return 'Transaction ou catégorie introuvable. Actualisez les listes.';
    if (error instanceof HttpErrorResponse && error.status === 400) return 'Vérifiez la catégorie, le montant, la date et la description.';
    return fallback;
  }
  formatDate(value: string): string { return value.split('-').reverse().join('/'); }
}
