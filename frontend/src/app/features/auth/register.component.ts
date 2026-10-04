import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { nonBlankValidator, passwordValidator } from '../../core/auth.validators';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-shell">
      <section class="auth-intro">
        <a class="brand" routerLink="/">Smart Personal Finance Wallet</a>
        <p class="eyebrow">UN NOUVEAU DÉPART</p>
        <h1>Votre espace.<br>Vos objectifs.</h1>
        <p>Créez votre compte pour commencer à organiser vos finances.</p>
      </section>
      <section class="auth-card" aria-labelledby="register-title">
        <p class="eyebrow">COMMENCER</p><h2 id="register-title">Créer un compte</h2>
        <p class="muted">Tous les champs sont obligatoires.</p>
        <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="loading()">
          @for (field of fields; track field.key) {
            <label [for]="field.key">{{ field.label }}</label>
            <input [id]="field.key" [type]="field.type" [autocomplete]="field.autocomplete" [formControlName]="field.key" [attr.maxlength]="field.maxLength" [attr.aria-describedby]="field.key + '-help'" [attr.aria-invalid]="form.controls[field.key].touched && form.controls[field.key].invalid">
            <p [id]="field.key + '-help'" [class.field-error]="form.controls[field.key].touched && form.controls[field.key].invalid" class="field-help">{{ field.help }}</p>
          }
          @if (error()) { <p class="error-banner" role="alert">{{ error() }}</p> }
          <button class="primary" type="submit" [disabled]="loading()">{{ loading() ? 'Création en cours…' : 'Créer mon compte' }}</button>
        </form>
        <p class="auth-footer">Déjà inscrit ? <a routerLink="/login">Se connecter</a></p>
      </section>
    </main>
  `,
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    firstName: ['', [Validators.required, nonBlankValidator, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, nonBlankValidator, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, passwordValidator]],
  });
  readonly fields: { key: 'firstName' | 'lastName' | 'email' | 'password'; label: string; type: string; autocomplete: string; maxLength: number | null; help: string }[] = [
    { key: 'firstName', label: 'Prénom', type: 'text', autocomplete: 'given-name', maxLength: 100, help: 'Prénom requis, 100 caractères maximum.' },
    { key: 'lastName', label: 'Nom', type: 'text', autocomplete: 'family-name', maxLength: 100, help: 'Nom requis, 100 caractères maximum.' },
    { key: 'email', label: 'Adresse e-mail', type: 'email', autocomplete: 'email', maxLength: 254, help: 'Adresse e-mail valide, 254 caractères maximum.' },
    { key: 'password', label: 'Mot de passe', type: 'password', autocomplete: 'new-password', maxLength: null, help: 'Au moins 12 caractères et au plus 72 octets UTF-8 (les accents et emoji comptent pour plusieurs octets).' },
  ];
  submit(): void {
    if (this.loading()) return;
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error.set('');
    this.loading.set(true);
    this.auth.register(this.form.getRawValue()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => { this.form.reset(); void this.router.navigateByUrl('/login'); },
      error: (error: unknown) => {
        this.form.controls.password.reset();
        // Use local, reviewed messages; never display technical API details.
        if (error instanceof HttpErrorResponse && error.status === 400) {
          const fieldErrors = error.error?.fieldErrors;
          for (const field of this.fields) {
            if (fieldErrors && Object.prototype.hasOwnProperty.call(fieldErrors, field.key)) {
              this.form.controls[field.key].setErrors({ server: true });
            }
          }
          this.error.set('Vérifiez les champs signalés et saisissez à nouveau votre mot de passe.');
        } else {
          this.error.set('Impossible de créer le compte. Vérifiez vos informations ou réessayez plus tard.');
        }
      },
    });
  }
}
