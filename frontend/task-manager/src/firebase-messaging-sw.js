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

// We'll only use onBackgroundMessage and remove the push event listener
messaging.onBackgroundMessage(function(payload) {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  
  const messageId = `${payload.notification.title}:${payload.notification.body}:${Date.now()}`;
  
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/firebase-logo.png',
    tag: messageId, // Add tag to prevent duplicates
    renotify: false
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});