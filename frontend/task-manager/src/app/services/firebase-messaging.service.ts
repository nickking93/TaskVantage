import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, retry, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { getMessaging, getToken, deleteToken, onMessage } from 'firebase/messaging';
import { getApp } from 'firebase/app';

@Injectable({
  providedIn: 'root'
})
export class FirebaseMessagingService {
  private apiUrl = environment.apiUrl;
  private isInitialized = false;
  private serviceWorkerRegistration: ServiceWorkerRegistration | null = null;
  private permissionRequestInProgress = false;

  constructor(private http: HttpClient) {}

  async initialize(): Promise<void> {
    const jwtToken = localStorage.getItem('jwtToken');

    if (!jwtToken) {
      return;
    }

    if (this.isInitialized) {
      return;
    }

    try {
      if (!this.checkBrowserSupport()) {
        return;
      }

      await this.initializeServiceWorker();
      await this.initializeMessaging();

      // Request permission and get token if not already done
      if (Notification.permission === 'granted') {
        await this.getAndUpdateToken();
      }

      this.isInitialized = true;

    } catch (error) {
      // Firebase messaging initialization failed - this is non-fatal
      // The app can continue without push notifications
      this.isInitialized = false;
    }
  }

  private async waitForServiceWorkerActivation(registration: ServiceWorkerRegistration): Promise<void> {
    if (registration.active) {
      return;
    }

    return new Promise((resolve) => {
      registration.addEventListener('activate', () => resolve(), { once: true });
    });
  }

  private async getAndUpdateToken(): Promise<string | null> {
    try {
      // Wait for service worker to be ready
      if (!this.serviceWorkerRegistration) {
        return null;
      }

      // Ensure service worker is active
      if (this.serviceWorkerRegistration.installing || this.serviceWorkerRegistration.waiting) {
        await this.waitForServiceWorkerActivation(this.serviceWorkerRegistration);
      }

      const token = await this.getMessagingToken();
      if (token) {
        await this.updateTokenInBackend(token);
        return token;
      }
      return null;
    } catch (error) {
      return null;
    }
  }

  private checkBrowserSupport(): boolean {
    if (!('serviceWorker' in navigator) || !('Notification' in window)) {
      return false;
    }
    return true;
  }

  private async initializeServiceWorker(): Promise<void> {
    // Unregister existing service workers
    const existingRegistrations = await navigator.serviceWorker.getRegistrations();
    for (const registration of existingRegistrations) {
      if (registration.scope.includes('firebase-cloud-messaging-push-scope')) {
        await registration.unregister();
      }
    }

    // Register new service worker
    this.serviceWorkerRegistration = await this.registerServiceWorker();

    // Wait for activation
    await this.waitForServiceWorkerActivation(this.serviceWorkerRegistration);
  }

  private async initializeMessaging(): Promise<void> {
    const messaging = getMessaging(getApp());

    onMessage(messaging, (payload) => {
      this.showNotification(payload);
    });
  }

  async requestPermissionAndGetToken(): Promise<string | null> {
    if (this.permissionRequestInProgress) {
      return null;
    }

    this.permissionRequestInProgress = true;

    try {
      if (!this.checkBrowserSupport()) {
        return null;
      }

      // Make sure service worker is initialized
      if (!this.isInitialized) {
        await this.initialize();
      }

      const permission = await Notification.requestPermission();
      
      if (permission === 'granted') {
        return this.getAndUpdateToken();
      }
      
      if (permission === 'denied') {
        localStorage.setItem('notificationPermission', 'denied');
      }
      
      return null;
    } catch (error) {
      return null;
    } finally {
      this.permissionRequestInProgress = false;
    }
  }

  private async getMessagingToken(): Promise<string | null> {
    try {
      const messaging = getMessaging(getApp());

      let options: { vapidKey: string; serviceWorkerRegistration?: ServiceWorkerRegistration } = {
        vapidKey: environment.firebaseConfig.vapidKey
      };

      if (this.serviceWorkerRegistration) {
        options.serviceWorkerRegistration = this.serviceWorkerRegistration;
      }

      const currentToken = await getToken(messaging, options);

      if (currentToken) {
        localStorage.setItem('notificationPermission', 'granted');
        return currentToken;
      }

      return null;
    } catch (error) {
      return null;
    }
  }

  private async registerServiceWorker(): Promise<ServiceWorkerRegistration> {
    const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', {
      scope: '/'
    });
    return registration;
  }

  private async showNotification(payload: any): Promise<void> {
    if (!this.serviceWorkerRegistration || Notification.permission !== 'granted') {
      return;
    }

    const messageId = payload.data?.messageId || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
    
    this.serviceWorkerRegistration.active?.postMessage({
      type: 'SHOW_NOTIFICATION',
      payload: {
        ...payload,
        data: {
          ...payload.data,
          messageId,
          messageType: 'foreground'
        }
      }
    });
  }

  private updateTokenInBackend(token: string): Promise<void> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
    });

    return this.http.post<void>(`${this.apiUrl}/api/update-fcm-token`, { fcmToken: token }, { headers })
      .pipe(
        catchError(() => {
          // Token update failed - non-fatal, notifications may not work
          return of(undefined);
        })
      ).toPromise();
  }

  clearTokenFromServer(username: string, authToken: string): Observable<any> {
    if (!username || !authToken) {
      return throwError(() => new Error('Username and auth token are required'));
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${authToken}`,
      'Content-Type': 'application/json'
    });
    
    return this.http.post(`${this.apiUrl}/api/users/${username}/clear-fcm-token`, {}, { headers }).pipe(
      retry(3),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    return throwError(() => new Error('An error occurred while processing the request'));
  }
}