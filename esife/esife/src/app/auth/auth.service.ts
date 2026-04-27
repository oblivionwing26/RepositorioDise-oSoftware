import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  name: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  name: string;
}

const TOKEN_KEY = 'esientradas.userToken';
const EMAIL_KEY = 'esientradas.userEmail';
const NAME_KEY = 'esientradas.userName';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = 'http://localhost:8081/users';

  constructor(private http: HttpClient) {}

  register(req: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/register`, req);
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, req).pipe(
      tap((res) => this.storeSession(res))
    );
  }

  logout(): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    localStorage.removeItem(NAME_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(TOKEN_KEY);
  }

  getEmail(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(EMAIL_KEY);
  }

  getName(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem(NAME_KEY);
  }

  private storeSession(res: LoginResponse): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem(TOKEN_KEY, res.token);
    if (res.email) localStorage.setItem(EMAIL_KEY, res.email);
    if (res.name) localStorage.setItem(NAME_KEY, res.name);
  }
}
