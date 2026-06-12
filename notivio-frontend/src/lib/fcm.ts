import { messaging } from '@/lib/firebase';
import { getToken, onMessage } from 'firebase/messaging';
import { api } from '@/lib/api';

const VAPID_KEY = process.env.NEXT_PUBLIC_FIREBASE_VAPID_KEY;

/**
 * Requests notification permission from the browser, gets the FCM token,
 * and registers it with our backend.
 * Returns true on success, false if denied or any error.
 */
export async function initializeFCM(): Promise<boolean> {
  if (!messaging) {
    console.warn('[FCM] Messaging not initialised (likely SSR or missing config)');
    return false;
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.info('[FCM] Notification permission denied');
      return false;
    }

    // Register service worker first
    const swRegistration = await navigator.serviceWorker.register(
      '/firebase-messaging-sw.js',
      { scope: '/' }
    );
    await navigator.serviceWorker.ready;

    const token = await getToken(messaging, {
      vapidKey: VAPID_KEY,
      serviceWorkerRegistration: swRegistration,
    });

    if (!token) {
      console.warn('[FCM] Failed to get FCM token');
      return false;
    }

    console.info('[FCM] Token obtained, registering with backend...');

    // Send token to our Spring Boot backend
    await api.post('/notifications/device-token', {
      token,
      platform: 'WEB',
      deviceName: `${navigator.platform} - ${getBrowserName()}`,
    });

    console.info('[FCM] Device token registered successfully!');

    // Listen for foreground messages and show a toast
    onMessage(messaging, (payload) => {
      console.info('[FCM] Foreground message received:', payload);
      const { title, body } = payload.notification ?? {};
      if (title) {
        showForegroundToast(title, body ?? '');
      }
    });

    return true;
  } catch (err) {
    console.error('[FCM] Initialization error:', err);
    return false;
  }
}

function getBrowserName(): string {
  const ua = navigator.userAgent;
  if (ua.includes('Chrome')) return 'Chrome';
  if (ua.includes('Firefox')) return 'Firefox';
  if (ua.includes('Safari')) return 'Safari';
  if (ua.includes('Edge')) return 'Edge';
  return 'Browser';
}

function showForegroundToast(title: string, body: string) {
  // Dispatch a custom event that our NotificationToast component listens to
  const event = new CustomEvent('fcm-foreground-message', {
    detail: { title, body },
  });
  window.dispatchEvent(event);
}
