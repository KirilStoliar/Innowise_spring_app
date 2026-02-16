import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { environment } from '../../environments/environment';
import {
  ApiResponse,
  LoginRequest,
  RefreshTokenRequest,
  TokenResponse,
  TokenValidationResponse,
  UserCredentialsRequest,
  Role,
  RegisterUserResponse,
} from '../types/auth.types';

const ACCESS_KEY = 'access_token';
const REFRESH_KEY = 'refresh_token';
const ROLE_KEY = 'auth_role';
const EMAIL_KEY = 'auth_email';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private readonly apiUrl = environment.apiGatewayUrl;

  private readonly _access = signal<string | null>(localStorage.getItem(ACCESS_KEY));
  private readonly _refresh = signal<string | null>(localStorage.getItem(REFRESH_KEY));
  private readonly _role = signal<Role | null>((localStorage.getItem(ROLE_KEY) as Role) ?? null);
  private readonly _email = signal<string | null>(localStorage.getItem(EMAIL_KEY));

  readonly isAuthenticated = computed(() => !!this._access());
  readonly role = computed(() => this._role());
  readonly email = computed(() => this._email());
  readonly isAdmin = computed(() => this._role() === 'ADMIN');

  constructor() {
    effect(() => {
      const a = this._access();
      a ? localStorage.setItem(ACCESS_KEY, a) : localStorage.removeItem(ACCESS_KEY);
    });

    effect(() => {
      const r = this._refresh();
      r ? localStorage.setItem(REFRESH_KEY, r) : localStorage.removeItem(REFRESH_KEY);
    });

    effect(() => {
      const role = this._role();
      role ? localStorage.setItem(ROLE_KEY, role) : localStorage.removeItem(ROLE_KEY);
    });

    effect(() => {
      const email = this._email();
      email ? localStorage.setItem(EMAIL_KEY, email) : localStorage.removeItem(EMAIL_KEY);
    });
  }

  getAccessToken() {
    return this._access();
  }
  getRefreshToken() {
    return this._refresh();
  }

  setTokens(tokens: TokenResponse) {
    this._access.set(tokens.accessToken);
    this._refresh.set(tokens.refreshToken);
  }

  setIdentity(v: TokenValidationResponse | null) {
    this._role.set(v?.role ?? null);
    this._email.set(v?.email ?? null);
  }

  logout() {
    this._access.set(null);
    this._refresh.set(null);
    this.setIdentity(null);
  }

  login(req: LoginRequest) {
    return this.http.post<ApiResponse<TokenResponse>>(`${this.apiUrl}/api/v1/auth/login`, req);
  }

  refresh(req: RefreshTokenRequest) {
    return this.http.post<ApiResponse<TokenResponse>>(`${this.apiUrl}/api/v1/auth/refresh`, req);
  }

  validate(accessToken: string) {
    const headers = new HttpHeaders({ Authorization: `Bearer ${accessToken}` });
    return this.http.post<ApiResponse<TokenValidationResponse>>(
      `${this.apiUrl}/api/v1/auth/validate`,
      null,
      { headers }
    );
  }

  registerByAdmin(req: UserCredentialsRequest) {
    return this.http.post<ApiResponse<RegisterUserResponse>>(`${this.apiUrl}/api/v1/auth/register`, req);
  }
}
