import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('google_auth_user_id');
    return new HttpHeaders()
      .set('Authorization', `Bearer ${token}`)
      .set('X-User-Id', userId || '');
  }

  // Get user settings including task sync status
  getUserSettings(): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/api/oauth2/google/sync-status`,
      { headers: this.getHeaders() }
    );
  }

  // Update task sync preference
  updateTaskSync(enabled: boolean): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/api/oauth2/google/sync-settings`,
      { enabled },
      { headers: this.getHeaders() }
    );
  }
}