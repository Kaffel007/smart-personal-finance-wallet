import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, startWith, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { DashboardSummary } from './dashboard-summary.model';
import { DashboardService } from './dashboard.service';
import { FinancialAmountPipe } from './financial-amount.pipe';

interface DashboardPeriod { year: number; month: number; }

@Component({
  selector: 'app-dashboard',
  imports: [FinancialAmountPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
  private readonly dashboard = inject(DashboardService);
  private readonly currentDate = new Date();
  readonly year = signal(Math.min(2100, Math.max(2000, this.currentDate.getFullYear())));
  readonly month = signal(this.currentDate.getMonth() + 1);
  readonly years = Array.from(
    { length: Math.min(2100, this.year() + 1) - Math.max(2000, this.year() - 3) + 1 },
    (_, index) => Math.max(2000, this.year() - 3) + index,
  );
  readonly months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
    'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly summary = signal<DashboardSummary | null>(null);
  private readonly reload = new Subject<DashboardPeriod>();
  private readonly percentFormatter = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });

  constructor() {
    this.reload.pipe(
      startWith({ year: this.year(), month: this.month() }),
      switchMap(period => {
        this.summary.set(null);
        this.error.set(false);
        this.loading.set(true);
        return this.dashboard.getSummary(period.year, period.month).pipe(
          catchError(() => of(null)),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe(summary => {
      this.summary.set(summary);
      this.error.set(summary === null);
      this.loading.set(false);
    });
  }

  changeMonth(event: Event): void {
    this.changePeriod(this.year(), Number((event.target as HTMLSelectElement).value));
  }
  changeYear(event: Event): void {
    this.changePeriod(Number((event.target as HTMLSelectElement).value), this.month());
  }
  private changePeriod(year: number, month: number): void {
    if (!Number.isInteger(year) || year < 2000 || year > 2100
      || !Number.isInteger(month) || month < 1 || month > 12) return;
    if (year === this.year() && month === this.month()) return;
    this.year.set(year);
    this.month.set(month);
    this.retry();
  }
  retry(): void { this.reload.next({ year: this.year(), month: this.month() }); }
  progressWidth(percent: number): number { return Math.min(100, Math.max(0, percent)); }
  formatPercent(percent: number): string { return `${this.percentFormatter.format(percent)} %`; }
}
