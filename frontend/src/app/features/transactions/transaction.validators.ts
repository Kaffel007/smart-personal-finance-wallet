import { AbstractControl, ValidationErrors } from '@angular/forms';

export function browserToday(): string {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function transactionDateValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value || '');
  if (!value) return null; // Required is handled separately.
  const parsed = new Date(`${value}T12:00:00`);
  const valid = /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parsed.getTime())
    && parsed.getFullYear() === Number(value.slice(0, 4))
    && parsed.getMonth() + 1 === Number(value.slice(5, 7))
    && parsed.getDate() === Number(value.slice(8, 10));
  return valid && value <= browserToday() ? null : { transactionDate: true };
}

export function amountValidator(control: AbstractControl): ValidationErrors | null {
  if (control.value === null || control.value === '') return null;
  const amount = Number(control.value);
  // Match DECIMAL(19,4) without computing any financial totals.
  return Number.isFinite(amount) && amount > 0 && amount < 1e15
    && /^\d+(\.\d{1,4})?$/.test(String(amount)) ? null : { amount: true };
}
