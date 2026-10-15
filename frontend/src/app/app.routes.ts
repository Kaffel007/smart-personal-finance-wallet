import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'register', canActivate: [guestGuard], loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
  { path: '', canActivate: [authGuard], loadComponent: () => import('./layout/main-layout.component').then(m => m.MainLayoutComponent), children: [
    { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
    { path: 'transactions', canActivate: [authGuard], loadComponent: () => import('./features/transactions/transactions.component').then(m => m.TransactionsComponent) },
    { path: 'categories', canActivate: [authGuard], loadComponent: () => import('./features/categories/categories.component').then(m => m.CategoriesComponent) },
    { path: 'budgets', canActivate: [authGuard], loadComponent: () => import('./features/budgets/budgets.component').then(m => m.BudgetsComponent) },
    { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  ] },
  { path: '**', redirectTo: '' },
];
