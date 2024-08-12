import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loginUrl = 'http://localhost:8080/api/login';  // Backend endpoint for manual login
  private socialLoginUrl = 'http://localhost:8080/api/social-login';  // Backend endpoint for social login
  private userDetails: any = null;

  constructor(private http: HttpClient) {}

  // Manual login method
  login(credentials: { username: string; password: string }): Observable<any> {
    return this.http.post(this.loginUrl, credentials).pipe(
      map(response => {
        this.userDetails = response;
        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        return response;
      }),
      catchError(error => {
        console.error('Login error', error);
        return of(null);
      })
    );
  }

  // Social login method
  verifySocialLogin(authCode: string, provider: string): Observable<any> {
    return this.http.post(this.socialLoginUrl, { authCode, provider }).pipe(
      map(response => {
        this.userDetails = response;
        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        return response;
      }),
      catchError(error => {
        console.error('Social login error', error);
        return of(null);
      })
    );
  }

  // Check if the user is authenticated
  isAuthenticated(): boolean {
    return localStorage.getItem('user') !== null;
  }

  // Get user details
  getUserDetails(): Observable<User> {
    if (!this.userDetails) {
      this.userDetails = JSON.parse(localStorage.getItem('user') || '{}');
    }
    return of(this.userDetails as User);
  }

  // Set user details manually (useful after social login)
  setUserDetails(user: any): void {
    this.userDetails = user;
    localStorage.setItem('user', JSON.stringify(this.userDetails));
  }

  // Logout method
  logout(): Observable<any> {
    this.userDetails = null;
    localStorage.removeItem('user');
    // You can also call a backend logout endpoint if necessary
    return of(true);
  }
}
