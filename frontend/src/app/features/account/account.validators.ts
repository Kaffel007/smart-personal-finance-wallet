import { AbstractControl, ValidationErrors } from '@angular/forms';

export function passwordChangeValidator(control: AbstractControl): ValidationErrors | null {
  const { currentPassword, newPassword, confirmation } = control.value;
  const errors: ValidationErrors = {};
  if (newPassword && confirmation && newPassword !== confirmation) errors['passwordMismatch'] = true;
  if (currentPassword && newPassword && currentPassword === newPassword) errors['passwordUnchanged'] = true;
  return Object.keys(errors).length ? errors : null;
}
