'use client';

import { useAuthStore } from '@/store/useAuthStore';
import { useRouter, usePathname } from 'next/navigation';
import { useEffect } from 'react';
import { LayoutDashboard, CheckSquare, Bell, Settings, LogOut, Sparkles } from 'lucide-react';
import Link from 'next/link';
import { cn } from '@/lib/utils';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { motion, AnimatePresence } from 'framer-motion';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user, logout } = useAuthStore();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/');
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated || !user) return null;

  const navItems = [
    { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    { name: 'Tasks & Deadlines', href: '/dashboard/tasks', icon: CheckSquare },
    { name: 'Notifications', href: '/dashboard/notifications', icon: Bell },
    { name: 'Settings', href: '/dashboard/settings', icon: Settings },
  ];

  return (
    <div className="flex h-screen overflow-hidden bg-gradient-to-br from-[#05050a] via-[#0a0a14] to-[#0f0f1e]">
      {/* Desktop Sidebar with enhanced styling */}
      <aside className="hidden md:flex flex-col w-72 ios-card rounded-none border-r border-white/10 h-full z-20 relative overflow-hidden">
        {/* Animated background gradient */}
        <div className="absolute inset-0 bg-gradient-to-br from-violet-500/5 via-transparent to-blue-500/5 pointer-events-none" />
        
        <div className="p-6 relative z-10">
          <motion.div 
            className="flex items-center gap-3"
            whileHover={{ scale: 1.02 }}
          >
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center shadow-lg shadow-neon-blue/30">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <h2 className="text-2xl font-bold font-heading bg-gradient-to-r from-neon-blue via-neon-purple to-neon-green bg-clip-text text-transparent animate-gradient">
              Notivio
            </h2>
          </motion.div>
        </div>
        
        <div className="px-4 py-2 flex-1 relative z-10">
          <nav className="space-y-2">
            {navItems.map((item, index) => {
              const Icon = item.icon;
              const isActive = pathname === item.href;
              return (
                <Link key={item.name} href={item.href}>
                  <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.1 }}
                    whileHover={{ scale: 1.02, x: 4 }}
                    whileTap={{ scale: 0.98 }}
                    className={cn(
                      "flex items-center gap-3 px-4 py-3.5 rounded-2xl transition-all duration-300 relative overflow-hidden group",
                      isActive 
                        ? "bg-gradient-to-r from-white/15 to-white/10 text-white shadow-lg border border-white/20" 
                        : "text-muted-foreground hover:bg-white/5 hover:text-white"
                    )}
                  >
                    {isActive && (
                      <motion.div
                        layoutId="activeTab"
                        className="absolute inset-0 bg-gradient-to-r from-neon-blue/20 to-neon-purple/20 rounded-2xl"
                        transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                      />
                    )}
                    <Icon className={cn("w-5 h-5 relative z-10", isActive && "drop-shadow-[0_0_8px_rgba(59,130,246,0.8)]")} />
                    <span className="font-semibold relative z-10">{item.name}</span>
                  </motion.div>
                </Link>
              );
            })}
          </nav>
        </div>
        
        <div className="p-4 border-t border-white/10 mt-auto relative z-10">
          <motion.div 
            className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-gradient-to-r from-white/10 to-white/5 border border-white/10"
            whileHover={{ scale: 1.02 }}
          >
            <Avatar className="w-10 h-10 border-2 border-white/20 ring-2 ring-neon-blue/30">
              <AvatarImage src={user.profilePicture} />
              <AvatarFallback className="bg-gradient-to-br from-neon-blue to-neon-purple text-white font-bold">
                {user.name.substring(0,2)}
              </AvatarFallback>
            </Avatar>
            <div className="flex-1 overflow-hidden">
              <p className="text-sm font-semibold truncate text-white">{user.name}</p>
              <p className="text-xs text-muted-foreground truncate">{user.email}</p>
            </div>
            <motion.button 
              onClick={logout} 
              className="p-2 text-muted-foreground hover:text-rose-400 transition-colors rounded-lg hover:bg-rose-500/10"
              whileHover={{ scale: 1.1, rotate: 10 }}
              whileTap={{ scale: 0.9 }}
            >
              <LogOut className="w-4 h-4" />
            </motion.button>
          </motion.div>
        </div>
      </aside>

      {/* Main Content Area with enhanced styling */}
      <main className="flex-1 overflow-y-auto pb-24 md:pb-0 relative">
        {/* Mobile Header with glassmorphism */}
        <motion.div 
          className="md:hidden flex items-center justify-between p-4 ios-blur sticky top-0 z-30 border-b border-white/10"
          initial={{ y: -100 }}
          animate={{ y: 0 }}
          transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-neon-blue to-neon-purple flex items-center justify-center shadow-lg shadow-neon-blue/30">
              <Sparkles className="w-4 h-4 text-white" />
            </div>
            <h2 className="text-xl font-bold font-heading bg-gradient-to-r from-neon-blue via-neon-purple to-neon-green bg-clip-text text-transparent">
              Notivio
            </h2>
          </div>
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Avatar className="w-9 h-9 border-2 border-white/20 ring-2 ring-neon-blue/20">
              <AvatarImage src={user.profilePicture} />
              <AvatarFallback className="bg-gradient-to-br from-neon-blue to-neon-purple text-white text-xs font-bold">
                {user.name.substring(0,2)}
              </AvatarFallback>
            </Avatar>
          </motion.div>
        </motion.div>
        
        <div className="p-4 md:p-8">
          <AnimatePresence mode="wait">
            <motion.div
              key={pathname}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.3 }}
            >
              {children}
            </motion.div>
          </AnimatePresence>
        </div>
      </main>

      {/* Enhanced Mobile Bottom Nav (iOS style) */}
      <motion.nav 
        className="md:hidden fixed bottom-0 left-0 right-0 ios-blur z-40 border-t border-white/10"
        initial={{ y: 100 }}
        animate={{ y: 0 }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
      >
        <div className="flex justify-around items-center px-2 py-2 pb-safe">
          {navItems.slice(0,4).map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link key={item.name} href={item.href} className="flex-1">
                <motion.div
                  whileTap={{ scale: 0.9 }}
                  className={cn(
                    "flex flex-col items-center justify-center py-2 rounded-2xl transition-all relative",
                    isActive ? "text-neon-blue" : "text-muted-foreground"
                  )}
                >
                  {isActive && (
                    <motion.div
                      layoutId="mobileActiveTab"
                      className="absolute inset-0 bg-gradient-to-r from-neon-blue/20 to-neon-purple/20 rounded-2xl"
                      transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                    />
                  )}
                  <motion.div
                    animate={isActive ? { y: [0, -3, 0] } : {}}
                    transition={{ duration: 0.6, repeat: Infinity, repeatDelay: 2 }}
                  >
                    <Icon className={cn(
                      "w-6 h-6 mb-1 relative z-10",
                      isActive && "drop-shadow-[0_0_10px_rgba(59,130,246,0.9)]"
                    )} />
                  </motion.div>
                  <span className={cn(
                    "text-[10px] font-bold relative z-10 tracking-wide",
                    isActive && "text-white"
                  )}>
                    {item.name.split(' ')[0]}
                  </span>
                  {isActive && (
                    <motion.div
                      className="absolute -bottom-1 w-1 h-1 rounded-full bg-neon-blue"
                      layoutId="mobileIndicator"
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  )}
                </motion.div>
              </Link>
            );
          })}
        </div>
      </motion.nav>
    </div>
  );
}
