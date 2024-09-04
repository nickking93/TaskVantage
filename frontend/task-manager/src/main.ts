import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

import { initializeApp } from 'firebase/app'; // Import the initializeApp function
import { getMessaging } from 'firebase/messaging'; // Import getMessaging

if (environment.production) {
  enableProdMode();
}

// Initialize Firebase
const firebaseApp = initializeApp(environment.firebaseConfig);

// Optional: Initialize other Firebase services if needed
const messaging = getMessaging(firebaseApp);

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
