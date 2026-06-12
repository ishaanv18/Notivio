'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { ExtractedTask, PaginatedResponse } from '@/types';
import { TaskCard } from '@/components/TaskCard';
import { Skeleton } from '@/components/ui/skeleton';
import { NotificationSetup } from '@/components/NotificationSetup';
import { getUrgencyScore, CountdownBadge } from '@/lib/taskIntelligence';
import {
  RefreshCw, Zap, CheckSquare, BellRing, Check, AlertTriangle,
  CalendarClock, TrendingUp, Sparkles, ArrowRight, Clock
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatDistanceToNow, format, isToday, isTomorrow } from 'date-fns';
import { motion, AnimatePresence, type Variants } from 'framer-motion';
import Link from 'next/link';
import { useMemo, useState } from 'react';
import toast from 'react-hot-toast';

interface BackendNotification {
  id: string; title: string; body: string; status: string; createdAt: string; type: string;
}
interface NotifPage { content: BackendNotification[]; totalElements: number; }

export default function Dashboard() {
  const queryClient = useQueryClient();
  const [isRefreshing, setIsRefreshing] = useState(false);

  const { data: taskData, isLoading: tasksLoading, refetch: refetchTasks } = useQuery({
    queryKey: ['tasks'],
    queryFn: async () => { const res = await api.get('/tasks?size=100'); return res.data as PaginatedResponse<ExtractedTask>; },
  });

  const { data: notifData } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => { const res = await api.get('/notifications?size=10'); return res.data as NotifPage; },
    refetchInterval: 30_000,
  });

  const { data: unreadData } = useQuery({
    queryKey: ['notifications-unread'],
    queryFn: async () => { const res = await api.get('/notifications/unread-count'); return res.data as { unreadCount: number }; },
    refetchInterval: 30_000,
  });

  const markAllRead = useMutation({
    mutationFn: () => api.post('/notifications/mark-read'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
      toast.success('All notifications marked as read', {
        icon: '✓',
        style: {
          background: 'rgba(16, 185, 129, 0.1)',
          color: '#fff',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          backdropFilter: 'blur(20px)',
        },
      });
    },
  });

  const toggleComplete = useMutation({
    mutationFn: ({ id, status }: { id: string; status: boolean }) =>
      api.patch(`/tasks/${id}/complete`, { completed: !status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      toast.success('Task updated', {
        icon: '✓',
        style: {
          background: 'rgba(59, 130, 246, 0.1)',
          color: '#fff',
          border: '1px solid rgba(59, 130, 246, 0.3)',
          backdropFilter: 'blur(20px)',
        },
      });
    },
  });

  const syncGmail = async () => {
    setIsRefreshing(true);
    try {
      await api.post('/gmail/sync?force=true');
      await refetchTasks();
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Gmail synced successfully!', {
        icon: '🔄',
        style: {
          background: 'rgba(139, 92, 246, 0.1)',
          color: '#fff',
          border: '1px solid rgba(139, 92, 246, 0.3)',
          backdropFilter: 'blur(20px)',
        },
      });
    } catch {
      toast.error('Failed to sync Gmail', {
        icon: '⚠️',
        style: {
          background: 'rgba(239, 68, 68, 0.1)',
          color: '#fff',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          backdropFilter: 'blur(20px)',
        },
      });
    } finally {
      setIsRefreshing(false);
    }
  };

  const tasks = useMemo(() => taskData?.content || [], [taskData]);
  
  // Smart computed stats
  const activeTasks    = useMemo(() => tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'CANCELLED'), [tasks]);
  const overdueTasks   = useMemo(() => activeTasks.filter(t => {
    const d = t.eventDate || t.deadline || t.dueDate;
    return d && new Date(d) < new Date();
  }), [activeTasks]);
  const todayTasks     = useMemo(() => activeTasks.filter(t => {
    const d = t.eventDate || t.deadline || t.dueDate;
    return d && isToday(new Date(d));
  }), [activeTasks]);
  const tomorrowTasks  = useMemo(() => activeTasks.filter(t => {
    const d = t.eventDate || t.deadline || t.dueDate;
    return d && isTomorrow(new Date(d));
  }), [activeTasks]);
  const completedCount = useMemo(() => tasks.filter(t => t.status === 'COMPLETED').length, [tasks]);
  const completionRate = tasks.length > 0 ? Math.round((completedCount / tasks.length) * 100) : 0;

  // Top 5 most urgent tasks for dashboard spotlight
  const urgentTasks = useMemo(() =>
    [...activeTasks]
      .sort((a, b) => getUrgencyScore(b) - getUrgencyScore(a))
      .slice(0, 5),
    [activeTasks]
  );

  const notifications = notifData?.content || [];
  const unreadCount   = unreadData?.unreadCount || 0;

  const containerVariants: Variants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1
      }
    }
  };

  const itemVariants: Variants = {
    hidden: { opacity: 0, y: 20 },
    visible: { 
      opacity: 1, 
      y: 0,
      transition: {
        type: 'spring',
        stiffness: 300,
        damping: 24
      }
    }
  };

  return (
    <>
      <motion.div
        className="space-y-6"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
      >

        {/* Floating Action Button for mobile */}
        <motion.button
          onClick={syncGmail}
          disabled={isRefreshing}
          className="md:hidden fixed bottom-20 right-6 z-40 w-16 h-16 rounded-full bg-gradient-to-br from-neon-blue to-neon-purple shadow-2xl shadow-neon-blue/50 flex items-center justify-center text-white font-bold"
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          animate={isRefreshing ? { rotate: 360 } : { rotate: 0 }}
          transition={isRefreshing ? { duration: 1, repeat: Infinity, ease: 'linear' } : {}}
        >
          <RefreshCw className="w-7 h-7" />
        </motion.button>

        {/* Header */}
        <motion.div 
          className="flex flex-col sm:flex-row sm:items-center justify-between gap-4"
          variants={itemVariants}
        >
          <div>
            <motion.h1 
              className="text-4xl font-bold font-heading bg-gradient-to-r from-white via-white to-white/60 bg-clip-text text-transparent"
              animate={{ backgroundPosition: ['0%', '100%', '0%'] }}
              transition={{ duration: 8, repeat: Infinity, ease: 'linear' }}
            >
              Overview
            </motion.h1>
            <p className="text-muted-foreground mt-2 flex items-center gap-2 flex-wrap">
              <span className="flex items-center gap-1.5 bg-white/5 px-3 py-1 rounded-full text-sm font-medium">
                <Zap className="w-3.5 h-3.5 text-violet-400" />
                {activeTasks.length} active
              </span>
              {overdueTasks.length > 0 && (
                <motion.span 
                  className="flex items-center gap-1.5 bg-rose-500/20 px-3 py-1 rounded-full text-sm font-semibold text-rose-400 border border-rose-500/30"
                  animate={{ scale: [1, 1.05, 1] }}
                  transition={{ duration: 2, repeat: Infinity }}
                >
                  <AlertTriangle className="w-3.5 h-3.5" />
                  {overdueTasks.length} overdue
                </motion.span>
              )}
              {todayTasks.length > 0 && (
                <span className="flex items-center gap-1.5 bg-amber-500/20 px-3 py-1 rounded-full text-sm font-semibold text-amber-400 border border-amber-500/30">
                  <CalendarClock className="w-3.5 h-3.5" />
                  {todayTasks.length} today
                </span>
              )}
            </p>
          </div>
          <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
            <Button
              onClick={syncGmail}
              disabled={isRefreshing}
              className="hidden md:flex bg-gradient-to-r from-neon-blue to-neon-purple hover:from-neon-blue/80 hover:to-neon-purple/80 text-white border-0 shadow-lg shadow-neon-blue/30 btn-haptic"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
              Sync Gmail
            </Button>
          </motion.div>
        </motion.div>

        {/* Push notification setup banner */}
        <motion.div variants={itemVariants}>
          <NotificationSetup />
        </motion.div>

        {/* Enhanced Stat cards */}
        <motion.div 
          className="grid grid-cols-2 lg:grid-cols-4 gap-3"
          variants={containerVariants}
        >
          <StatCard 
            icon={<Zap className="w-5 h-5" />} 
            gradient="from-violet-500 to-purple-500"
            label="Active" 
            value={activeTasks.length} 
            loading={tasksLoading} 
          />
          <StatCard 
            icon={<AlertTriangle className="w-5 h-5" />} 
            gradient="from-rose-500 to-red-500"
            label="Overdue" 
            value={overdueTasks.length} 
            loading={tasksLoading} 
            highlight={overdueTasks.length > 0} 
            pulse={overdueTasks.length > 0}
          />
          <StatCard 
            icon={<CalendarClock className="w-5 h-5" />} 
            gradient="from-amber-500 to-orange-500"
            label="Due Today" 
            value={todayTasks.length} 
            loading={tasksLoading} 
          />
          <StatCard 
            icon={<CheckSquare className="w-5 h-5" />} 
            gradient="from-emerald-500 to-green-500"
            label="Completed" 
            value={completedCount} 
            loading={tasksLoading} 
            sub={`${completionRate}%`} 
          />
        </motion.div>

        {/* Today spotlight with enhanced animation */}
        <AnimatePresence>
          {todayTasks.length > 0 && (
            <motion.div
              variants={itemVariants}
              initial="hidden"
              animate="visible"
              exit={{ opacity: 0, height: 0 }}
              className="ios-card border-amber-500/30 bg-gradient-to-br from-amber-950/30 to-orange-950/20 p-6 shadow-2xl shadow-amber-500/10 relative overflow-hidden"
            >
              {/* Animated background */}
              <div className="absolute inset-0 bg-gradient-to-br from-amber-500/10 via-transparent to-orange-500/10 animate-gradient pointer-events-none" />
              
              <div className="relative z-10">
                <div className="flex items-center justify-between mb-5">
                  <h2 className="text-lg font-bold text-amber-400 flex items-center gap-2.5">
                    <motion.div
                      animate={{ rotate: [0, 10, -10, 0], scale: [1, 1.1, 1] }}
                      transition={{ duration: 2, repeat: Infinity }}
                    >
                      <Sparkles className="w-5 h-5" />
                    </motion.div>
                    Today&apos;s Focus
                    <span className="text-sm font-normal text-white/40">· {format(new Date(), 'EEEE, dd MMM')}</span>
                  </h2>
                  <motion.span 
                    className="text-xs font-bold bg-amber-500/20 text-amber-400 border border-amber-500/30 rounded-full px-3 py-1.5"
                    whileHover={{ scale: 1.05 }}
                  >
                    {todayTasks.length} task{todayTasks.length !== 1 ? 's' : ''}
                  </motion.span>
                </div>
                <div className="space-y-3">
                  {todayTasks.map((task, index) => {
                    const d = task.eventDate || task.deadline || task.dueDate;
                    return (
                      <motion.div 
                        key={task.id} 
                        className="flex items-center justify-between ios-card bg-gradient-to-r from-white/[0.08] to-white/[0.04] px-5 py-4 border border-white/10 hover:border-amber-500/30 transition-all"
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: index * 0.1 }}
                        whileHover={{ x: 4, scale: 1.01 }}
                      >
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-semibold text-white truncate">{task.title}</p>
                          {task.organizer && <p className="text-xs text-white/50 mt-0.5">{task.organizer}</p>}
                        </div>
                        <CountdownBadge dateRaw={d} />
                      </motion.div>
                    );
                  })}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Tomorrow preview with enhanced styling */}
        <AnimatePresence>
          {tomorrowTasks.length > 0 && (
            <motion.div 
              variants={itemVariants}
              initial="hidden"
              animate="visible"
              exit={{ opacity: 0, height: 0 }}
              className="ios-card border-sky-500/20 p-5 bg-gradient-to-br from-sky-950/20 to-blue-950/10 relative overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-sky-500/5 via-transparent to-blue-500/5 pointer-events-none" />
              <div className="relative z-10">
                <h3 className="text-sm font-bold text-sky-400 flex items-center gap-2 mb-4">
                  <CalendarClock className="w-4.5 h-4.5" /> 
                  Tomorrow 
                  <span className="text-white/40 font-normal">· {tomorrowTasks.length} task{tomorrowTasks.length !== 1 ? 's' : ''}</span>
                </h3>
                <div className="flex flex-wrap gap-2.5">
                  {tomorrowTasks.map((task, index) => (
                    <motion.span 
                      key={task.id} 
                      className="text-xs bg-sky-500/15 text-sky-300 border border-sky-500/25 rounded-full px-4 py-2 font-medium truncate max-w-[200px] hover:bg-sky-500/20 transition-all"
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: index * 0.05 }}
                      whileHover={{ scale: 1.05, y: -2 }}
                    >
                      {task.title}
                    </motion.span>
                  ))}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Main content grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

          {/* Urgent tasks with enhanced styling */}
          <motion.div className="lg:col-span-2 space-y-4" variants={itemVariants}>
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold font-heading flex items-center gap-2.5">
                <motion.div
                  animate={{ y: [0, -3, 0] }}
                  transition={{ duration: 2, repeat: Infinity }}
                >
                  <TrendingUp className="w-5 h-5 text-violet-400" />
                </motion.div>
                Most Urgent
              </h2>
              <Link href="/dashboard/tasks">
                <motion.div 
                  className="text-xs text-white/40 hover:text-violet-400 flex items-center gap-1.5 transition-colors px-3 py-1.5 rounded-full hover:bg-white/5"
                  whileHover={{ scale: 1.05, x: 2 }}
                >
                  View all {activeTasks.length} tasks <ArrowRight className="w-3.5 h-3.5" />
                </motion.div>
              </Link>
            </div>

            {tasksLoading ? (
              Array(3).fill(0).map((_, i) => (
                <Skeleton key={i} className="h-32 w-full rounded-3xl bg-gradient-to-br from-white/[0.08] to-white/[0.03] animate-shimmer" />
              ))
            ) : urgentTasks.length > 0 ? (
              <motion.div 
                className="space-y-4"
                variants={containerVariants}
              >
                {urgentTasks.map((task, index) => (
                  <motion.div
                    key={task.id}
                    variants={itemVariants}
                    custom={index}
                  >
                    <TaskCard
                      task={task}
                      onToggleComplete={(id, status) => toggleComplete.mutate({ id, status })}
                    />
                  </motion.div>
                ))}
              </motion.div>
            ) : (
              <motion.div 
                className="ios-card p-16 text-center flex flex-col items-center border-emerald-500/20"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
              >
                <motion.div
                  animate={{ rotate: [0, 10, -10, 0] }}
                  transition={{ duration: 3, repeat: Infinity }}
                >
                  <CheckSquare className="w-16 h-16 text-emerald-400/40 mb-4" />
                </motion.div>
                <h3 className="text-lg font-semibold text-white mb-2">All caught up! 🎉</h3>
                <p className="text-muted-foreground text-sm">No active tasks detected in your Gmail.</p>
              </motion.div>
            )}
          </motion.div>

          {/* Notification Panel with iOS styling */}
          <motion.div className="space-y-4" variants={itemVariants}>
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold font-heading flex items-center gap-2.5">
                <motion.div
                  animate={{ rotate: [0, 15, -15, 0] }}
                  transition={{ duration: 2, repeat: Infinity }}
                >
                  <BellRing className="w-5 h-5 text-sky-400" />
                </motion.div>
                Alerts
                {unreadCount > 0 && (
                  <motion.span 
                    className="text-xs bg-gradient-to-r from-sky-500 to-blue-500 text-white rounded-full px-2.5 py-1 font-bold shadow-lg shadow-sky-500/50"
                    animate={{ scale: [1, 1.1, 1] }}
                    transition={{ duration: 2, repeat: Infinity }}
                  >
                    {unreadCount}
                  </motion.span>
                )}
              </h2>
              {unreadCount > 0 && (
                <motion.button
                  onClick={() => markAllRead.mutate()}
                  className="text-xs text-white/40 hover:text-sky-400 flex items-center gap-1.5 transition-colors px-3 py-1.5 rounded-full hover:bg-white/5"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <Check className="w-3.5 h-3.5" /> Mark all read
                </motion.button>
              )}
            </div>

            <div className="ios-card overflow-hidden divide-y divide-white/5">
              {notifications.length === 0 ? (
                <motion.div 
                  className="p-12 text-center text-white/40 text-sm"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                >
                  <BellRing className="w-10 h-10 mx-auto mb-3 opacity-30" />
                  <p className="font-medium">No notifications yet</p>
                </motion.div>
              ) : (
                notifications.map((notif, index) => (
                  <motion.div
                    key={notif.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05 }}
                    className={`p-4 transition-all hover:bg-white/[0.04] ${notif.status === 'SENT' ? 'bg-sky-500/5' : ''}`}
                    whileHover={{ x: 4 }}
                  >
                    <div className="flex items-start gap-3">
                      <motion.div 
                        className={`w-2.5 h-2.5 rounded-full mt-1.5 flex-shrink-0 ${
                          notif.status === 'SENT' 
                            ? 'bg-sky-400 shadow-[0_0_8px_rgba(14,165,233,0.8)]' 
                            : 'bg-white/20'
                        }`}
                        animate={notif.status === 'SENT' ? { scale: [1, 1.2, 1] } : {}}
                        transition={{ duration: 2, repeat: Infinity }}
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-white truncate">{notif.title}</p>
                        <p className="text-xs text-white/60 mt-1 line-clamp-2">{notif.body}</p>
                        <span className="text-[10px] text-sky-400/70 mt-1.5 block flex items-center gap-1">
                          <Clock className="w-3 h-3" />
                          {formatDistanceToNow(new Date(notif.createdAt), { addSuffix: true })}
                        </span>
                      </div>
                    </div>
                  </motion.div>
                ))
              )}
            </div>

            <Link href="/dashboard/notifications" className="block text-center">
              <motion.div 
                className="text-xs text-white/30 hover:text-sky-400 flex items-center gap-1.5 transition-colors justify-center py-2"
                whileHover={{ scale: 1.05 }}
              >
                View all notifications <ArrowRight className="w-3.5 h-3.5" />
              </motion.div>
            </Link>
          </motion.div>
        </div>
      </motion.div>
    </>
  );
}

