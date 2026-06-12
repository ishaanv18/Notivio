'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Bell, X } from 'lucide-react';

interface ToastMessage {
  id: string;
  title: string;
  body: string;
}

export function NotificationToast() {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    const handler = (e: Event) => {
      const { title, body } = (e as CustomEvent).detail;
      const id = crypto.randomUUID();
      setToasts(prev => [...prev, { id, title, body }]);

      // Auto-dismiss after 6 seconds
      setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== id));
      }, 6000);
    };

    window.addEventListener('fcm-foreground-message', handler);
    return () => window.removeEventListener('fcm-foreground-message', handler);
  }, []);

  const dismiss = (id: string) => setToasts(prev => prev.filter(t => t.id !== id));

  return (
    <div className="fixed bottom-6 right-6 z-[9999] flex flex-col gap-3 max-w-sm w-full">
      <AnimatePresence mode="popLayout">
        {toasts.map(toast => (
          <motion.div
            key={toast.id}
            layout
            initial={{ opacity: 0, y: 40, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8, x: 40 }}
            transition={{ type: 'spring', stiffness: 300, damping: 30 }}
            className="glass-panel border border-neon-purple/40 p-4 shadow-[0_0_30px_rgba(139,92,246,0.3)] w-full"
          >
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded-full bg-neon-purple/20 flex items-center justify-center flex-shrink-0 border border-neon-purple/30">
                <Bell className="w-4 h-4 text-neon-purple" />
              </div>
              <div className="flex-1">
                <p className="font-semibold text-white text-sm">{toast.title}</p>
                <p className="text-white/60 text-xs mt-0.5 leading-relaxed">{toast.body}</p>
              </div>
              <button
                onClick={() => dismiss(toast.id)}
                className="text-white/40 hover:text-white transition-colors flex-shrink-0"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
