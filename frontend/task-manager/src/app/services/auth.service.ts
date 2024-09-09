import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { User } from '../models/user.model';
import { environment } from '../../environments/environment';
import { JwtHelperService } from '@auth0/angular-jwt'; 

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = environment.apiUrl;
  private socialLoginUrl = `${this.apiUrl}/api/social-login`;
  private loginUrl = `${this.apiUrl}/api/login`;
  private registerUrl = `${this.apiUrl}/api/register`;
  private userDetails: User | null = null;
  private jwtHelper = new JwtHelperService();

  constructor(private http: HttpClient) {}

  /**
   * Verifies social login using provider and authorization code.
   * @param authCode The authorization code from the social provider.
   * @param provider The social login provider (e.g., Google, Facebook).
   * @returns Observable containing response data.
   */
  verifySocialLogin(authCode: string, provider: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = { authCode, provider };
    return this.http.post<any>(this.socialLoginUrl, body, { headers }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  /**
   * Handles login by sending credentials and storing user data on success.
   * @param credentials Contains username, password, and optional FCM token.
   * @returns Observable containing user data.
   */
  login(credentials: { username: string; password: string; fcmToken?: string }): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<User>(this.loginUrl, credentials, { headers }).pipe(
      map((response: any) => {
        const userId = response.userId;
        const username = response.username;
        const token = response.token;

        // Store user details and token in localStorage
        this.userDetails = {
          id: userId,
          username: username,
          password: '',
          token: token
        };

        localStorage.setItem('user', JSON.stringify(this.userDetails));
        localStorage.setItem('jwtToken', token);

        return this.userDetails;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Registers a new user with username and password.
   * @param credentials Object containing username and password.
   * @returns Observable containing response data.
   */
  register(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.registerUrl, credentials, { headers }).pipe(
      map((response: any) => {
        if (response && response.message && response.message.includes('Registration successful')) {
          return response;
        } else {
          throw new Error(response.message || 'Registration failed.');
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Retrieves the JWT token from localStorage.
   * @returns The JWT token or null if not found.
   */
  private getToken(): string | null {
    return localStorage.getItem('jwtToken');
  }

  /**
   * Gets the authentication token (JWT).
   * @returns The JWT token or null.
   */
  public getAuthToken(): string | null {
    return this.getToken();
  }

  /**
   * Creates HTTP headers with the JWT token for authorized requests.
   * @returns HttpHeaders containing the authorization token.
   */
  public getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  /**
   * Handles errors from HTTP requests and provides appropriate error messages.
   * @param error The HttpErrorResponse object.
   * @returns Observable throwing an appropriate error message.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Error:', error.message);

    if (error.status === 401 && error.error.message === "Your email is not verified. Please verify your email before logging in.") {
      return throwError(() => new Error('Email not verified. Please check your inbox and verify your email.'));
    } else if (error.status === 401) {
      return throwError(() => new Error('Login failed. Please check your credentials and try again.'));
    }

    return throwError(() => new Error('An unexpected error occurred. Please try again.'));
  }

  /**
   * Checks whether the user is authenticated by verifying the JWT token.
   * @returns Boolean indicating whether the user is authenticated.
   */
  isAuthenticated(): boolean {
    const token = this.getToken();

    if (token) {
      const isExpired = this.jwtHelper.isTokenExpired(token);

      if (!isExpired) {
        return true;
      } else {
        this.logout();
      }
    }

    this.logout();  
    return false;
  }

  /**
   * Retrieves stored user details or fetches from localStorage if available.
   * @returns Observable containing the user details.
   */
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

  /**
   * Sets user details and stores them in localStorage.
   * @param user The user details to be stored.
   */
  setUserDetails(user: User): void {
    this.userDetails = user;
    localStorage.setItem('user', JSON.stringify(this.userDetails));
  }

  /**
   * Logs the user out by clearing localStorage and resetting user details.
   * @returns Observable indicating logout success.
   */
  logout(): Observable<any> {
    this.userDetails = null;
    localStorage.removeItem('user');
    localStorage.removeItem('jwtToken');
    return new Observable((observer) => observer.next(true));
  }

  /**
   * Fetches tasks for the authenticated user.
   * @returns Observable containing the list of tasks.
   */
  getTasks(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get(`${this.apiUrl}/tasks`, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Verifies the user's email by sending the verification token.
   * @param token The email verification token.
   * @returns Observable containing the verification result.
   */
  verifyEmail(token: string): Observable<any> {
    const url = `${this.apiUrl}/api/verify-email?token=${token}`;
    return this.http.get(url, { responseType: 'text' }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

/**
 * Sends a password reset link to the user's email address.
 * @param email The user's email address.
 * @returns Observable indicating success or failure.
 */
sendResetPasswordLink(email: string): Observable<any> {
  const url = `${this.apiUrl}/api/forgot-password`;
  console.log('Sending reset password link request to:', url);  // Log the request URL and payload
  console.log('Email:', email);                                // Log email
  return this.http.post(url, { email }).pipe(
    map(response => {
      console.log('Reset link sent successfully:', response);  // Log success response
      return response;
    }),
    catchError((error) => {
      console.error('Error sending reset link:', error);       // Log error
      return this.handleError(error);
    })
  );
}

/**
 * Updates the user's password using the provided reset token and new password.
 * @param token The reset token.
 * @param password The new password.
 * @returns Observable indicating success or failure.
 */
updatePassword(token: string, password: string): Observable<any> {
  const url = `${this.apiUrl}/api/reset-password`;
  console.log('Sending password update request to:', url);  // Log the request URL and payload
  console.log('Token:', token, 'Password:', password);      // Log token and password
  return this.http.post(url, { token, newPassword: password }).pipe(
    map(response => {
      console.log('Password updated successfully:', response);  // Log success response
      return response;
    }),
    catchError((error) => {
      console.error('Error updating password:', error);  // Log error
      return this.handleError(error);
    })
  );
  }
}