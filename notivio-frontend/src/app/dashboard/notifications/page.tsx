'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import {
  BellRing, Check, Zap, Calendar, Trophy, Clock,
  AlertTriangle, Info, Sparkles, Target, TrendingUp, CheckCircle
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { Skeleton } from '@/components/ui/skeleton';
import { motion, AnimatePresence } from 'framer-motion';

interface BackendNotification {
  id: string;
  title: string;
  body: string;
  status: string;
  createdAt: string;
  type: string;
}

interface NotifPage {
  content: BackendNotification[];
  totalElements: number;
}

// Parse body: try JSON, fallback to plain text
function parseBody(body: string): { parsed: Record<string, unknown> | null; raw: string } {
  if (!body) return { parsed: null, raw: '' };
  try {
    const obj = JSON.parse(body);
    if (typeof obj === 'object') return { parsed: obj, raw: body };
  } catch { /* not JSON */ }
  return { parsed: null, raw: body };
}

// Icon per notification type
function NotifIcon({ type }: { type: string }) {
  const cls = 'w-5 h-5';
  switch (type) {
    case 'DIGEST':        return <Sparkles className={cls} />;
    case 'REMINDER':      return <Clock className={cls} />;
    case 'DEADLINE_ALERT':return <AlertTriangle className={cls} />;
    case 'TASK_CREATED':  return <Target className={cls} />;
    case 'QUOTA_ALERT':   return <TrendingUp className={cls} />;
    case 'SYSTEM':        return <Info className={cls} />;
    default:              return <BellRing className={cls} />;
  }
}

// Color per type
function typeStyle(type: string) {
  switch (type) {
    case 'DIGEST':         return { dot: 'bg-violet-400', glow: 'shadow-violet-400/50', icon: 'text-violet-400', bg: 'bg-violet-500/10' };
    case 'REMINDER':       return { dot: 'bg-sky-400',    glow: 'shadow-sky-400/50',    icon: 'text-sky-400',    bg: 'bg-sky-500/10'    };
    case 'DEADLINE_ALERT': return { dot: 'bg-rose-400',   glow: 'shadow-rose-400/50',   icon: 'text-rose-400',   bg: 'bg-rose-500/10'   };
    case 'TASK_CREATED':   return { dot: 'bg-emerald-400',glow: 'shadow-emerald-400/50',icon: 'text-emerald-400',bg: 'bg-emerald-500/10' };
    default:               return { dot: 'bg-blue-400',   glow: 'shadow-blue-400/50',   icon: 'text-blue-400',   bg: 'bg-blue-500/10'   };
  }
}

// Render the daily digest body beautifully
function DigestBody({ data }: { data: Record<string, unknown> }) {
  const digest = (data.digest as Record<string, unknown>) || data;
  const urgent   = (digest.urgent_tasks   as string[]) || [];
  const upcoming = (digest.upcoming_deadlines as string[]) || [];
  const motivation = (digest.motivation as string) || '';

  return (
    <div className="mt-3 space-y-3">
      {urgent.length > 0 && (
        <div className="rounded-xl bg-rose-500/10 border border-rose-500/20 p-3">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-3.5 h-3.5 text-rose-400" />
            <span className="text-xs font-semibold text-rose-400 uppercase tracking-wider">Urgent Today</span>
          </div>
          <ul className="space-y-1">
            {urgent.map((t, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-white/80">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-rose-400 flex-shrink-0" />
                {t}
              </li>
            ))}
          </ul>
        </div>
      )}

      {upcoming.length > 0 && (
        <div className="rounded-xl bg-amber-500/10 border border-amber-500/20 p-3">
          <div className="flex items-center gap-2 mb-2">
            <Calendar className="w-3.5 h-3.5 text-amber-400" />
            <span className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Upcoming Deadlines</span>
          </div>
          <ul className="space-y-1">
            {upcoming.map((t, i) => (
              <li key={i} className="flex items-start gap-2 text-sm text-white/80">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-amber-400 flex-shrink-0" />
                {t}
              </li>
            ))}
          </ul>
        </div>
      )}

      {motivation && (
        <div className="rounded-xl bg-violet-500/10 border border-violet-500/20 p-3">
          <div className="flex items-center gap-2 mb-1">
            <Trophy className="w-3.5 h-3.5 text-violet-400" />
            <span className="text-xs font-semibold text-violet-400 uppercase tracking-wider">Motivation</span>
          </div>
          <p className="text-sm text-white/70 italic leading-relaxed">&quot;{motivation}&quot;</p>
        </div>
      )}
    </div>
  );
}

