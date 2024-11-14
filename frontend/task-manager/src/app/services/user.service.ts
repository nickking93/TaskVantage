import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) { }

  // Fetch user settings (whether Google is connected and task sync is enabled)
  getUserSettings(): Observable<any> {
    return this.http.get('/api/user/settings');
  }

  // Update task sync preference
  updateTaskSync(enabled: boolean): Observable<any> {
    return this.http.post('/api/user/task-sync', { enabled });
  }

  // Disconnect Google Authorization
  disconnectGoogleCalendar(): Observable<any> {
    return this.http.post('/api/user/disconnect-google', {});
  }
}