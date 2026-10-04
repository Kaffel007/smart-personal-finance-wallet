import { Pipe, PipeTransform } from '@angular/core';

// No currency is defined by the current API. Keep the display unit in one place.
export const DASHBOARD_AMOUNT_UNIT = 'unités';
const amountFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

@Pipe({ name: 'financialAmount' })
export class FinancialAmountPipe implements PipeTransform {
  transform(value: number): string {
    return `${amountFormatter.format(value)} ${DASHBOARD_AMOUNT_UNIT}`;
  }
}
