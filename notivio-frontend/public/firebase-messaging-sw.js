// Firebase Service Worker — handles background push notifications
// This file MUST be in /public so the browser can access it at the root scope.
// It is intentionally NOT processed by Next.js/webpack.

importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.12.0/firebase-messaging-compat.js');

// NOTE: These values are public-safe (they are the NEXT_PUBLIC_ ones from your .env)
// They need to be hardcoded here because this file is not processed by Next.js.
// After you configure Firebase, replace these placeholder values.
firebase.initializeApp({
  apiKey:            self.__FIREBASE_API_KEY__            || "AIzaSyBJjT_mJZPwY0-adHQ4yU5ObmtINmNErwI",
  authDomain:        self.__FIREBASE_AUTH_DOMAIN__        || "notivio-4e698.firebaseapp.com",
  projectId:         self.__FIREBASE_PROJECT_ID__         || "notivio-4e698",
  storageBucket:     self.__FIREBASE_STORAGE_BUCKET__     || "notivio-4e698.firebasestorage.app",
  messagingSenderId: self.__FIREBASE_MESSAGING_SENDER_ID__ || "316631568894",
  appId:             self.__FIREBASE_APP_ID__             || "1:316631568894:web:66c3a6861bb7092b22b0af",
});

const messaging = firebase.messaging();

// Handle background messages (app closed or in background tab)
messaging.onBackgroundMessage((payload) => {
  console.log('[Service Worker] Background message received:', payload);

  const notificationTitle = payload.notification?.title || '🔔 Notivio Reminder';
  const notificationOptions = {
    body:  payload.notification?.body || 'You have a new reminder.',
    icon:  '/icon-192x192.png',
    badge: '/icon-192x192.png',
    data:  payload.data,
    actions: [
      { action: 'open',    title: 'Open Notivio' },
      { action: 'dismiss', title: 'Dismiss' },
    ],
    requireInteraction: true, // keeps notification visible until user interacts
    vibrate: [200, 100, 200],
    tag: payload.data?.taskId || 'notivio-notification', // replaces previous notification for same task
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click — open/focus the app
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  if (event.action === 'dismiss') return;

  const taskId = event.notification.data?.taskId;
  const targetUrl = taskId
    ? `http://localhost:3000/dashboard/tasks?highlight=${taskId}`
    : 'http://localhost:3000/dashboard';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // If the app is already open, focus it
      for (const client of clientList) {
        if (client.url.includes('localhost:3000') && 'focus' in client) {
          client.navigate(targetUrl);
          return client.focus();
        }
      }
      // Otherwise open a new window
      if (clients.openWindow) {
        return clients.openWindow(targetUrl);
      }
    })
  );
});
