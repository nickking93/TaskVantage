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
  private userDetails: User | null = null;

  constructor(private http: HttpClient) {}

  // Manual login method
  login(credentials: { username: string; password: string }): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<User>(this.loginUrl, credentials, { headers }).pipe(
      map((response: any) => {
        // Ensure the response contains userId, username, and token
        const userId = response.userId;
        const username = response.username;
        const token = response.token;

        // Construct the user object
        this.userDetails = {
          id: userId,
          username: username,
          password: '', // Password isn't needed here, set it as empty
          token: token // Include the JWT token
        };

        // Store user details and token in local storage
        localStorage.setItem('user', JSON.stringify(this.userDetails));
        localStorage.setItem('jwtToken', token); // Corrected the key for consistency

        return this.userDetails;
      }),
      catchError(this.handleError)
    );
  }

  // Registration method
  register(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.registerUrl, credentials, { headers }).pipe(
      map((response: any) => {
        console.log('Registration successful', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Method to get the JWT token from localStorage
  private getToken(): string | null {
    return localStorage.getItem('jwtToken');  // Ensure consistency with the login method
  }

  // Include the token in the headers for authenticated requests
  public getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // Handle error response
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Error:', error.message);
    return throwError(() => new Error('Error: ' + error.message));
  }

  // Check if the user is authenticated
  isAuthenticated(): boolean {
    return this.getToken() !== null;
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

  // Set user details manually (if needed)
  setUserDetails(user: User): void {
    this.userDetails = user;
    localStorage.setItem('user', JSON.stringify(this.userDetails));
  }

  // Logout method
  logout(): Observable<any> {
    this.userDetails = null;
    localStorage.removeItem('user');
    localStorage.removeItem('jwtToken');  // Ensure consistency with the login method
    return new Observable((observer) => observer.next(true));
  }

  // Method for authenticated requests (example: fetching tasks)
  getTasks(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get('http://localhost:8080/api/tasks', { headers }).pipe(
      catchError(this.handleError)
    );
  }
}