function StatCard({
  icon, gradient, label, value, loading, highlight, sub, pulse,
}: {
  icon: React.ReactNode; gradient: string; label: string; value: number;
  loading: boolean; highlight?: boolean; sub?: string; pulse?: boolean;
}) {
  return (
    <motion.div
      variants={{
        hidden: { opacity: 0, y: 20 },
        visible: { opacity: 1, y: 0 }
      }}
      whileHover={{ y: -4, scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      className={`
        ios-card p-5 flex flex-col gap-3 transition-all relative overflow-hidden group
        ${highlight ? 'border-rose-500/40 shadow-2xl shadow-rose-500/20 ring-2 ring-rose-500/20' : 'border-white/10'}
        ${pulse ? 'animate-badge-pulse' : ''}
      `}
    >
      {/* Animated gradient background */}
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-500`} />
      
      {/* Glowing icon */}
      <motion.div 
        className={`relative w-12 h-12 rounded-2xl bg-gradient-to-br ${gradient} opacity-15 flex items-center justify-center border border-white/10 group-hover:scale-110 transition-all`}
        whileHover={{ rotate: [0, -10, 10, 0] }}
        transition={{ duration: 0.5 }}
      >
        <div className={`absolute inset-0 rounded-2xl bg-gradient-to-br ${gradient} blur-xl opacity-50`} />
        <div className={`relative text-white`}>
          {icon}
        </div>
      </motion.div>
      
      <div className="relative z-10">
        <p className="text-xs font-semibold text-white/50 uppercase tracking-wider">{label}</p>
        {loading ? (
          <div className="h-8 w-16 bg-white/10 rounded-lg animate-shimmer mt-2" />
        ) : (
          <div className="flex items-baseline gap-2 mt-1">
            <motion.h2 
              className="text-3xl font-bold text-white"
              key={value}
              initial={{ scale: 1.2, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ type: 'spring', stiffness: 300, damping: 20 }}
            >
              {value}
            </motion.h2>
            {sub && (
              <span className={`text-sm font-bold bg-gradient-to-r ${gradient} bg-clip-text text-transparent`}>
                {sub}
              </span>
            )}
          </div>
        )}
      </div>
    </motion.div>
  );
}
