'use client';

import { useMemo, useEffect, useState } from 'react';
import {
  isToday, isTomorrow, isThisWeek, isPast, differenceInSeconds,
} from 'date-fns';
import { ExtractedTask } from '@/types';
import { Clock } from 'lucide-react';

/** Returns an urgency score 0–1000 (higher = more urgent) */
export function getUrgencyScore(task: ExtractedTask): number {
  if (task.status === 'COMPLETED' || task.status === 'CANCELLED') return -1;

  const dateRaw = task.eventDate || task.deadline || task.dueDate;
  if (!dateRaw) return 10; // no date = low priority

  const date = new Date(dateRaw);
  if (isNaN(date.getTime())) return 10;

  const now = Date.now();
  const msUntil = date.getTime() - now;
  const hoursUntil = msUntil / 3_600_000;

  let score = 0;

  // Overdue gets the highest possible urgency
  if (hoursUntil < 0) score += 1000;
  // Due within 24h
  else if (hoursUntil <= 24) score += 500 + (24 - hoursUntil) * 10;
  // Due within 48h
  else if (hoursUntil <= 48) score += 200;
  // Due this week
  else if (hoursUntil <= 168) score += 100;

  // Priority multiplier
  const priorityBonus = { HIGH: 150, MEDIUM: 50, LOW: 0 };
  score += priorityBonus[task.priority || 'MEDIUM'] || 0;

  return score;
}

export type TimeGroup = 'OVERDUE' | 'TODAY' | 'TOMORROW' | 'THIS_WEEK' | 'LATER' | 'NO_DATE';

export function getTimeGroup(task: ExtractedTask): TimeGroup {
  const dateRaw = task.eventDate || task.deadline || task.dueDate;
  if (!dateRaw) return 'NO_DATE';
  const date = new Date(dateRaw);
  if (isNaN(date.getTime())) return 'NO_DATE';

  if (isPast(date) && task.status !== 'COMPLETED') return 'OVERDUE';
  if (isToday(date)) return 'TODAY';
  if (isTomorrow(date)) return 'TOMORROW';
  if (isThisWeek(date, { weekStartsOn: 1 })) return 'THIS_WEEK';
  return 'LATER';
}

export const GROUP_META: Record<TimeGroup, { label: string; color: string; dot: string }> = {
  OVERDUE:   { label: '🔴 Overdue',    color: 'text-rose-400',    dot: 'bg-rose-500' },
  TODAY:     { label: '⚡ Today',       color: 'text-amber-400',   dot: 'bg-amber-500' },
  TOMORROW:  { label: '🌅 Tomorrow',   color: 'text-sky-400',     dot: 'bg-sky-500' },
  THIS_WEEK: { label: '📅 This Week',  color: 'text-violet-400',  dot: 'bg-violet-500' },
  LATER:     { label: '🗓 Later',       color: 'text-slate-400',   dot: 'bg-slate-500' },
  NO_DATE:   { label: '📌 No Date',    color: 'text-white/40',    dot: 'bg-white/20' },
};

export const GROUP_ORDER: TimeGroup[] = ['OVERDUE', 'TODAY', 'TOMORROW', 'THIS_WEEK', 'LATER', 'NO_DATE'];

/** Hook: live countdown for a date */
export function useCountdown(dateRaw: string | null | undefined) {
  const date = useMemo(() => {
    if (!dateRaw) return null;
    const d = new Date(dateRaw);
    return isNaN(d.getTime()) ? null : d;
  }, [dateRaw]);

  const [seconds, setSeconds] = useState(() =>
    date ? Math.max(0, differenceInSeconds(date, new Date())) : 0
  );

  useEffect(() => {
    if (!date || !isToday(date)) return;
    const id = setInterval(() => {
      setSeconds(Math.max(0, differenceInSeconds(date, new Date())));
    }, 1000);
    return () => clearInterval(id);
  }, [date]);

  if (!date || !isToday(date)) return null;

  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;

  return { h, m, s, label: `${h}h ${m}m ${s}s` };
}

/** Tiny live countdown badge — shows only for TODAY tasks */
export function CountdownBadge({ dateRaw }: { dateRaw: string | null | undefined }) {
  const countdown = useCountdown(dateRaw);
  if (!countdown) return null;

  return (
    <span className="flex items-center gap-1 text-[11px] font-mono font-bold text-amber-400 bg-amber-500/10 border border-amber-500/30 rounded-full px-2 py-0.5 animate-pulse">
      <Clock className="w-3 h-3" />
      {countdown.label}
    </span>
  );
}
