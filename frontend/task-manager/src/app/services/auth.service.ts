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

  verifySocialLogin(authCode: string, provider: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = { authCode, provider };
    return this.http.post<any>(this.socialLoginUrl, body, { headers }).pipe(
      map(response => {
        console.log('Social login successful', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  login(credentials: { username: string; password: string; fcmToken?: string }): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<User>(this.loginUrl, credentials, { headers }).pipe(
      map((response: any) => {
        const userId = response.userId;
        const username = response.username;
        const token = response.token;

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

  register(credentials: { username: string; password: string }): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post(this.registerUrl, credentials, { headers }).pipe(
      map((response: any) => {
        if (response && response.message && response.message.includes('Registration successful')) {
          console.log('Registration successful, email verification sent', response);
          return response; // Return the response directly as the success case
        } else {
          console.error('Registration failed', response);
          throw new Error(response.message || 'Registration failed.');
        }
      }),
      catchError(this.handleError)
    );
  }   

  private getToken(): string | null {
    return localStorage.getItem('jwtToken'); // Ensure consistency with key name
  }

  public getAuthToken(): string | null {
    return this.getToken();
  }

  public getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Error:', error.message);
    return throwError(() => new Error('Error: ' + error.message));
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    console.log('isAuthenticated: Retrieved token:', token);
  
    if (token) {
      const isExpired = this.jwtHelper.isTokenExpired(token);
      console.log('isAuthenticated: Is token expired?', isExpired);
  
      if (!isExpired) {
        console.log('isAuthenticated: Token is valid and not expired.');
        return true;
      } else {
        console.log('isAuthenticated: Token is expired, logging out.');
      }
    } else {
      console.log('isAuthenticated: No token found, user is not authenticated.');
    }
  
    this.logout();  
    return false;
  }  

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

  setUserDetails(user: User): void {
    this.userDetails = user;
    localStorage.setItem('user', JSON.stringify(this.userDetails));
  }

  logout(): Observable<any> {
    this.userDetails = null;
    localStorage.removeItem('user');
    localStorage.removeItem('jwtToken');
    return new Observable((observer) => observer.next(true));
  }

  getTasks(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get(`${this.apiUrl}/tasks`, { headers }).pipe(
      catchError(this.handleError)
    );
  }
}