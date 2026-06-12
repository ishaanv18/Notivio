'use client';

import { useEffect, useState } from 'react';
import { initializeFCM } from '@/lib/fcm';
import { useAuthStore } from '@/store/useAuthStore';
import { Bell, BellOff, BellRing } from 'lucide-react';
import { motion } from 'framer-motion';

type PermissionState = 'idle' | 'loading' | 'granted' | 'denied' | 'unsupported';

export function NotificationSetup() {
  const { isAuthenticated } = useAuthStore();
  const [state, setState] = useState<PermissionState>('idle');
  const [isDismissed, setIsDismissed] = useState(false);

  // Check initial permission state
  useEffect(() => {
    if (!isAuthenticated) return;
    if (typeof Notification === 'undefined') {
      setState('unsupported');
      return;
    }
    if (Notification.permission === 'granted') {
      setState('granted');
      // Silently re-register token to keep it fresh
      initializeFCM().catch(console.warn);
    } else if (Notification.permission === 'denied') {
      setState('denied');
    }
    // else 'default' → show the prompt banner
  }, [isAuthenticated]);

  const handleEnable = async () => {
    setState('loading');
    const success = await initializeFCM();
    setState(success ? 'granted' : 'denied');
  };

  // Don't show anything if already granted, unsupported, not logged in, or dismissed
  if (!isAuthenticated || state === 'unsupported' || isDismissed) return null;

  if (state === 'granted') {
    return (
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -10 }}
        className="flex items-center gap-2 text-sm text-neon-green font-medium"
      >
        <BellRing className="w-4 h-4 animate-pulse" />
        <span>Push notifications enabled</span>
      </motion.div>
    );
  }

  if (state === 'denied') {
    return (
      <div className="flex items-center gap-2 text-sm text-white/50">
        <BellOff className="w-4 h-4" />
        <span>Notifications blocked. Enable in browser settings.</span>
      </div>
    );
  }

  // Default / idle → show the enable banner
  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      className="glass-panel border border-neon-blue/30 p-4 flex items-center justify-between gap-4 shadow-[0_0_20px_rgba(59,130,246,0.1)]"
    >
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-neon-blue/20 border border-neon-blue/30 flex items-center justify-center flex-shrink-0">
          <Bell className="w-5 h-5 text-neon-blue" />
        </div>
        <div>
          <p className="text-sm font-semibold text-white">Enable Push Notifications</p>
          <p className="text-xs text-white/60 mt-0.5">
            Get alerted on your browser & iPhone when deadlines approach.
          </p>
        </div>
      </div>
      <div className="flex items-center gap-2 flex-shrink-0">
        <button
          onClick={handleEnable}
          disabled={state === 'loading'}
          className="px-4 py-2 text-sm rounded-full bg-neon-blue text-white font-semibold hover:bg-neon-blue/80 transition-all disabled:opacity-70 shadow-[0_0_15px_rgba(59,130,246,0.4)]"
        >
          {state === 'loading' ? 'Enabling...' : 'Enable'}
        </button>
        <button
          onClick={() => setIsDismissed(true)}
          className="px-3 py-2 text-sm rounded-full text-white/50 hover:text-white/80 transition-colors"
        >
          Later
        </button>
      </div>
    </motion.div>
  );
}
