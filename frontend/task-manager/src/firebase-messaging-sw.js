importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyA0Q6KvMqU0IBnem3sCGWeOmI2lOtlTYmw",
  authDomain: "taskvantage-c1425.firebaseapp.com",
  projectId: "taskvantage-c1425",
  storageBucket: "taskvantage-c1425.appspot.com",
  messagingSenderId: "281462521187",
  appId: "1:281462521187:web:497c15254b6817070c6756",
  measurementId: "G-6FNFVE7SQ1"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function(payload) {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  // Customize notification here
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/firebase-logo.png'
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

self.addEventListener('push', function(event) {
  const notificationData = event.data.json();

  const notificationTitle = notificationData.notification.title;
  const notificationOptions = {
    body: notificationData.notification.body,
    icon: '/firebase-logo.png'
  };

  event.waitUntil(
    self.registration.showNotification(notificationTitle, notificationOptions)
  );
});
