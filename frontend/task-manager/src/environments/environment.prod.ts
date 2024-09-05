export const environment = {
  production: true,  // Change to true in environment.prod.ts
  apiUrl: 'https://taskvantage-backend-hvfsayf4bbajhzh8.eastus-01.azurewebsites.net',
  googleClientId: 'YOUR_GOOGLE_CLIENT_ID',
  appleClientId: 'YOUR_APPLE_CLIENT_ID',
  appleRedirectURI: 'YOUR_APPLE_REDIRECT_URI',
  googleRedirectURI: 'YOUR_GOOGLE_REDIRECT_URI',
  facebookRedirectURI: 'YOUR_FACEBOOK_REDIRECT_URI',
  firebaseConfig: {
    apiKey: "AIzaSyA0Q6KvMqU0IBnem3sCGWeOmI2lOtlTYmw",
    authDomain: "taskvantage-c1425.firebaseapp.com",
    projectId: "taskvantage-c1425",
    storageBucket: "taskvantage-c1425.appspot.com",
    messagingSenderId: "281462521187",
    appId: "1:281462521187:web:497c15254b6817070c6756",
    measurementId: "G-6FNFVE7SQ1",
    vapidKey: "BBmYZtJYON6McY9uZJAZZiEtpMav2WTBHG3N80JGRayNi8v6mhb6xubym__L7xTkNHbZEjbNhu6TSY9sA32fnhM"  // Replace with your VAPID public key
  }
};
