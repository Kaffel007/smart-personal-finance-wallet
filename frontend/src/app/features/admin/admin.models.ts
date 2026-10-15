import { UserSummary } from '../../core/auth.models';

export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: UserSummary['role'];
  enabled: boolean;
  blocked: boolean;
  createdAt: string;
}

export interface AdminStats {
  totalUsers: number;
  activeUsers: number;
  blockedUsers: number;
  adminUsers: number;
}
