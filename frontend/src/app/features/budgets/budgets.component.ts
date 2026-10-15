import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, finalize, of, skip, startWith, Subject, switchMap } from 'rxjs';
import { Category } from '../categories/category.models';
import { CategoryService } from '../categories/category.service';
import { FinancialAmountPipe } from '../dashboard/financial-amount.pipe';
import { amountValidator } from '../transactions/transaction.validators';
import { Budget, CreateBudgetRequest } from './budget.models';
import { BudgetService } from './budget.service';

const integerValidator = (control: AbstractControl) => Number.isInteger(control.value) ? null : { integer: true };

@Component({
  selector: 'app-budgets',
  imports: [ReactiveFormsModule, RouterLink, FinancialAmountPipe],
  templateUrl: './budgets.component.html',
  styleUrls: ['../../shared/finance-ui.css', './budgets.component.css'],
})
export class BudgetsComponent {
  private readonly service = inject(BudgetService);
  private readonly categoryService = inject(CategoryService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly builder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private readonly categoryReload = new Subject<void>();
  private readonly currentDate = new Date();
  private readonly currentYear = Math.min(2100, Math.max(2000, this.currentDate.getFullYear()));
  readonly years = Array.from({ length: Math.min(2100, this.currentYear + 1) - Math.max(2000, this.currentYear - 3) + 1 },
    (_, index) => Math.max(2000, this.currentYear - 3) + index);
  readonly months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
  readonly budgets = signal<Budget[]>([]);
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
  readonly editing = signal<Budget | null>(null);
  readonly pendingDelete = signal<Budget | null>(null);
  readonly filters = this.builder.group({
    year: this.builder.nonNullable.control(this.currentYear),
    month: this.builder.nonNullable.control(this.currentDate.getMonth() + 1),
    categoryId: this.builder.control<number | null>(null),
  });
  get filterYears(): number[] { return [...new Set([...this.years, this.filters.controls.year.value])].sort((a, b) => a - b); }
  readonly form = this.builder.group({
    categoryId: this.builder.control<number | null>(null, [Validators.required, Validators.min(1), integerValidator]),
    year: this.builder.nonNullable.control(this.currentYear, [Validators.required, Validators.min(2000), Validators.max(2100), integerValidator]),
    month: this.builder.nonNullable.control(this.currentDate.getMonth() + 1, [Validators.required, Validators.min(1), Validators.max(12), integerValidator]),
    amount: this.builder.control<number | null>(null, [Validators.required, amountValidator]),
  });
  private readonly percentFormatter = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });

  constructor() {
    this.filters.valueChanges.pipe(startWith(this.filters.getRawValue()), distinctUntilChanged((a, b) => a.year === b.year && a.month === b.month && a.categoryId === b.categoryId), skip(1), takeUntilDestroyed())
      .subscribe(() => { this.pendingDelete.set(null); this.load(); });
    this.categoryReload.pipe(startWith(undefined), switchMap(() => {
      this.categoriesLoading.set(true);
      this.categoriesError.set('');
      return this.categoryService.getAll('EXPENSE').pipe(catchError(() => {
        this.categoriesError.set('Impossible de charger les catégories.');
        return of([] as Category[]);
      }));
    }), takeUntilDestroyed()).subscribe(categories => {
      this.categories.set(categories.filter(category => category.type === 'EXPENSE'));
      this.categoriesLoading.set(false);
    });
    this.reload.pipe(startWith(undefined), switchMap(() => {
      this.loading.set(true);
      this.listError.set('');
      this.budgets.set([]);
      const { year, month, categoryId } = this.filters.getRawValue();
      return this.service.getAll({ year, month, categoryId: categoryId ?? undefined }).pipe(catchError(() => {
        this.listError.set('Impossible de charger les budgets.');
        return of([] as Budget[]);
      }));
    }), takeUntilDestroyed()).subscribe(budgets => {
      this.budgets.set(budgets);
      this.loading.set(false);
    });
  }
  load(): void { this.reload.next(); }
  loadCategories(): void { this.categoryReload.next(); }
  openCreate(): void {
    if (this.busy()) return;
    this.editing.set(null);
    this.pendingDelete.set(null);
    const { year, month } = this.filters.getRawValue();
    this.form.reset({ categoryId: null, year, month, amount: null });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  openEdit(budget: Budget): void {
    if (this.busy()) return;
    this.editing.set(budget);
    this.pendingDelete.set(null);
    this.form.reset({ categoryId: budget.categoryId, year: budget.year, month: budget.month, amount: budget.amount });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  cancelForm(): void { if (!this.busy()) { this.showForm.set(false); this.actionError.set(''); } }
  submit(): void {
    if (this.busy() || this.categoriesLoading() || this.categoriesError()) return;
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (!this.categories().some(category => category.id === value.categoryId)) this.form.controls.categoryId.setErrors({ unavailable: true });
    if (this.form.invalid || value.categoryId === null || value.amount === null) return;
    const request: CreateBudgetRequest = { categoryId: value.categoryId, year: value.year, month: value.month, amount: value.amount };
    const editing = this.editing();
    this.saving.set(true);
    this.actionError.set('');
    const operation = editing ? this.service.update(editing.id, request) : this.service.create(request);
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: budget => {
        this.showForm.set(false);
        this.editing.set(null);
        this.form.reset();
        this.feedback.set(editing ? 'Budget mis à jour.' : 'Budget créé.');
        // Follow the saved period and clear the optional category filter so the budget is visible.
        const previous = this.filters.getRawValue();
        this.filters.setValue({ year: budget.year, month: budget.month, categoryId: null });
        if (previous.year === budget.year && previous.month === budget.month && previous.categoryId === null) this.load();
      },
      error: error => this.actionError.set(this.errorMessage(error, "Impossible d'enregistrer le budget.")),
    });
  }
  askDelete(budget: Budget): void {
    if (this.busy()) return;
    this.showForm.set(false);
    this.actionError.set('');
    this.feedback.set('');
    this.pendingDelete.set(budget);
  }
  cancelDelete(): void { if (!this.busy()) { this.pendingDelete.set(null); this.actionError.set(''); } }
  confirmDelete(): void {
    const budget = this.pendingDelete();
    if (!budget || this.busy()) return;
    this.deleting.set(true);
    this.actionError.set('');
    this.service.delete(budget.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.deleting.set(false))).subscribe({
      next: () => { this.pendingDelete.set(null); this.feedback.set('Budget supprimé.'); this.load(); },
      error: error => this.actionError.set(this.errorMessage(error, 'Impossible de supprimer le budget.')),
    });
  }
  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 409) return 'Un budget existe déjà pour cette catégorie et cette période.';
      if (error.status === 404) return 'Budget ou catégorie introuvable. Actualisez les listes.';
      if (error.status === 400 && error.error?.message === 'Un budget doit utiliser une catégorie de dépense.') return 'Un budget doit utiliser une catégorie de dépense.';
      if (error.status === 400) return 'Vérifiez la catégorie, la période et le montant.';
    }
    return fallback;
  }
  progressWidth(percent: number): number { return Math.min(100, Math.max(0, percent)); }
  formatPercent(percent: number): string { return `${this.percentFormatter.format(percent)} %`; }
}
