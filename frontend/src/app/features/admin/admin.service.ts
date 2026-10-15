import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { AdminStats, AdminUser } from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/admin`;
  getStats() { return this.http.get<AdminStats>(`${this.url}/stats`); }
  getUsers(search?: string) {
    const term = search?.trim();
    const params = term ? new HttpParams().set('search', term) : new HttpParams();
    return this.http.get<AdminUser[]>(`${this.url}/users`, { params });
  }
  getUser(id: number) { return this.http.get<AdminUser>(`${this.url}/users/${id}`); }
  blockUser(id: number) { return this.http.put<AdminUser>(`${this.url}/users/${id}/block`, null); }
  unblockUser(id: number) { return this.http.put<AdminUser>(`${this.url}/users/${id}/unblock`, null); }
}
