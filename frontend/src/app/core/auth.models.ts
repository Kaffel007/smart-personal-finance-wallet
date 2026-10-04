export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}
export interface LoginRequest { email: string; password: string; }
export interface UserSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: 'USER' | 'ADMIN';
}
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserSummary;
}
