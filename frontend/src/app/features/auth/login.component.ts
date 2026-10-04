import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { nonBlankValidator } from '../../core/auth.validators';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-shell">
      <section class="auth-intro">
        <a class="brand" routerLink="/">Smart Personal Finance Wallet</a>
        <p class="eyebrow">VOTRE ESPACE PERSONNEL</p>
        <h1>Vos finances,<br>en toute clarté.</h1>
        <p>Retrouvez votre espace pour suivre vos finances personnelles.</p>
      </section>
      <section class="auth-card" aria-labelledby="login-title">
        <p class="eyebrow">BIENVENUE</p><h2 id="login-title">Connexion</h2>
        <p class="muted">Connectez-vous à votre compte.</p>
        <form [formGroup]="form" (ngSubmit)="submit()" [attr.aria-busy]="loading()">
          <label for="email">Adresse e-mail</label>
          <input id="email" type="email" autocomplete="email" formControlName="email" maxlength="254" aria-describedby="email-error" [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid">
          @if (form.controls.email.touched && form.controls.email.invalid) {
            <p id="email-error" class="field-error">Saisissez une adresse e-mail valide (254 caractères maximum).</p>
          }
          <label for="password">Mot de passe</label>
          <input id="password" type="password" autocomplete="current-password" formControlName="password" aria-describedby="password-error" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid">
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p id="password-error" class="field-error">Le mot de passe est obligatoire.</p>
          }
          @if (error()) { <p class="error-banner" role="alert">{{ error() }}</p> }
          <button class="primary" type="submit" [disabled]="loading()">{{ loading() ? 'Connexion en cours…' : 'Se connecter' }}</button>
        </form>
        <p class="auth-footer">Pas encore de compte ? <a routerLink="/register">Créer un compte</a></p>
      </section>
    </main>
  `,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, nonBlankValidator]],
  });
  submit(): void {
    if (this.loading()) return;
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error.set('');
    this.loading.set(true);
    this.auth.login(this.form.getRawValue()).pipe(
      switchMap(() => this.auth.getCurrentUser()),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: () => { this.form.reset(); void this.router.navigateByUrl('/dashboard'); },
      error: () => {
        this.auth.logout();
        this.form.controls.password.reset();
        this.error.set('Connexion impossible. Vérifiez vos identifiants ou réessayez plus tard.');
      },
    });
  }
}
