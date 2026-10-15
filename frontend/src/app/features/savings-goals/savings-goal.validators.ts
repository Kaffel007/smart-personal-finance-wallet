import { AbstractControl, ValidationErrors } from '@angular/forms';
import { amountValidator } from '../transactions/transaction.validators';

export function nonBlankNameValidator(control: AbstractControl): ValidationErrors | null {
  return String(control.value ?? '').trim() ? null : { blank: true };
}

export function savedAmountValidator(control: AbstractControl): ValidationErrors | null {
  return control.value === 0 ? null : amountValidator(control);
}

export function targetDateValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  if (!value) return null;
  const parsed = new Date(`${value}T12:00:00`);
  const valid = /^\d{4}-\d{2}-\d{2}$/.test(value) && Number(value.slice(0, 4)) >= 1
    && parsed.getFullYear() === Number(value.slice(0, 4))
    && parsed.getMonth() + 1 === Number(value.slice(5, 7))
    && parsed.getDate() === Number(value.slice(8, 10));
  return valid ? null : { targetDate: true };
}
