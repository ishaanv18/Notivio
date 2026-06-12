'use client';

import { ReactNode, useRef, useState, useEffect } from 'react';
import { motion, useMotionValue, useTransform, PanInfo } from 'framer-motion';
import { RefreshCw } from 'lucide-react';

interface PullToRefreshProps {
  onRefresh: () => Promise<void>;
  children: ReactNode;
}

export function PullToRefresh({ onRefresh, children }: PullToRefreshProps) {
  const [isRefreshing, setIsRefreshing] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const y = useMotionValue(0);
  
  const refreshThreshold = 100;
  const opacity = useTransform(y, [0, refreshThreshold], [0, 1]);
  const rotate = useTransform(y, [0, refreshThreshold], [0, 360]);
  const scale = useTransform(y, [0, refreshThreshold / 2, refreshThreshold], [0.5, 0.8, 1]);

  const handleDragEnd = async (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    if (info.offset.y > refreshThreshold && !isRefreshing) {
      setIsRefreshing(true);
      try {
        await onRefresh();
      } finally {
        setIsRefreshing(false);
        y.set(0);
      }
    } else {
      y.set(0);
    }
  };

  useEffect(() => {
    if (isRefreshing) {
      y.set(refreshThreshold);
    }
  }, [isRefreshing, y, refreshThreshold]);

  return (
    <div ref={containerRef} className="relative h-full overflow-hidden">
      {/* Pull indicator */}
      <motion.div
        style={{ opacity }}
        className="absolute top-0 left-0 right-0 flex justify-center items-center h-24 z-50 pointer-events-none"
      >
        <motion.div
          style={{ rotate, scale }}
          className="w-12 h-12 rounded-full bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center shadow-lg"
        >
          <RefreshCw className={`w-6 h-6 text-white ${isRefreshing ? 'animate-spin' : ''}`} />
        </motion.div>
      </motion.div>

      {/* Main content */}
      <motion.div
        drag="y"
        dragConstraints={{ top: 0, bottom: 0 }}
        dragElastic={{ top: 0.3, bottom: 0 }}
        onDragEnd={handleDragEnd}
        style={{ y }}
        className="h-full overflow-y-auto"
        animate={isRefreshing ? { y: refreshThreshold } : { y: 0 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      >
        {children}
      </motion.div>
    </div>
  );
}
