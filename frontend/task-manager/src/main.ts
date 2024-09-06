import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

import { initializeApp } from 'firebase/app';
import { getMessaging, onMessage } from 'firebase/messaging';

if (environment.production) {
  enableProdMode();
}

// Initialize Firebase
const firebaseApp = initializeApp(environment.firebaseConfig);

// Initialize Firebase Messaging
const messaging = getMessaging(firebaseApp);

// Register the service worker for push notifications
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/firebase-messaging-sw.js')
    .then((registration) => {
      console.log('Service Worker registered with scope:', registration.scope);

      // Handle foreground messages
      onMessage(messaging, (payload) => {
        console.log('Message received. ', payload);
        // Customize notification here if needed
      });
    })
    .catch(err => console.error('Service Worker registration failed: ', err));
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));