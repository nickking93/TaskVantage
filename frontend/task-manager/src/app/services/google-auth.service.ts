import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  private clientId = '872741914932-asspmr6jois4ovvr3bvjm4p44csq9qjs.apps.googleusercontent.com';
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  connectGoogleCalendar(userId: string): void {
    if (!userId) {
      throw new Error('User ID is required to connect Google Calendar');
    }

    const authUrl = `${this.apiUrl}/oauth2/authorization/google?userId=${userId}`;
    window.location.href = authUrl;
  }

  checkGoogleCalendarConnection(userId: string): Observable<any> {
    if (!userId) {
      return throwError(() => new Error('User ID is required'));
    }

    const headers = new HttpHeaders().set('X-User-Id', userId);
      
    return this.http.get(`${this.apiUrl}/api/oauth2/google/status`, { headers }).pipe(
      catchError((error) => {
        if (error.status === 401) {
          // Token expired, redirect to login
          localStorage.clear();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }

  initiateGoogleAuth(userId: string): void {
    // Store userId in localStorage before redirect
    localStorage.setItem('google_auth_user_id', userId);
    // Redirect to backend OAuth endpoint
    window.location.href = `${this.apiUrl}/oauth2/authorization/google?userId=${userId}`;
  }

  disconnectGoogleCalendar(): Observable<any> {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('google_auth_user_id');

    if (!token || !userId) {
      return throwError(() => new Error('Token and user ID are required to disconnect Google Calendar'));
    }

    const headers = new HttpHeaders()
      .set('X-User-Id', userId)
      .set('Authorization', `Bearer ${token}`);

    return this.http.post(`${this.apiUrl}/api/oauth2/google/disconnect`, {}, { headers });
  }
}