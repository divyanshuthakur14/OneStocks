import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  token: string;
  refreshToken: string;
  username: string;
  email: string;
}

@Injectable({
  providedIn: 'root'
})

export class AuthService {
  getCurrentUserId() {
    throw new Error('Method not implemented.');
  }

  private baseUrl = 'http://localhost:8082/api/auth';

  constructor(private http: HttpClient) {}

  signup(data: SignupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/signup`, data).pipe(
      tap(response => { if (response.success) this.saveSession(response); })
    );
  }

  login(data: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, data).pipe(
      tap(response => { if (response.success) this.saveSession(response); })
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (response.success) {
          localStorage.setItem('token', response.token);
        }
      })
    );
  }

  logout(): Observable<AuthResponse> {
    const body = {
      token: this.getToken(),
      refreshToken: this.getRefreshToken()
    };
    return this.http.post<AuthResponse>(`${this.baseUrl}/logout`, body).pipe(
      tap(() => this.clearSession())
    );
  }

  private saveSession(response: AuthResponse): void {
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('username', response.username);
    localStorage.setItem('email', response.email);
  }

  clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
  }

  getToken(): string | null { return localStorage.getItem('token'); }
  getRefreshToken(): string | null { return localStorage.getItem('refreshToken'); }
  getUsername(): string | null { return localStorage.getItem('username'); }
  isLoggedIn(): boolean { return !!this.getToken(); }
  
}