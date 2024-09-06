import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { getMessaging, deleteToken } from 'firebase/messaging'; // Import these functions
import { getApp } from 'firebase/app'; // Import Firebase App

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
    
    return this.http.post(url, { fcmToken }, { headers }).pipe(
      catchError(error => {
        console.error('Error sending FCM token to server:', error);
        return throwError(() => new Error('Error sending token to server'));
      })
    );
  }

  clearTokenFromServer(username: string, authToken: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${authToken}`
    });
    const url = `${this.apiUrl}/api/users/${username}/clear-fcm-token`; // Use username instead of userId
    
    return this.http.post(url, {}, { headers }).pipe(
      catchError(error => {
        if (error.status === 404) {
          console.warn('Token not found when trying to clear:', error);
        } else {
          console.error('Error clearing FCM token from server:', error);
        }
        return throwError(() => new Error('Error clearing token from server'));
      })
    );
  }

  removeToken(token: string): Promise<void> {
    const messaging = getMessaging(getApp()); // Get the messaging instance
    return deleteToken(messaging).then((result: boolean) => {
      if (result) {
        console.log('Token successfully removed.');
      } else {
        console.warn('Token removal failed or no token was found.');
      }
    }).catch((error) => {
      console.error('Error removing token:', error);
    });
  }  
}