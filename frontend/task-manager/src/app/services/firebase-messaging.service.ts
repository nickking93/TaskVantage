import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, from, of } from 'rxjs';
import { catchError, retry, tap, switchMap, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { getMessaging, getToken, deleteToken, onMessage } from 'firebase/messaging';
import { getApp } from 'firebase/app';

@Injectable({
  providedIn: 'root'
})
export class FirebaseMessagingService {
  private apiUrl = environment.apiUrl;
  private readonly TOKEN_REFRESH_INTERVAL = 12 * 60 * 60 * 1000; // 12 hours in milliseconds
  private lastTokenTimestamp: number | null = null;
  private processedMessageIds = new Map<string, number>();
  private lastMessageTimestamp: number | null = null;
  private readonly MESSAGE_DEDUPE_WINDOW = 2000; // 2 seconds window for deduplication

  constructor(private http: HttpClient) {
    this.initializeMessaging();
  }

  private initializeMessaging(): void {
    try {
      const messaging = getMessaging(getApp());
      onMessage(messaging, (payload) => {
        // Only handle foreground messages if the app is focused
        if (document.visibilityState === 'visible') {
          this.handleIncomingMessage(payload);
        }
      });
  
      // Clean up old message IDs every minute
      setInterval(() => this.cleanupOldMessages(), 60000);
    } catch (error) {
      console.error('Error initializing Firebase Messaging:', error);
    }
  }

  private handleIncomingMessage(payload: any): void {
    console.log('Raw Firebase payload received:', payload);
    const currentTime = Date.now();
    
    // Generate a unique message ID based on content and time window
    const messageId = this.generateMessageId(payload);
    console.log('Generated messageId:', messageId);
    
    // Check for duplicates
    if (this.isDuplicateMessage(messageId, currentTime)) {
        console.log('Duplicate message detected, ignoring message:', messageId);
        return;
    }

    console.log('Message passed deduplication check:', messageId);
    console.log('Current processed messages:', Array.from(this.processedMessageIds.entries()));

    // Store the message ID with timestamp
    this.processedMessageIds.set(messageId, currentTime);
    this.lastMessageTimestamp = currentTime;

    // Show the notification using the Notifications API
    if (Notification.permission === 'granted') {
        const notificationTitle = payload.notification?.title || 'Notification';
        const notificationOptions = {
            body: payload.notification?.body,
            tag: messageId, // Use messageId as tag to prevent duplicates
            renotify: false, // Prevent renotification for same tag
            silent: false,
            data: {
                messageId,
                timestamp: currentTime
            }
        };

        try {
            console.log('Creating notification with options:', notificationOptions);
            new Notification(notificationTitle, notificationOptions);
        } catch (error) {
            console.error('Error showing notification:', error);
        }
    }
}

  private generateMessageId(payload: any): string {
    const title = payload.notification?.title || '';
    const body = payload.notification?.body || '';
    const timestamp = Math.floor(Date.now() / this.MESSAGE_DEDUPE_WINDOW);
    return `${title}:${body}:${timestamp}`;
  }

  private isDuplicateMessage(messageId: string, currentTime: number): boolean {
    // Check if we've seen this exact message ID recently
    const lastSeenTime = this.processedMessageIds.get(messageId);
    if (lastSeenTime) {
      return (currentTime - lastSeenTime) < this.MESSAGE_DEDUPE_WINDOW;
    }

    // Check if we've received any message too recently
    if (this.lastMessageTimestamp) {
      return (currentTime - this.lastMessageTimestamp) < this.MESSAGE_DEDUPE_WINDOW;
    }

    return false;
  }

  private cleanupOldMessages(): void {
    const currentTime = Date.now();
    for (const [messageId, timestamp] of this.processedMessageIds.entries()) {
      if (currentTime - timestamp > 60000) { // Remove entries older than 1 minute
        this.processedMessageIds.delete(messageId);
      }
    }
  }

  sendTokenToServer(userId: string, fcmToken: string, authToken: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    });
    const url = `${this.apiUrl}/api/tasks/${userId}/update-token`;
    
    return this.http.post(url, { fcmToken }, { headers }).pipe(
      retry(3), // Retry failed requests up to 3 times
      tap(() => console.log('FCM token successfully sent to server')),
      catchError(this.handleError)
    );
  }

  clearTokenFromServer(username: string, authToken: string): Observable<any> {
    if (!username || !authToken) {
      return throwError(() => new Error('Username and auth token are required'));
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    });
    const url = `${this.apiUrl}/api/users/${username}/clear-fcm-token`;
    
    return this.removeTokenFromDevice().pipe(
      switchMap(() => this.http.post(url, {}, { headers })),
      retry(3),
      tap(() => console.log('FCM token successfully cleared from server')),
      catchError(this.handleError)
    );
  }

  async requestPermissionAndGetToken(): Promise<string | null> {
    try {
      const messaging = getMessaging(getApp());
      const permission = await Notification.requestPermission();
      
      if (permission === 'granted') {
        const currentToken = await getToken(messaging, {
          vapidKey: environment.firebaseConfig.vapidKey
        });

        if (currentToken) {
          this.lastTokenTimestamp = Date.now();
          this.scheduleTokenRefresh();
          return currentToken;
        }
      }
      
      console.warn('No registration token available.');
      return null;
    } catch (error) {
      console.error('Error getting permission or token:', error);
      return null;
    }
  }

  private scheduleTokenRefresh(): void {
    setInterval(() => {
      this.refreshToken();
    }, this.TOKEN_REFRESH_INTERVAL);
  }

  private async refreshToken(): Promise<void> {
    const newToken = await this.requestPermissionAndGetToken();
    if (newToken) {
      // Get the current user's auth token and ID
      const authToken = localStorage.getItem('authToken'); // Adjust based on your auth storage
      const userId = localStorage.getItem('userId'); // Adjust based on your user ID storage
      
      if (authToken && userId) {
        this.sendTokenToServer(userId, newToken, authToken).subscribe();
      }
    }
  }

  private removeTokenFromDevice(): Observable<void> {
    const messaging = getMessaging(getApp());
    return from(deleteToken(messaging)).pipe(
      map(() => {
        console.log('Device token successfully removed');
        this.lastTokenTimestamp = null;
        return void 0;
      }),
      catchError(error => {
        console.error('Error removing device token:', error);
        return throwError(() => error);
      })
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    } else {
      // Server-side error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
    }
    
    console.error(errorMessage);
    return throwError(() => new Error(errorMessage));
  }
}