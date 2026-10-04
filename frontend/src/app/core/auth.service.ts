import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { LoginRequest, LoginResponse, RegisterRequest, UserSummary } from './auth.models';

export const TOKEN_STORAGE_KEY = 'smart_finance_access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly userState = signal<UserSummary | null>(null);
  readonly currentUser = this.userState.asReadonly();

  register(request: RegisterRequest) {
    return this.http.post<UserSummary>(`${API_BASE_URL}/auth/register`, request);
  }
  login(request: LoginRequest) {
    return this.http.post<LoginResponse>(`${API_BASE_URL}/auth/login`, request).pipe(
      tap(response => {
        localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
        this.userState.set(response.user);
      }),
    );
  }
  getCurrentUser() {
    return this.http.get<UserSummary>(`${API_BASE_URL}/auth/me`).pipe(
      tap(user => this.userState.set(user)),
    );
  }
  getToken(): string | null { return localStorage.getItem(TOKEN_STORAGE_KEY); }
  isAuthenticated(): boolean { return !!this.getToken(); }
  logout(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    this.userState.set(null);
    void this.router.navigateByUrl('/login');
  }
}
