import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <a class="brand" routerLink="/dashboard">Smart Personal<br>Finance Wallet</a>
        <nav aria-label="Navigation principale">
          <a routerLink="/dashboard" routerLinkActive="active" ariaCurrentWhenActive="page">Tableau de bord</a>
          <span class="nav-pending">Transactions <small>À venir</small></span>
          <span class="nav-pending">Budgets <small>À venir</small></span>
          <span class="nav-pending">Objectifs d'épargne <small>À venir</small></span>
        </nav>
        <button class="logout" type="button" (click)="auth.logout()">Déconnexion</button>
      </aside>
      <div class="workspace">
        <header class="workspace-header"><span>Espace personnel</span><span>{{ auth.currentUser()?.firstName }}</span></header>
        <main class="workspace-content">
          @if (loading()) { <p role="status">Chargement de votre espace…</p> }
          @else if (error()) {
            <p class="error-banner" role="alert">Impossible de charger votre profil. Réessayez plus tard.</p>
            <button class="primary" type="button" (click)="loadUser()">Réessayer</button>
          } @else { <router-outlet /> }
        </main>
      </div>
    </div>
  `,
})
export class MainLayoutComponent {
  readonly auth = inject(AuthService);
  readonly loading = signal(true);
  readonly error = signal(false);
  constructor() { this.loadUser(); }
  loadUser(): void {
    this.loading.set(true);
    this.error.set(false);
    this.auth.getCurrentUser().subscribe({
      next: () => this.loading.set(false),
      error: () => { this.loading.set(false); this.error.set(true); },
    });
  }
}
