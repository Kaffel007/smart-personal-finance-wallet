import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../core/api.config';
import { Account, ChangePasswordRequest, UpdateEmailRequest, UpdateProfileRequest } from './account.models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE_URL}/account`;
  getMe() { return this.http.get<Account>(`${this.url}/me`); }
  updateProfile(request: UpdateProfileRequest) {
    const { firstName, lastName } = request;
    return this.http.put<Account>(`${this.url}/profile`, { firstName, lastName });
  }
  updateEmail(request: UpdateEmailRequest) {
    const { newEmail, currentPassword } = request;
    return this.http.put<Account>(`${this.url}/email`, { newEmail, currentPassword });
  }
  changePassword(request: ChangePasswordRequest) {
    const { currentPassword, newPassword } = request;
    return this.http.put<void>(`${this.url}/password`, { currentPassword, newPassword });
  }
}
