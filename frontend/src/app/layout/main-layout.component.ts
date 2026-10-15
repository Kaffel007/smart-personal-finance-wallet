import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <a class="brand" [routerLink]="auth.currentUser()?.role === 'ADMIN' ? '/admin' : '/dashboard'">Smart Personal<br>Finance Wallet</a>
        <nav aria-label="Navigation principale">
          @if (auth.currentUser(); as user) {
          @if (user.role === 'ADMIN') {
          <a routerLink="/admin" routerLinkActive="active" ariaCurrentWhenActive="page">Administration</a>
          } @else {
          <a routerLink="/dashboard" routerLinkActive="active" ariaCurrentWhenActive="page">Tableau de bord</a>
          <a routerLink="/transactions" routerLinkActive="active" ariaCurrentWhenActive="page">Transactions</a>
          <a routerLink="/categories" routerLinkActive="active" ariaCurrentWhenActive="page">Catégories</a>
          <a routerLink="/budgets" routerLinkActive="active" ariaCurrentWhenActive="page">Budgets</a>
          <a routerLink="/savings-goals" routerLinkActive="active" ariaCurrentWhenActive="page">Objectifs d'épargne</a>
          }
          <a routerLink="/account" routerLinkActive="active" ariaCurrentWhenActive="page">Mon compte</a>
          }
        </nav>
        <button class="logout" type="button" (click)="auth.logout()">Déconnexion</button>
      </aside>
      <div class="workspace">
        <header class="workspace-header"><span>{{ auth.currentUser()?.role === 'ADMIN' ? 'Espace administrateur' : 'Espace personnel' }}</span><span>{{ auth.currentUser()?.firstName }}</span></header>
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
  constructor() {
    if (this.auth.currentUser()) this.loading.set(false);
    else this.loadUser();
  }
  loadUser(): void {
    this.loading.set(true);
    this.error.set(false);
    this.auth.getCurrentUser().subscribe({
      next: () => this.loading.set(false),
      error: () => { this.loading.set(false); this.error.set(true); },
    });
  }
}
