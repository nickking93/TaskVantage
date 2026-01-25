import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, from } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
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
  private refreshTokenUrl = `${this.apiUrl}/api/refresh-token`;
  private userDetails: User | null = null;
  private jwtHelper = new JwtHelperService();
  private db: IDBDatabase | null = null;
  private readonly DB_NAME = 'TaskVantageAuth';
  private readonly STORE_NAME = 'tokens';

  constructor(private http: HttpClient) {
    this.initializeDB();
  }

  /**
   * Initializes IndexedDB for PWA token storage
   */
  private async initializeDB(): Promise<void> {
    if (!this.isPWA()) return;

    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.DB_NAME, 1);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        this.db = request.result;
        resolve();
      };

      request.onupgradeneeded = (event: IDBVersionChangeEvent) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(this.STORE_NAME)) {
          db.createObjectStore(this.STORE_NAME);
        }
      };
    });
  }

  /**
   * Checks if the app is running as an installed PWA
   */
  private isPWA(): boolean {
    return window.matchMedia('(display-mode: standalone)').matches ||
           window.navigator.standalone ||
           document.referrer.includes('android-app://');
  }

  /**
   * Stores authentication tokens either in IndexedDB (PWA) or localStorage
   */
  private async storeTokens(tokens: { token: string, refreshToken: string }): Promise<void> {
    if (this.isPWA() && this.db) {
      const transaction = this.db.transaction(this.STORE_NAME, 'readwrite');
      const store = transaction.objectStore(this.STORE_NAME);
      await Promise.all([
        new Promise<void>(resolve => {
          store.put(tokens.token, 'jwtToken');
          store.put(tokens.refreshToken, 'refreshToken');
          transaction.oncomplete = () => resolve();
        })
      ]);
    } else {
      localStorage.setItem('jwtToken', tokens.token);
      localStorage.setItem('refreshToken', tokens.refreshToken);
    }
  }

  /**
   * Retrieves tokens from storage
   */
  private async getStoredTokens(): Promise<{ token: string | null, refreshToken: string | null }> {
    if (this.isPWA() && this.db) {
      const transaction = this.db.transaction(this.STORE_NAME, 'readonly');
      const store = transaction.objectStore(this.STORE_NAME);
      const token = await new Promise<string | null>(resolve => {
        const request = store.get('jwtToken');
        request.onsuccess = () => resolve(request.result);
      });
      const refreshToken = await new Promise<string | null>(resolve => {
        const request = store.get('refreshToken');
        request.onsuccess = () => resolve(request.result);
      });
      return { token, refreshToken };
    } else {
      return {
        token: localStorage.getItem('jwtToken'),
        refreshToken: localStorage.getItem('refreshToken')
      };
    }
  }

  /**
   * Clears stored tokens
   */
  private async clearTokens(): Promise<void> {
    if (this.isPWA() && this.db) {
      const transaction = this.db.transaction(this.STORE_NAME, 'readwrite');
      const store = transaction.objectStore(this.STORE_NAME);
      await Promise.all([
        new Promise<void>(resolve => {
          store.clear();
          transaction.oncomplete = () => resolve();
        })
      ]);
    }
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  }

  /**
 * Sets the persistent login state
 */
