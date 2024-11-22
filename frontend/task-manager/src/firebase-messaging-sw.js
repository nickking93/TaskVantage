importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

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

// Configuration
const CONFIG = {
  NOTIFICATION_COOLDOWN: 60000,      // 1 minute
  DUPLICATE_WINDOW: 300000,          // 5 minutes
  MAX_HISTORY_SIZE: 100,            // Maximum number of stored message IDs
  DEFAULT_ICON: '/firebase-logo.png',
  DEFAULT_CLICK_ACTION: '/'
};

// Message history management
class NotificationManager {
  constructor() {
    this.history = new Map();
    this.lastCleanup = Date.now();
  }

  cleanup() {
    const now = Date.now();
    // Only cleanup if it's been more than 1 minute since last cleanup
    if (now - this.lastCleanup < 60000) return;

    const cutoff = now - CONFIG.DUPLICATE_WINDOW;
    for (const [id, timestamp] of this.history.entries()) {
      if (timestamp < cutoff) {
        this.history.delete(id);
      }
    }

    // If still too many entries, remove oldest ones
    if (this.history.size > CONFIG.MAX_HISTORY_SIZE) {
      const sorted = Array.from(this.history.entries())
        .sort((a, b) => a[1] - b[1]);
      const toRemove = sorted.slice(0, this.history.size - CONFIG.MAX_HISTORY_SIZE);
      toRemove.forEach(([id]) => this.history.delete(id));
    }

    this.lastCleanup = now;
  }

  isDuplicate(messageId, timestamp) {
    this.cleanup();
    const existingTimestamp = this.history.get(messageId);
    if (!existingTimestamp) return false;
    return (timestamp - existingTimestamp) < CONFIG.DUPLICATE_WINDOW;
  }

  recordMessage(messageId, timestamp) {
    this.cleanup();
    this.history.set(messageId, timestamp);
  }
}

const notificationManager = new NotificationManager();

// Helper function to create notification options
function createNotificationOptions(payload, messageId) {
  const timestamp = Date.now();
  const notificationData = {
    messageId,
    timestamp,
    clickAction: payload.data?.clickAction || CONFIG.DEFAULT_CLICK_ACTION,
    taskId: payload.data?.taskId,
    messageType: payload.data?.messageType || 'background'
  };

  return {
    body: payload.notification?.body || '',
    icon: payload.notification?.icon || CONFIG.DEFAULT_ICON,
    badge: '/badge-icon.png',
    tag: messageId,
    data: notificationData,
    requireInteraction: true,
    renotify: false,
    silent: false,
    timestamp: timestamp
  };
}

// Handle background messages
messaging.onBackgroundMessage(async function(payload) {
  console.log('[firebase-messaging-sw.js] Received background message:', payload);

  const timestamp = Date.now();
  const messageId = payload.data?.messageId || `${timestamp}-${Math.random().toString(36).substr(2, 9)}`;

  // Check for duplicates
  if (notificationManager.isDuplicate(messageId, timestamp)) {
    console.log('[firebase-messaging-sw.js] Duplicate message detected, skipping notification');
    return;
  }

  // Record the message
  notificationManager.recordMessage(messageId, timestamp);

  // Create and show notification
  const notificationOptions = createNotificationOptions(payload, messageId);
  
  try {
    await self.registration.showNotification(
      payload.notification?.title || 'New Notification',
      notificationOptions
    );
    console.log('[firebase-messaging-sw.js] Notification displayed successfully');
  } catch (error) {
    console.error('[firebase-messaging-sw.js] Error showing notification:', error);
  }
});

// Handle messages from the main app
self.addEventListener('message', async (event) => {
  if (event.data?.type === 'SHOW_NOTIFICATION' && event.data?.payload) {
    const payload = event.data.payload;
    
    // Handle foreground notification similarly to background
    const timestamp = Date.now();
    const messageId = payload.data?.messageId || `${timestamp}-${Math.random().toString(36).substr(2, 9)}`;

    if (notificationManager.isDuplicate(messageId, timestamp)) {
      console.log('[firebase-messaging-sw.js] Duplicate foreground message detected, skipping notification');
      return;
    }

    notificationManager.recordMessage(messageId, timestamp);
    const notificationOptions = createNotificationOptions(payload, messageId);

    try {
      await self.registration.showNotification(
        payload.notification?.title || 'New Notification',
        notificationOptions
      );
      console.log('[firebase-messaging-sw.js] Foreground notification displayed successfully');
    } catch (error) {
      console.error('[firebase-messaging-sw.js] Error showing foreground notification:', error);
    }
  }
});

// Handle notification clicks
self.addEventListener('notificationclick', function(event) {
  console.log('[firebase-messaging-sw.js] Notification clicked:', event);

  event.notification.close();

  const clickAction = event.notification.data?.clickAction || CONFIG.DEFAULT_CLICK_ACTION;
  const urlToOpen = new URL(clickAction, self.location.origin).href;

  event.waitUntil(
    clients.matchAll({
      type: 'window',
      includeUncontrolled: true
    })
    .then(function(clientList) {
      // Try to focus an existing window
      for (const client of clientList) {
        if (client.url === urlToOpen && 'focus' in client) {
          return client.focus();
        }
      }
      // If no existing window, open a new one
      if (clients.openWindow) {
        return clients.openWindow(urlToOpen);
      }
    })
    .catch(function(error) {
      console.error('[firebase-messaging-sw.js] Error handling notification click:', error);
    })
  );
});

// Handle notification close events
self.addEventListener('notificationclose', function(event) {
  console.log('[firebase-messaging-sw.js] Notification closed:', event.notification.data);
});