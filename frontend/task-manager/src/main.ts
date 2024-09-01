import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

import { initializeApp } from 'firebase/app';
import { getAnalytics } from 'firebase/analytics';
import { getMessaging, onMessage, getToken } from 'firebase/messaging';
import { HttpClient } from '@angular/common/http';

if (environment.production) {
  enableProdMode();
}

// Initialize Firebase
try {
  const app = initializeApp(environment.firebaseConfig);

  // Conditionally initialize Analytics if in production mode or as needed
  if (environment.production) {
    const analytics = getAnalytics(app);
  }

  // Initialize Firebase Cloud Messaging
  const messaging = getMessaging(app);

  // Register the service worker for Firebase Messaging
  navigator.serviceWorker.register('/firebase-messaging-sw.js')
    .then((registration) => {
      console.log('Service Worker registered with scope:', registration.scope);

      // Request permission to receive notifications
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          console.log('Notification permission granted.');
          getToken(messaging, { vapidKey: environment.firebaseConfig.vapidKey, serviceWorkerRegistration: registration })
            .then((currentToken) => {
              if (currentToken) {
                console.log('FCM Token:', currentToken);
                // Send the token to your server
                sendTokenToServer(currentToken);
              } else {
                console.log('No registration token available. Request permission to generate one.');
              }
            }).catch((err) => {
              console.log('An error occurred while retrieving token. ', err);
            });
        } else {
          console.log('Unable to get permission to notify.');
        }
      });

      // Handle incoming messages when the app is in the foreground
      onMessage(messaging, (payload) => {
        console.log('Message received. ', payload);
        // Customize notification handling here
      });
    })
    .catch((err) => {
      console.error('Service Worker registration failed: ', err);
    });

} catch (error) {
  console.error('Firebase initialization error:', error);
}

// Function to send the token to the backend server
function sendTokenToServer(token: string) {
  const http = platformBrowserDynamic().injector.get(HttpClient);
  http.post(`${environment.apiUrl}/api/users/update-token`, { token })
    .subscribe(response => {
      console.log('Token sent to server successfully:', response);
    }, error => {
      console.error('Error sending token to server:', error);
    });
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