// Smart body renderer
function NotifBody({ notif }: { notif: BackendNotification }) {
  const { parsed, raw } = parseBody(notif.body);

  // Detect digest by body structure OR title — the backend sends type=SYSTEM for digests
  const isDigest =
    parsed &&
    (notif.type === 'DIGEST' ||
      'digest' in parsed ||
      notif.title?.toLowerCase().includes('digest'));

  if (isDigest && parsed) {
    return <DigestBody data={parsed} />;
  }

  // For any other parseable JSON, render a clean readable summary
  if (parsed) {
    return (
      <div className="mt-2 space-y-1.5">
        {Object.entries(parsed).map(([k, v]) => {
          let display: string;
          if (Array.isArray(v)) {
            display = (v as unknown[]).join(', ');
          } else if (v !== null && typeof v === 'object') {
            // Nested objects: show each sub-key
            return (
              <div key={k} className="mt-1">
                <span className="text-xs text-white/40 uppercase tracking-wider">{k.replace(/_/g, ' ')}</span>
                <div className="ml-3 mt-0.5 space-y-0.5">
                  {Object.entries(v as Record<string, unknown>).map(([sk, sv]) => (
                    <div key={sk} className="flex gap-2 text-sm">
                      <span className="text-white/40 capitalize min-w-[120px]">{sk.replace(/_/g, ' ')}:</span>
                      <span className="text-white/80">
                        {Array.isArray(sv) ? (sv as string[]).join(', ') : String(sv)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            );
          } else {
            display = String(v ?? '');
          }
          return (
            <div key={k} className="flex gap-2 text-sm">
              <span className="text-white/40 capitalize min-w-[100px]">{k.replace(/_/g, ' ')}:</span>
              <span className="text-white/80">{display}</span>
            </div>
          );
        })}
      </div>
    );
  }

  // Plain text
  return <p className="text-sm text-white/70 mt-1.5 leading-relaxed">{raw}</p>;
}

export default function NotificationsPage() {
  const queryClient = useQueryClient();

  const { data: notifData, isLoading } = useQuery<NotifPage>({
    queryKey: ['notifications-full'],
    queryFn: async () => {
      const res = await api.get('/notifications?size=50');
      return res.data;
    },
  });

  const markAllRead = useMutation({
    mutationFn: () => api.post('/notifications/mark-read'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-full'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread'] });
    },
  });

  const notifications = notifData?.content || [];
  const unreadCount = notifications.filter(n => n.status === 'SENT').length;

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold font-heading">Notifications</h1>
          <p className="text-muted-foreground mt-1">
            {unreadCount > 0 ? (
              <span><span className="text-blue-400 font-medium">{unreadCount} unread</span> · {notifications.length} total</span>
            ) : (
              'All your recent alerts and reminders.'
            )}
          </p>
        </div>

        {unreadCount > 0 && (
          <button
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending}
            className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl text-sm font-medium transition-all text-white/80 hover:text-white disabled:opacity-50"
          >
            <Check className="w-4 h-4" />
            Mark all read
          </button>
        )}
      </div>

      {/* List */}
      <div className="glass-panel overflow-hidden border border-white/10 shadow-[0_0_30px_rgba(0,0,0,0.5)]">
        {isLoading ? (
          <div className="p-6 space-y-4">
            {Array(5).fill(0).map((_, i) => (
              <Skeleton key={i} className="h-20 w-full rounded-xl bg-white/5" />
            ))}
          </div>
        ) : notifications.length === 0 ? (
          <motion.div className="flex flex-col items-center justify-center py-20 px-4 text-center">
            <CheckCircle className="w-16 h-16 text-emerald-500/50 mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">You&apos;re all caught up!</h3>
            <p className="text-white/50 max-w-sm">No new notifications to show. Go crush your tasks!</p>
          </motion.div>
        ) : (
          <AnimatePresence>
            <div className="divide-y divide-white/5">
              {notifications.map((notif, index) => {
                const style = typeStyle(notif.type);
                const isUnread = notif.status === 'SENT';

                return (
                  <motion.div
                    key={notif.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.04 }}
                    className={`p-5 transition-colors hover:bg-white/[0.03] ${isUnread ? style.bg : ''}`}
                  >
                    <div className="flex items-start gap-4">
                      {/* Unread dot */}
                      <div className="flex flex-col items-center gap-1 pt-1">
                        <div className={`w-2 h-2 rounded-full flex-shrink-0 ${isUnread ? `${style.dot} shadow-[0_0_6px_currentColor] ${style.glow}` : 'bg-white/15'}`} />
                      </div>

                      {/* Icon */}
                      <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${style.bg} ${style.icon}`}>
                        <NotifIcon type={notif.type} />
                      </div>

                      {/* Content */}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex items-center gap-2 flex-wrap">
                            <h4 className="text-base font-semibold text-white leading-tight">{notif.title}</h4>
                            <span className={`text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full ${style.bg} ${style.icon}`}>
                              {notif.type.replace(/_/g, ' ')}
                            </span>
                          </div>
                          <span className="text-xs text-white/35 whitespace-nowrap flex-shrink-0 mt-0.5">
                            {formatDistanceToNow(new Date(notif.createdAt), { addSuffix: true })}
                          </span>
                        </div>

                        {/* Smart body renderer */}
                        <NotifBody notif={notif} />
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </AnimatePresence>
        )}
      </div>
    </div>
  );
}
