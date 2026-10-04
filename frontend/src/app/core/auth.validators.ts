import { AbstractControl, ValidationErrors } from '@angular/forms';

export function nonBlankValidator(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && !control.value.trim() ? { blank: true } : null;
}

export function passwordValidator(control: AbstractControl): ValidationErrors | null {
  const password: string = control.value || '';
  return password.trim() && Array.from(password).length >= 12
    && new TextEncoder().encode(password).length <= 72 ? null : { passwordPolicy: true };
}
