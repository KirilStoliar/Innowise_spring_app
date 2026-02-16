export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export type Role = 'ADMIN' | 'USER';

export interface TokenValidationResponse {
  valid: boolean;
  email: string;
  role: Role;
  message: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface UserCredentialsRequest {
  password: string;
  role: Role;
  name: string;
  surname: string;
  email: string;
  birthDate: string;
}

export interface RegisterUserResponse {
  id: number;
  email: string;
  password: string;
  role: Role;
  active: boolean;
  refreshToken: string | null;
  refreshTokenExpiry: string | null;
  createdAt: string;
  updatedAt: string;
  name: string;
  surname: string;
  birthDate: string;
}
