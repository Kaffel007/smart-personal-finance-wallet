import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  template: `
    <p class="eyebrow">VUE D'ENSEMBLE</p>
    <h1>Tableau de bord</h1>
    <p class="muted">Bienvenue {{ auth.currentUser()?.firstName }}.</p>
    <section class="dashboard-placeholder" aria-labelledby="statistics-title">
      <div class="placeholder-symbol" aria-hidden="true">€</div>
      <h2 id="statistics-title">Votre vue financière se prépare</h2>
      <p>Les statistiques de vos finances seront affichées ici.</p>
    </section>
  `,
})
export class DashboardComponent { readonly auth = inject(AuthService); }
