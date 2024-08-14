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
  private userDetails: User | null = null;

  constructor(private http: HttpClient) {}

  // Manual login method
  login(credentials: { username: string; password: string }): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<User>(this.loginUrl, credentials, { headers }).pipe(
      map((response: any) => { // Ensure the response type is any to access properties directly
        // Extract userId and username from the response
        const userId = response.userId;
        const username = response.username;

        // Construct the user object
        this.userDetails = {
          id: userId,
          username: username,
          password: '', // Password isn't needed here, set it as empty
          token: response.token || '' // Include token if available
        };

        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        return this.userDetails;
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
    console.error('Error:', error.message);
    return throwError(() => new Error('Error: ' + error.message));
  }

  // Check if the user is authenticated
  isAuthenticated(): boolean {
    return localStorage.getItem('user') !== null;
  }

  // Get user details
  getUserDetails(): Observable<User> {
    if (!this.userDetails) {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        this.userDetails = JSON.parse(storedUser);
      }
    }
    return new Observable((observer) => {
      if (this.userDetails) {
        observer.next(this.userDetails);
      } else {
        observer.error('User not authenticated.');
      }
    });
  }

  // Set user details manually (useful after social login)
  setUserDetails(user: User): void {
    this.userDetails = user;
    localStorage.setItem('user', JSON.stringify(this.userDetails));
  }

  // Logout method
  logout(): Observable<any> {
    this.userDetails = null;
    localStorage.removeItem('user');
    return new Observable((observer) => observer.next(true));
  }

  // Updated social login method
  verifySocialLogin(authCode: string, provider: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.socialLoginUrl, { authCode, provider }, { headers }).pipe(
      map(response => {
        // Assuming the response contains userId and username
        const userId = (response as any).userId;
        const username = (response as any).username;

        // Construct the user object
        this.userDetails = {
          id: userId,
          username: username,
          password: '', // Password might not be available in this context
          token: (response as any).token || '' // Include token if available
        };

        // Store user details in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));

        return response;
      }),
      catchError(this.handleError)
    );
  }
}
