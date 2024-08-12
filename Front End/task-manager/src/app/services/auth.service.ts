import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private loginUrl = 'http://localhost:8080/api/login';  // Backend endpoint for manual login
  private registerUrl = 'http://localhost:8080/api/register';  // Backend endpoint for registration
  private socialLoginUrl = 'http://localhost:8080/api/social-login';  // Backend endpoint for social login
  private userDetails: any = null;

  constructor(private http: HttpClient) {}

  // Manual login method
  login(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.loginUrl, credentials, { headers }).pipe(
      map(response => {
        this.userDetails = response;
        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        // Optionally, store a token if returned by the backend
        if (response && (response as any).token) {
          localStorage.setItem('authToken', (response as any).token);
        }
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Registration method
  register(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.registerUrl, credentials, { headers }).pipe(
      map(response => {
        console.log('Registration successful', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Handle error response
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Registration error:', error.message);
    return throwError(() => new Error('Registration error: ' + error.message));
  }

  // Social login method
  verifySocialLogin(authCode: string, provider: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.socialLoginUrl, { authCode, provider }, { headers }).pipe(
      map(response => {
        this.userDetails = response;
        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        // Optionally, store a token if returned by the backend
        if (response && (response as any).token) {
          localStorage.setItem('authToken', (response as any).token);
        }
        return response;
      }),
      catchError(this.handleError)
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
    return new Observable((observer) => observer.next(this.userDetails as User));
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
    localStorage.removeItem('authToken');  // Optionally remove the token
    return new Observable((observer) => observer.next(true));
  }
}
