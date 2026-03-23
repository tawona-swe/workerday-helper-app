import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { AuthResponse, User } from '../models/user.model';

const API = 'http://localhost:8080/api/auth';
const TOKEN_KEY = 'wh_token';
const USER_KEY = 'wh_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  currentUser = signal<User | null>(this.loadUser());

  constructor(private http: HttpClient, private router: Router) {}

  register(name: string, email: string, password: string) {
    return this.http.post<AuthResponse>(`${API}/register`, { name, email, password }).pipe(
      tap(res => this.persist(res))
    );
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${API}/login`, { email, password }).pipe(
      tap(res => this.persist(res))
    );
  }

  updateProfile(data: { name?: string; password?: string }) {
    return this.http.put<User>(`${API}/me`, data).pipe(
      tap(user => {
        const updated = { ...this.currentUser()!, ...user };
        this.currentUser.set(updated);
        localStorage.setItem(USER_KEY, JSON.stringify(updated));
      })
    );
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private persist(res: AuthResponse) {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    this.currentUser.set(res.user);
    this.router.navigate(['/dashboard']);
  }

  private loadUser(): User | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
