import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FirebaseMessagingService {

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  sendTokenToServer(userId: string, fcmToken: string, authToken: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${authToken}`
    });
    const url = `${this.apiUrl}/api/tasks/${userId}/update-token`;
    return this.http.post(url, { fcmToken }, { headers });
  }
}
