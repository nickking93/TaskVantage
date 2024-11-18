import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
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
      console.error('User ID is required to connect Google Calendar.');
      return;
    }

    const authUrl = `${this.apiUrl}/oauth2/authorization/google?userId=${userId}`;
    console.log('Redirecting to:', authUrl);
    window.location.href = authUrl;
  }

  checkGoogleCalendarConnection(userId: string): Observable<any> {
    if (!userId) {
      console.error('User ID is required to check Google Calendar connection.');
      return new Observable();
    }
  
    // Get the token using the same key as LoginComponent
    const token = localStorage.getItem('token');
  
    if (!token) {
      console.error('No JWT token found');
      return new Observable();
    }
  
    const headers = new HttpHeaders()
      .set('X-User-Id', userId)
      .set('Authorization', `Bearer ${token}`);
      
    return this.http.get(`${this.apiUrl}/api/oauth2/google/status`, { headers });
  }

  disconnectGoogleCalendar(): Observable<any> {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('google_auth_user_id');

    if (!token || !userId) {
      console.error('Missing token or user ID');
      return new Observable();
    }

    const headers = new HttpHeaders()
      .set('X-User-Id', userId)
      .set('Authorization', `Bearer ${token}`);

    return this.http.post(`${this.apiUrl}/api/oauth2/google/disconnect`, {}, { headers });
  }
}