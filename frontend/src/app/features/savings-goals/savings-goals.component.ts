import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { FinancialAmountPipe } from '../dashboard/financial-amount.pipe';
import { amountValidator } from '../transactions/transaction.validators';
import { SavingsGoal } from './savings-goal.models';
import { SavingsGoalService } from './savings-goal.service';
import { nonBlankNameValidator, savedAmountValidator, targetDateValidator } from './savings-goal.validators';

@Component({
  selector: 'app-savings-goals',
  imports: [ReactiveFormsModule, FinancialAmountPipe],
  templateUrl: './savings-goals.component.html',
  styleUrls: ['../../shared/finance-ui.css', './savings-goals.component.css'],
})
export class SavingsGoalsComponent {
  private readonly service = inject(SavingsGoalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly builder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private readonly percentFormatter = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
  readonly goals = signal<SavingsGoal[]>([]);
  readonly loading = signal(true);
  readonly listError = signal('');
  readonly actionError = signal('');
  readonly feedback = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly busy = computed(() => this.saving() || this.deleting());
  readonly showForm = signal(false);
  readonly editing = signal<SavingsGoal | null>(null);
  readonly pendingDelete = signal<SavingsGoal | null>(null);
  readonly form = this.builder.group({
    name: this.builder.nonNullable.control('', [Validators.required, nonBlankNameValidator, Validators.maxLength(120)]),
    targetAmount: this.builder.control<number | null>(null, [Validators.required, amountValidator]),
    savedAmount: this.builder.control<number | null>(0, savedAmountValidator),
    targetDate: this.builder.nonNullable.control('', targetDateValidator),
  });

  constructor() {
    this.reload.pipe(startWith(undefined), switchMap(() => {
      this.loading.set(true);
      this.listError.set('');
      this.goals.set([]);
      return this.service.getAll().pipe(catchError(() => {
        this.listError.set("Impossible de charger les objectifs d'épargne.");
        return of([] as SavingsGoal[]);
      }));
    }), takeUntilDestroyed()).subscribe(goals => {
      this.goals.set(goals);
      this.loading.set(false);
    });
  }
  load(): void { this.reload.next(); }
  openCreate(): void {
    if (this.busy()) return;
    this.editing.set(null);
    this.pendingDelete.set(null);
    this.form.controls.savedAmount.setValidators(savedAmountValidator);
    this.form.reset({ name: '', targetAmount: null, savedAmount: 0, targetDate: '' });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  openEdit(goal: SavingsGoal): void {
    if (this.busy()) return;
    this.editing.set(goal);
    this.pendingDelete.set(null);
    this.form.controls.savedAmount.setValidators([Validators.required, savedAmountValidator]);
    this.form.reset({ name: goal.name, targetAmount: goal.targetAmount, savedAmount: goal.savedAmount, targetDate: goal.targetDate ?? '' });
    this.actionError.set('');
    this.feedback.set('');
    this.showForm.set(true);
  }
  cancelForm(): void { if (!this.busy()) { this.showForm.set(false); this.actionError.set(''); } }
  submit(): void {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const value = this.form.getRawValue();
    if (value.targetAmount === null) return;
    const request = { name: value.name.trim(), targetAmount: value.targetAmount, savedAmount: value.savedAmount ?? 0, targetDate: value.targetDate || null };
    const editing = this.editing();
    this.saving.set(true);
    this.actionError.set('');
    const operation = editing ? this.service.update(editing.id, request) : this.service.create(request);
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.showForm.set(false);
        this.editing.set(null);
        this.form.reset({ name: '', targetAmount: null, savedAmount: 0, targetDate: '' });
        this.feedback.set(editing ? 'Objectif mis à jour.' : 'Objectif créé.');
        this.load();
      },
      error: error => this.actionError.set(this.errorMessage(error, "Impossible d'enregistrer l'objectif.")),
    });
  }
  askDelete(goal: SavingsGoal): void {
    if (this.busy()) return;
    this.showForm.set(false);
    this.actionError.set('');
    this.feedback.set('');
    this.pendingDelete.set(goal);
  }
  cancelDelete(): void { if (!this.busy()) { this.pendingDelete.set(null); this.actionError.set(''); } }
  confirmDelete(): void {
    const goal = this.pendingDelete();
    if (!goal || this.busy()) return;
    this.deleting.set(true);
    this.actionError.set('');
    this.service.delete(goal.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.deleting.set(false))).subscribe({
      next: () => { this.pendingDelete.set(null); this.feedback.set('Objectif supprimé.'); this.load(); },
      error: error => this.actionError.set(this.errorMessage(error, "Impossible de supprimer l'objectif.")),
    });
  }
  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse && error.status === 404) return 'Objectif introuvable. Actualisez la liste.';
    if (error instanceof HttpErrorResponse && error.status === 400) return 'Vérifiez le nom, les montants et la date cible.';
    return fallback;
  }
  progressWidth(percent: number): number { return Math.min(100, Math.max(0, percent)); }
  formatPercent(percent: number): string { return `${this.percentFormatter.format(percent)} %`; }
  formatDate(value: string): string { return value.split('-').reverse().join('/'); }
}
