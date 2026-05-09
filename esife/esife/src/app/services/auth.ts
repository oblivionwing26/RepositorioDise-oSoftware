import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
export interface LoginResponse {
  token: string;
  expiresIn: number;
}

@Injectable({providedIn: 'root'})
export class Auth {
  private readonly USERS_API = 'http://localhost:8081/users';
  private readonly STORAGE_KEY = 'jwt';

  constructor (private http: HttpClient) {}

  register (email: string, password: string) : Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/register`, { email, password });
  }

  login(email: string, password: string) : Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.USERS_API}/login`, { email, password });
  }
  forgotPassword(email: string) : Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string) : Observable<void> {
    return this.http.post<void>(`${this.USERS_API}/reset-password`, { token, newPassword });
  }

  cancelAccount(): Observable<void> {
    const headers = {Authorization: `Bearer ${this.getToken()}`};
    return this.http.delete<void>(`${this.USERS_API}/cancel`, { headers });
  }

  saveToken(token: string): void {
    if(typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, token);
    }
  }

  getToken(): string | null {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(this.STORAGE_KEY) : null;
  }

  logout(): void{
    if(typeof localStorage !== 'undefined') {
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  isLogged(): boolean {
    return !!this.getToken();
  }
  
}
