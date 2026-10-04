import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../core/api.config';
import { DashboardComponent } from './dashboard.component';
import { emptySummaryFixture, summaryFixture } from './dashboard.test-data';

const endpoint = `${API_BASE_URL}/dashboard/summary`;
const formattedAmount = (value: number) => `${new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)} unités`;

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let http: HttpTestingController;
  const text = () => fixture.nativeElement.textContent as string;
  const initialRequest = () => http.expectOne(request => request.url === endpoint);
  const changeSelect = (id: string, value: number) => {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector(`#${id}`);
    select.value = String(value);
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
  });
  afterEach(() => { fixture.destroy(); http.verify(); });

  it('loads automatically using the browser current month and year', () => {
    const now = new Date();
    const request = initialRequest();
    expect(request.request.params.get('year')).toBe(String(now.getFullYear()));
    expect(request.request.params.get('month')).toBe(String(now.getMonth() + 1));
    expect(text()).toContain('Chargement du tableau de bord');
    expect(fixture.nativeElement.querySelector('.dashboard-results').getAttribute('aria-busy')).toBe('true');
    expect(fixture.nativeElement.querySelectorAll('#dashboard-month option').length).toBe(12);
    expect(fixture.componentInstance.years.every(year => year >= 2000 && year <= 2100)).toBe(true);
    const monthSelect: HTMLSelectElement = fixture.nativeElement.querySelector('#dashboard-month');
    const yearSelect: HTMLSelectElement = fixture.nativeElement.querySelector('#dashboard-year');
    expect(monthSelect.value).toBe(String(now.getMonth() + 1));
    expect(yearSelect.value).toBe(String(now.getFullYear()));
    request.flush({ ...summaryFixture, year: now.getFullYear(), month: now.getMonth() + 1 });
    fixture.detectChanges();
    expect(monthSelect.selectedOptions[0].textContent).toBe(fixture.componentInstance.months[now.getMonth()]);
    expect(yearSelect.selectedOptions[0].textContent?.trim()).toBe(String(now.getFullYear()));
    expect(fixture.nativeElement.querySelector('.period-caption').textContent)
      .toBe(`${fixture.componentInstance.months[now.getMonth()]} ${now.getFullYear()} · Revenus, dépenses et budgets du mois`);
  });

  it('renders all amounts, counts and backend percentages without recalculating the balance', () => {
    initialRequest().flush({ ...summaryFixture, balance: 1234.56 });
    fixture.detectChanges();
    for (const [id, amount] of [['income', 3000], ['expense', 500], ['balance', 1234.56]] as const) {
      expect(fixture.nativeElement.querySelector(`[data-testid=${id}]`).textContent).toBe(formattedAmount(amount));
    }
    expect(fixture.nativeElement.querySelector('[data-testid=transactions]').textContent).toBe('2');
    expect(text()).toContain('1 budget(s)');
    expect(text()).toContain('1 objectif(s)');
    expect(text()).toContain(formattedAmount(1000));
    expect(text()).toContain(formattedAmount(2000));
    expect(text()).toContain(formattedAmount(1500));
    expect(text()).toContain('50 %');
    expect(text()).toContain('25 %');
    expect(text()).toContain('toutes périodes confondues');
    const bars = fixture.nativeElement.querySelectorAll('[role=progressbar]');
    expect(bars[0].getAttribute('aria-valuenow')).toBe('50');
    expect(bars[1].getAttribute('aria-valuenow')).toBe('25');
  });

  it('reloads for a changed month and immediately hides previous amounts', () => {
    initialRequest().flush(summaryFixture);
    fixture.detectChanges();
    const month = fixture.componentInstance.month() === 12 ? 1 : fixture.componentInstance.month() + 1;
    changeSelect('dashboard-month', month);
    expect(text()).toContain('Chargement');
    expect(fixture.nativeElement.querySelector('[data-testid=income]')).toBeNull();
    const request = initialRequest();
    expect(request.request.params.get('month')).toBe(String(month));
    const year = fixture.componentInstance.year();
    expect(request.request.params.get('year')).toBe(String(year));
    request.flush({ ...summaryFixture, month, year });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#dashboard-month').value).toBe(String(month));
    expect(fixture.nativeElement.querySelector('#dashboard-year').value).toBe(String(year));
    expect(fixture.nativeElement.querySelector('.period-caption').textContent)
      .toBe(`${fixture.componentInstance.months[month - 1]} ${year} · Revenus, dépenses et budgets du mois`);
  });

  it('reloads for a changed year', () => {
    initialRequest().flush(summaryFixture);
    fixture.detectChanges();
    const year = fixture.componentInstance.years.find(value => value !== fixture.componentInstance.year())!;
    changeSelect('dashboard-year', year);
    const request = initialRequest();
    expect(request.request.params.get('year')).toBe(String(year));
    expect(request.request.params.get('month')).toBe(String(fixture.componentInstance.month()));
    const month = fixture.componentInstance.month();
    request.flush({ ...summaryFixture, year, month });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#dashboard-month').value).toBe(String(month));
    expect(fixture.nativeElement.querySelector('#dashboard-year').value).toBe(String(year));
    expect(fixture.nativeElement.querySelector('.period-caption').textContent)
      .toBe(`${fixture.componentInstance.months[month - 1]} ${year} · Revenus, dépenses et budgets du mois`);
  });

  it('does not reload an unchanged selection', () => {
    initialRequest().flush(summaryFixture);
    fixture.detectChanges();
    changeSelect('dashboard-month', fixture.componentInstance.month());
    changeSelect('dashboard-year', fixture.componentInstance.year());
    http.expectNone(request => request.url === endpoint);
  });

  it('cancels an obsolete request when the period changes rapidly', () => {
    const first = initialRequest();
    const nextMonth = fixture.componentInstance.month() === 12 ? 1 : fixture.componentInstance.month() + 1;
    changeSelect('dashboard-month', nextMonth);
    expect(first.cancelled).toBe(true);
    initialRequest().flush({ ...summaryFixture, month: nextMonth, totalIncome: 900 });
    fixture.detectChanges();
    expect(text()).toContain(formattedAmount(900));
  });

  it('keeps zero cards and displays empty budget/savings states', () => {
    initialRequest().flush(emptySummaryFixture);
    fixture.detectChanges();
    expect(text()).toContain('Aucune donnée financière pour cette période.');
    expect(text()).toContain('Aucun budget défini pour cette période.');
    expect(text()).toContain("Aucun objectif d'épargne pour le moment.");
    expect(fixture.nativeElement.querySelectorAll('.metric-card').length).toBe(4);
    expect(fixture.nativeElement.querySelector('[data-testid=income]').textContent).toBe(formattedAmount(0));
    expect(fixture.nativeElement.querySelector('.neutral')).not.toBeNull();
  });

  it('shows a generic error and retries the same period successfully', () => {
    initialRequest().flush({ message: 'internal failure detail' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(text()).toContain('Impossible de charger le tableau de bord.');
    expect(text()).not.toContain('internal failure detail');
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(text()).toContain('Chargement');
    const retry = initialRequest();
    expect(retry.request.params.get('year')).toBe(String(fixture.componentInstance.year()));
    expect(retry.request.params.get('month')).toBe(String(fixture.componentInstance.month()));
    retry.flush(summaryFixture);
    fixture.detectChanges();
    expect(text()).not.toContain('Impossible de charger');
    expect(text()).toContain(formattedAmount(3000));
  });

  it('shows actual percentages and negative remaining values beyond 100 while clamping visual and ARIA ranges', () => {
    initialRequest().flush({ ...summaryFixture, budgetUsagePercent: 125, budgetRemaining: -250, savingsProgressPercent: 150, savingsRemaining: -1000 });
    fixture.detectChanges();
    expect(text()).toContain('125 %');
    expect(text()).toContain('150 %');
    expect(text()).toContain('Budget dépassé.');
    expect(text()).toContain("Cible globale d'épargne dépassée.");
    expect(text()).toContain(formattedAmount(-250));
    expect(text()).toContain(formattedAmount(-1000));
    const bars = fixture.nativeElement.querySelectorAll('[role=progressbar]');
    expect(bars[0].getAttribute('aria-valuenow')).toBe('100');
    expect(bars[0].getAttribute('aria-valuetext')).toBe('125 %');
    expect(bars[1].getAttribute('aria-valuetext')).toBe('150 %');
    for (const fill of fixture.nativeElement.querySelectorAll('.progress-fill')) expect(fill.style.width).toBe('100%');
  });

  it.each([[-1, 'negative', 'Solde négatif'], [0, 'neutral', 'Solde nul'], [1, 'positive', 'Solde positif']])('distinguishes a balance of %s visually and in text', (balance, cssClass, label) => {
    initialRequest().flush({ ...summaryFixture, balance });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(`.${cssClass} [data-testid=balance]`)).not.toBeNull();
    expect(text()).toContain(label);
  });
});
