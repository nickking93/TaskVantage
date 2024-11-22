import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, from, of } from 'rxjs';
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
    console.log('Starting FirebaseMessagingService initialize');
    
    const jwtToken = localStorage.getItem('jwtToken');
    console.log('JWT Token exists:', !!jwtToken);
    
    if (!jwtToken) {
      console.log('User not logged in, skipping notification initialization');
      return;
    }

    if (this.isInitialized) {
      console.log('Firebase messaging already initialized');
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
      console.log('Firebase messaging initialized successfully');

    } catch (error) {
      console.error('Error initializing Firebase Messaging:', error);
      throw error;
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
        console.error('No service worker registration available');
        return null;
      }

      // Ensure service worker is active
      if (this.serviceWorkerRegistration.installing || this.serviceWorkerRegistration.waiting) {
        await this.waitForServiceWorkerActivation(this.serviceWorkerRegistration);
      }

      const token = await this.getMessagingToken();
      if (token) {
        console.log('Got FCM token:', token);
        await this.updateTokenInBackend(token);
        return token;
      }
      return null;
    } catch (error) {
      console.error('Error getting and updating token:', error);
      return null;
    }
  }

  private checkBrowserSupport(): boolean {
    if (!('serviceWorker' in navigator) || !('Notification' in window)) {
      console.warn('Notifications are not supported in this browser');
      return false;
    }
    return true;
  }

  private async initializeServiceWorker(): Promise<void> {
    console.log('Initializing service worker');
    
    try {
      // Unregister existing service workers
      const existingRegistrations = await navigator.serviceWorker.getRegistrations();
      for (const registration of existingRegistrations) {
        if (registration.scope.includes('firebase-cloud-messaging-push-scope')) {
          await registration.unregister();
        }
      }

      // Register new service worker
      this.serviceWorkerRegistration = await this.registerServiceWorker();
      console.log('Service worker registered successfully');

      // Wait for activation
      await this.waitForServiceWorkerActivation(this.serviceWorkerRegistration);
      console.log('Service worker activated successfully');
    } catch (error) {
      console.error('Error in service worker initialization:', error);
      throw error;
    }
  }

  private async initializeMessaging(): Promise<void> {
    console.log('Initializing Firebase messaging');
    const messaging = getMessaging(getApp());
    
    onMessage(messaging, (payload) => {
      console.log('Foreground message received:', payload);
      this.showNotification(payload);
    });
  }

  async requestPermissionAndGetToken(): Promise<string | null> {
    if (this.permissionRequestInProgress) {
      console.log('Permission request already in progress');
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

      console.log('Requesting Notification permission from browser');
      const permission = await Notification.requestPermission();
      console.log('Browser permission request result:', permission);
      
      if (permission === 'granted') {
        return this.getAndUpdateToken();
      }
      
      if (permission === 'denied') {
        localStorage.setItem('notificationPermission', 'denied');
      }
      
      return null;
    } catch (error) {
      console.error('Error requesting permission:', error);
      return null;
    } finally {
      this.permissionRequestInProgress = false;
    }
  }

  private async getMessagingToken(): Promise<string | null> {
    try {
      const messaging = getMessaging(getApp());
      console.log('Getting messaging token...');
      
      let options: { vapidKey: string; serviceWorkerRegistration?: ServiceWorkerRegistration } = {
        vapidKey: environment.firebaseConfig.vapidKey
      };

      if (this.serviceWorkerRegistration) {
        options.serviceWorkerRegistration = this.serviceWorkerRegistration;
      }

      const currentToken = await getToken(messaging, options);

      if (currentToken) {
        console.log('Messaging token obtained successfully');
        localStorage.setItem('notificationPermission', 'granted');
        return currentToken;
      } else {
        console.warn('No messaging token received');
      }
      
      return null;
    } catch (error) {
      console.error('Error getting messaging token:', error);
      return null;
    }
  }

  private async registerServiceWorker(): Promise<ServiceWorkerRegistration> {
    try {
      const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', {
        scope: '/'
      });
      console.log('Service Worker registered with scope:', registration.scope);
      return registration;
    } catch (error) {
      console.error('Service Worker registration failed:', error);
      throw error;
    }
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

    console.log('Updating FCM token in backend:', token);
    return this.http.post<void>(`${this.apiUrl}/api/update-fcm-token`, { fcmToken: token }, { headers })
      .pipe(
        tap(() => console.log('FCM token successfully updated in backend')),
        catchError((error) => {
          console.error('Error updating FCM token in backend:', error);
          throw error;
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
      tap(() => console.log('FCM token cleared from server')),
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('An error occurred:', error);
    return throwError(() => new Error('An error occurred while processing the request'));
  }
}