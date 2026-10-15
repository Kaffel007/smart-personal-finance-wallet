import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { nonBlankValidator, passwordValidator } from '../../core/auth.validators';
import { Account } from './account.models';
import { AccountService } from './account.service';
import { passwordChangeValidator } from './account.validators';

@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule],
  templateUrl: './account.component.html',
  styleUrls: ['../../shared/finance-ui.css', './account.component.css'],
})
export class AccountComponent {
  private readonly service = inject(AccountService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly builder = inject(FormBuilder);
  private readonly reload = new Subject<void>();
  private readonly dateFormatter = new Intl.DateTimeFormat('fr-FR');
  readonly account = signal<Account | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly profileSaving = signal(false);
  readonly emailSaving = signal(false);
  readonly passwordSaving = signal(false);
  readonly busy = computed(() => this.profileSaving() || this.emailSaving() || this.passwordSaving());
  readonly profileError = signal('');
  readonly emailError = signal('');
  readonly passwordError = signal('');
  readonly profileFeedback = signal('');
  readonly emailFeedback = signal('');
  readonly passwordFeedback = signal('');
  readonly profileForm = this.builder.nonNullable.group({
    firstName: ['', [Validators.required, nonBlankValidator, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, nonBlankValidator, Validators.maxLength(100)]],
  });
  readonly emailForm = this.builder.nonNullable.group({
    newEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    currentPassword: ['', [Validators.required, nonBlankValidator]],
  });
  readonly passwordForm = this.builder.nonNullable.group({
    currentPassword: ['', [Validators.required, nonBlankValidator]],
    newPassword: ['', [Validators.required, passwordValidator]],
    confirmation: ['', Validators.required],
  }, { validators: passwordChangeValidator });

  constructor() {
    this.reload.pipe(startWith(undefined), switchMap(() => {
      this.loading.set(true);
      this.loadError.set('');
      this.account.set(null);
      return this.service.getMe().pipe(catchError(() => {
        this.loadError.set('Impossible de charger votre compte.');
        return of(null);
      }));
    }), takeUntilDestroyed()).subscribe(account => {
      if (account) this.acceptAccount(account);
      this.loading.set(false);
    });
    this.destroyRef.onDestroy(() => this.clearPasswords());
  }
  load(): void { if (!this.busy()) this.reload.next(); }
  private acceptAccount(account: Account): void {
    this.account.set(account);
    this.profileForm.reset({ firstName: account.firstName, lastName: account.lastName });
    this.auth.syncCurrentUser(account);
  }
  saveProfile(): void {
    if (this.busy() || this.loading() || !this.account()) return;
    this.profileForm.markAllAsTouched();
    this.profileFeedback.set(''); this.profileError.set('');
    if (this.profileForm.invalid) return;
    const value = this.profileForm.getRawValue();
    this.profileSaving.set(true);
    this.service.updateProfile({ firstName: value.firstName.trim(), lastName: value.lastName.trim() })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.profileSaving.set(false))).subscribe({
        next: account => { this.acceptAccount(account); this.profileFeedback.set('Profil mis à jour.'); },
        error: () => this.profileError.set('Impossible de mettre à jour le profil.'),
      });
  }
  saveEmail(): void {
    if (this.busy() || this.loading() || !this.account()) return;
    this.emailForm.controls.newEmail.setValue(this.emailForm.controls.newEmail.value.trim());
    this.emailForm.markAllAsTouched();
    this.emailFeedback.set(''); this.emailError.set('');
    if (this.emailForm.invalid) return;
    this.emailSaving.set(true);
    this.service.updateEmail(this.emailForm.getRawValue())
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.emailSaving.set(false))).subscribe({
        next: account => {
          this.emailForm.reset();
          this.clearPasswords();
          this.acceptAccount(account);
          this.emailFeedback.set('Adresse e-mail mise à jour.');
        },
        error: error => {
          this.emailForm.controls.currentPassword.reset();
          this.emailError.set(this.credentialError(error, 'email'));
        },
      });
  }
  savePassword(): void {
    if (this.busy() || this.loading() || !this.account()) return;
    this.passwordForm.markAllAsTouched();
    this.passwordFeedback.set(''); this.passwordError.set('');
    if (this.passwordForm.invalid) return;
    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.passwordSaving.set(true);
    this.service.changePassword({ currentPassword, newPassword })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.passwordSaving.set(false))).subscribe({
        next: () => { this.clearPasswords(); this.passwordFeedback.set('Mot de passe mis à jour.'); },
        error: error => { this.passwordForm.reset(); this.passwordError.set(this.credentialError(error, 'password')); },
      });
  }
  private credentialError(error: unknown, section: 'email' | 'password'): string {
    if (error instanceof HttpErrorResponse) {
      if (section === 'email' && error.status === 409) return 'Cette adresse e-mail est déjà utilisée.';
      if (error.status === 400) {
        const fields = error.error?.fieldErrors;
        if (fields && Object.prototype.hasOwnProperty.call(fields, 'currentPassword')) return 'Le mot de passe actuel est incorrect.';
        if (section === 'password' && fields && Object.prototype.hasOwnProperty.call(fields, 'newPassword')) {
          return 'Le nouveau mot de passe doit respecter la politique indiquée et être différent du mot de passe actuel.';
        }
      }
    }
    return section === 'email' ? "Impossible de modifier l'adresse e-mail." : 'Impossible de modifier le mot de passe.';
  }
  private clearPasswords(): void {
    this.emailForm.controls.currentPassword.reset();
    this.passwordForm.reset();
  }
  formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : this.dateFormatter.format(date);
  }
}
