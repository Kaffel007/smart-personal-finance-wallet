import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { AdminStats, AdminUser } from './admin.models';
import { AdminService } from './admin.service';

@Component({
  selector: 'app-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['../../shared/finance-ui.css', './admin.component.css'],
})
export class AdminComponent {
  private readonly service = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly statsReload = new Subject<void>();
  private readonly usersReload = new Subject<void>();
  private readonly dateFormatter = new Intl.DateTimeFormat('fr-FR');
  readonly stats = signal<AdminStats | null>(null);
  readonly users = signal<AdminUser[]>([]);
  readonly statsLoading = signal(true);
  readonly usersLoading = signal(true);
  readonly statsError = signal('');
  readonly usersError = signal('');
  readonly actionError = signal('');
  readonly feedback = signal('');
  readonly busy = signal(false);
  readonly pendingBlock = signal<AdminUser | null>(null);
  readonly unblockingId = signal<number | null>(null);
  readonly searchControl = new FormControl('', { nonNullable: true });
  readonly submittedSearch = signal('');

  constructor() {
    this.statsReload.pipe(startWith(undefined), switchMap(() => {
      this.statsLoading.set(true);
      this.statsError.set('');
      this.stats.set(null);
      return this.service.getStats().pipe(catchError(() => {
        this.statsError.set('Impossible de charger les statistiques.');
        return of(null);
      }));
    }), takeUntilDestroyed()).subscribe(stats => { this.stats.set(stats); this.statsLoading.set(false); });
    this.usersReload.pipe(startWith(undefined), switchMap(() => {
      this.usersLoading.set(true);
      this.usersError.set('');
      this.users.set([]);
      return this.service.getUsers(this.submittedSearch()).pipe(catchError(() => {
        this.usersError.set('Impossible de charger les utilisateurs.');
        return of([] as AdminUser[]);
      }));
    }), takeUntilDestroyed()).subscribe(users => { this.users.set(users); this.usersLoading.set(false); });
  }
  loadStats(): void { this.statsReload.next(); }
  loadUsers(): void { this.usersReload.next(); }
  search(): void {
    if (this.busy()) return;
    const term = this.searchControl.value.trim();
    this.searchControl.setValue(term);
    this.submittedSearch.set(term);
    this.pendingBlock.set(null);
    this.actionError.set('');
    this.loadUsers();
  }
  resetSearch(): void {
    if (this.busy()) return;
    this.searchControl.setValue('');
    this.search();
  }
  canManage(user: AdminUser): boolean {
    return user.role === 'USER' && user.id !== this.auth.currentUser()?.id;
  }
  askBlock(user: AdminUser): void {
    if (this.busy() || !this.canManage(user) || user.blocked) return;
    this.pendingBlock.set(user);
    this.actionError.set('');
    this.feedback.set('');
  }
  cancelBlock(): void { if (!this.busy()) { this.pendingBlock.set(null); this.actionError.set(''); } }
  confirmBlock(): void {
    const user = this.pendingBlock();
    if (user) this.changeBlocked(user, true);
  }
  unblock(user: AdminUser): void {
    if (!this.busy() && this.canManage(user) && user.blocked) this.pendingBlock.set(null);
    this.changeBlocked(user, false);
  }
  private changeBlocked(user: AdminUser, blocked: boolean): void {
    if (this.busy() || !this.canManage(user) || user.blocked === blocked) return;
    this.busy.set(true);
    this.actionError.set('');
    this.feedback.set('');
    this.unblockingId.set(blocked ? null : user.id);
    const operation = blocked ? this.service.blockUser(user.id) : this.service.unblockUser(user.id);
    operation.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.busy.set(false);
      this.unblockingId.set(null);
    })).subscribe({
      next: () => {
        this.pendingBlock.set(null);
        this.feedback.set(blocked ? 'Utilisateur bloqué.' : 'Utilisateur débloqué.');
        this.loadUsers();
        this.loadStats();
      },
      error: error => this.actionError.set(this.errorMessage(error, blocked)),
    });
  }
  private errorMessage(error: unknown, blocked: boolean): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 403) return "Vous n'avez pas l'autorisation d'effectuer cette action.";
      if (error.status === 409) return "Cette action n'est pas autorisée sur ce compte. Actualisez la liste.";
      if (error.status === 404) return 'Utilisateur introuvable. Actualisez la liste.';
    }
    return blocked ? 'Impossible de bloquer cet utilisateur.' : 'Impossible de débloquer cet utilisateur.';
  }
  formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : this.dateFormatter.format(date);
  }
}
