import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  private clientId = '872741914932-asspmr6jois4ovvr3bvjm4p44csq9qjs.apps.googleusercontent.com';
  private backendUrl = 'http://localhost:8080';

  constructor(
    private http: HttpClient,
    private router: Router
  ) { }

  connectGoogleCalendar(userId: string): void {
    if (!userId) {
      console.error('User ID is required to connect Google Calendar.');
      return;
    }

    // Directly redirect to OAuth endpoint with userId as parameter
    const authUrl = `${this.backendUrl}/oauth2/authorization/google?userId=${userId}`;
    console.log('Redirecting to:', authUrl);
    window.location.href = authUrl;
  }

  checkGoogleCalendarConnection(userId: string): Observable<any> {
    if (!userId) {
      console.error('User ID is required to check Google Calendar connection.');
      return new Observable();
    }

    const headers = new HttpHeaders().set('X-User-Id', userId);
    return this.http.get(`${this.backendUrl}/api/oauth2/google/status`, { headers });
  }
}