async setPersistentLogin(enabled: boolean): Promise<void> {
  if (this.isPWA() && this.db) {
    const transaction = this.db.transaction(this.STORE_NAME, 'readwrite');
    const store = transaction.objectStore(this.STORE_NAME);
    await new Promise<void>(resolve => {
      store.put(enabled, 'persistentLogin');
      transaction.oncomplete = () => resolve();
    });
  } else {
    localStorage.setItem('persistentLogin', enabled.toString());
  }
}

  /**
   * Verifies social login using provider and authorization code
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
   * Handles login by sending credentials and storing user data on success
   */
  login(credentials: { username: string; password: string; fcmToken?: string }): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<any>(this.loginUrl, credentials, { headers }).pipe(
      switchMap(async (response: any) => {
        const userId = response.userId;
        const username = response.username;
        const token = response.token;
        const refreshToken = response.refreshToken;

        this.userDetails = {
          id: userId,
          username: username,
          password: '',
          token: token
        };

        await this.storeTokens({ token, refreshToken });
        await this.storeUserDetails(this.userDetails);

        return this.userDetails;
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Registers a new user
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
   * Stores user details in appropriate storage
   */
  private async storeUserDetails(user: User): Promise<void> {
    if (this.isPWA() && this.db) {
      const transaction = this.db.transaction(this.STORE_NAME, 'readwrite');
      const store = transaction.objectStore(this.STORE_NAME);
      await new Promise<void>(resolve => {
        store.put(JSON.stringify(user), 'userDetails');
        transaction.oncomplete = () => resolve();
      });
    } else {
      localStorage.setItem('user', JSON.stringify(user));
    }
  }

  /**
   * Retrieves stored user details
   */
  private async getStoredUserDetails(): Promise<User | null> {
    if (this.isPWA() && this.db) {
      const transaction = this.db.transaction(this.STORE_NAME, 'readonly');
      const store = transaction.objectStore(this.STORE_NAME);
      const userStr = await new Promise<string | null>(resolve => {
        const request = store.get('userDetails');
        request.onsuccess = () => resolve(request.result);
      });
      return userStr ? JSON.parse(userStr) : null;
    } else {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    }
  }

  /**
   * Refreshes the access token using the refresh token
   */
  refreshToken(): Observable<string> {
    return from(this.getStoredTokens()).pipe(
      switchMap(({ refreshToken }) => {
        if (!refreshToken) {
          return throwError(() => new Error('No refresh token available'));
        }
        return this.http.post<{ token: string }>(`${this.refreshTokenUrl}`, { refreshToken });
      }),
      map(response => response.token),
      catchError(error => {
        return throwError(() => error);
      })
    );
  }

  /**
   * Gets the authentication token
   */
  async getAuthToken(): Promise<string | null> {
    const { token } = await this.getStoredTokens();
    return token;
  }

  /**
   * Creates HTTP headers with authorization token
   */
  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('JWT token is missing.');
    }
    return new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    });
  }  

  /**
   * Checks if the user is authenticated
   */
  async isAuthenticated(): Promise<boolean> {
    const { token } = await this.getStoredTokens();
    if (!token) {
      return false;
    }
    
    try {
      const isExpired = this.jwtHelper.isTokenExpired(token);
      if (isExpired) {
        // Try to refresh the token
        try {
          await this.refreshToken().toPromise();
          return true;
        } catch {
          await this.clearTokens();
          return false;
        }
      }
      return true;
    } catch {
      await this.clearTokens();
      return false;
    }
  }

  /**
   * Gets user details
   */
  getUserDetails(): Observable<User> {
    return from(this.getStoredUserDetails()).pipe(
      map(user => {
        if (!user) {
          throw new Error('User not authenticated.');
        }
        return user;
      })
    );
  }

  /**
   * Sets user details
   */
  async setUserDetails(user: User): Promise<void> {
    this.userDetails = user;
    await this.storeUserDetails(user);
  }

  /**
   * Handles user logout
   */
  logout(): Observable<any> {
    return from(this.clearTokens()).pipe(
      map(() => {
        this.userDetails = null;
        return true;
      })
    );
  }

  /**
   * Gets tasks for the authenticated user
   */
  getTasks(): Observable<any> {
    const headers = this.getAuthHeaders();
    return this.http.get(`${this.apiUrl}/tasks`, { headers }).pipe(
      catchError(this.handleError)
    );
  }
  

  /**
   * Verifies user's email
   */
  verifyEmail(token: string): Observable<any> {
    const url = `${this.apiUrl}/api/verify-email?token=${token}`;
    return this.http.get(url, { responseType: 'text' }).pipe(
      map(response => response),
      catchError(this.handleError)
    );
  }

  /**
   * Sends password reset link
   */
  sendResetPasswordLink(email: string): Observable<any> {
    const url = `${this.apiUrl}/api/forgot-password`;
    return this.http.post(url, { email }).pipe(
      map(response => response),
      catchError((error) => {
        return this.handleError(error);
      })
    );
  }

  /**
   * Updates password using reset token
   */
  updatePassword(token: string, password: string): Observable<any> {
    const url = `${this.apiUrl}/api/reset-password`;
    return this.http.post(url, { token, newPassword: password }).pipe(
      map(response => response),
      catchError((error) => {
        return this.handleError(error);
      })
    );
  }

  /**
   * Handles HTTP errors
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    if (error.status === 401 && error.error.message === "Your email is not verified. Please verify your email before logging in.") {
      return throwError(() => new Error('Email not verified. Please check your inbox and verify your email.'));
    } else if (error.status === 401) {
      return throwError(() => new Error('Login failed. Please check your credentials and try again.'));
    }

    return throwError(() => new Error('An unexpected error occurred. Please try again.'));
  }
}