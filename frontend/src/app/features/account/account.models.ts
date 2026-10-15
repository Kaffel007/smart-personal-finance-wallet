import { UserSummary } from '../../core/auth.models';

export interface Account {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: UserSummary['role'];
  enabled: boolean;
  blocked: boolean;
  createdAt: string;
}
export interface UpdateProfileRequest { firstName: string; lastName: string; }
export interface UpdateEmailRequest { newEmail: string; currentPassword: string; }
export interface ChangePasswordRequest { currentPassword: string; newPassword: string; }
