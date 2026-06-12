'use client';

import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { ExtractedTask, PaginatedResponse } from '@/types';
import { TaskCard } from '@/components/TaskCard';
import { Skeleton } from '@/components/ui/skeleton';
import { motion, AnimatePresence } from 'framer-motion';
import {
  getUrgencyScore, getTimeGroup, GROUP_META, GROUP_ORDER, TimeGroup,
  CountdownBadge
} from '@/lib/taskIntelligence';
import {
  Search, Filter, CheckSquare, Zap, AlertTriangle,
  SlidersHorizontal, X
} from 'lucide-react';

const TASK_TYPES = ['ALL', 'INTERVIEW', 'EXAM', 'ASSIGNMENT', 'DEADLINE', 'MEETING', 'EVENT', 'INTERNSHIP', 'OTHER'];
const PRIORITIES = ['ALL', 'HIGH', 'MEDIUM', 'LOW'];

export default function TasksPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [showCompleted, setShowCompleted] = useState(false);
  const [groupBy, setGroupBy] = useState<'time' | 'type'>('time');

  const { data, isLoading } = useQuery<PaginatedResponse<ExtractedTask>>({
    queryKey: ['tasks', 'all'],
    queryFn: async () => {
      const res = await api.get('/tasks?size=100');
      return res.data;
    },
  });

  const toggleComplete = useMutation({
    mutationFn: ({ id, status }: { id: string; status: boolean }) =>
      api.patch(`/tasks/${id}/complete`, { completed: !status }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['tasks'] }),
  });

  const tasks = useMemo(() => data?.content || [], [data]);

  // Apply filters
  const filtered = useMemo(() => {
    return tasks
      .filter(t => {
        if (!showCompleted && (t.status === 'COMPLETED' || t.status === 'CANCELLED')) return false;
        if (typeFilter !== 'ALL' && t.taskType !== typeFilter) return false;
        if (priorityFilter !== 'ALL' && t.priority !== priorityFilter) return false;
        if (search) {
          const q = search.toLowerCase();
          return (
            t.title?.toLowerCase().includes(q) ||
            t.description?.toLowerCase().includes(q) ||
            t.organizer?.toLowerCase().includes(q) ||
            t.aiSummary?.toLowerCase().includes(q)
          );
        }
        return true;
      })
      .sort((a, b) => getUrgencyScore(b) - getUrgencyScore(a));
  }, [tasks, search, typeFilter, priorityFilter, showCompleted]);

  // Group by time or type
  const grouped = useMemo(() => {
    if (groupBy === 'type') {
      const map = new Map<string, ExtractedTask[]>();
      for (const t of filtered) {
        const key = t.taskType || 'OTHER';
        if (!map.has(key)) map.set(key, []);
        map.get(key)!.push(t);
      }
      return map;
    }
    // time grouping
    const map = new Map<string, ExtractedTask[]>();
    for (const g of GROUP_ORDER) map.set(g, []);
    for (const t of filtered) {
      const g = getTimeGroup(t);
      map.get(g)!.push(t);
    }
    // remove empty groups
    Array.from(map.entries()).forEach(([k, v]) => {
      if (v.length === 0) map.delete(k);
    });
    return map;
  }, [filtered, groupBy]);

  const activeCount  = tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'CANCELLED').length;
  const overdueCount = tasks.filter(t => t.status === 'OVERDUE' || (
    (t.deadline || t.eventDate) && new Date(t.deadline || t.eventDate || '') < new Date() && t.status !== 'COMPLETED'
  )).length;
  const completedCount = tasks.filter(t => t.status === 'COMPLETED').length;

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-5xl mx-auto">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold font-heading">Tasks & Deadlines</h1>
          <p className="text-muted-foreground mt-1">
            {activeCount} active · {overdueCount > 0 && <span className="text-rose-400 font-semibold">{overdueCount} overdue · </span>}
            {completedCount} done
          </p>
        </div>
      </div>

      {/* Stats bar */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Active', value: activeCount, color: 'text-violet-400', bg: 'bg-violet-500/10 border-violet-500/20', icon: <Zap className="w-4 h-4" /> },
          { label: 'Overdue', value: overdueCount, color: 'text-rose-400', bg: 'bg-rose-500/10 border-rose-500/20', icon: <AlertTriangle className="w-4 h-4" /> },
          { label: 'Completed', value: completedCount, color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20', icon: <CheckSquare className="w-4 h-4" /> },
        ].map(s => (
          <div key={s.label} className={`rounded-xl border p-3 flex items-center gap-3 ${s.bg}`}>
            <span className={s.color}>{s.icon}</span>
            <div>
              <p className="text-xs text-white/50">{s.label}</p>
              <p className={`text-xl font-bold ${s.color}`}>{s.value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Search + Filters */}
      <div className="glass-panel p-4 space-y-3 border border-white/10">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/40" />
          <input
            type="text"
            placeholder="Search tasks, companies, descriptions..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-white/5 border border-white/10 rounded-xl text-sm text-white placeholder-white/30 focus:outline-none focus:border-violet-500/50 focus:bg-white/8 transition-all"
          />
          {search && (
            <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
              <X className="w-4 h-4 text-white/40 hover:text-white" />
            </button>
          )}
        </div>

        {/* Filter pills */}
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs text-white/40 font-semibold flex items-center gap-1"><Filter className="w-3 h-3" /> Type:</span>
            {TASK_TYPES.map(t => (
              <button
                key={t}
                onClick={() => setTypeFilter(t)}
                className={`text-xs px-3 py-1 rounded-full font-semibold border transition-all ${
                  typeFilter === t
                    ? 'bg-violet-500/30 text-violet-300 border-violet-500/50'
                    : 'bg-white/5 text-white/50 border-white/10 hover:bg-white/10'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <div className="flex items-center justify-between flex-wrap gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-xs text-white/40 font-semibold flex items-center gap-1"><SlidersHorizontal className="w-3 h-3" /> Priority:</span>
            {PRIORITIES.map(p => (
              <button
                key={p}
                onClick={() => setPriorityFilter(p)}
                className={`text-xs px-3 py-1 rounded-full font-semibold border transition-all ${
                  priorityFilter === p
                    ? 'bg-amber-500/30 text-amber-300 border-amber-500/50'
                    : 'bg-white/5 text-white/50 border-white/10 hover:bg-white/10'
                }`}
              >
                {p === 'HIGH' ? '🔴' : p === 'MEDIUM' ? '🟡' : p === 'LOW' ? '🟢' : ''} {p}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-3">
            {/* Group by toggle */}
            <div className="flex items-center gap-1 bg-white/5 border border-white/10 rounded-lg p-1">
              {(['time', 'type'] as const).map(g => (
                <button
                  key={g}
                  onClick={() => setGroupBy(g)}
                  className={`text-xs px-3 py-1 rounded-md font-semibold transition-all capitalize ${
                    groupBy === g ? 'bg-white/15 text-white' : 'text-white/40 hover:text-white/70'
                  }`}
                >
                  {g === 'time' ? '⏱ Time' : '🏷 Type'}
                </button>
              ))}
            </div>

            {/* Show completed toggle */}
            <label className="flex items-center gap-2 text-xs text-white/50 cursor-pointer">
              <div
                onClick={() => setShowCompleted(!showCompleted)}
                className={`w-8 h-4 rounded-full transition-all relative cursor-pointer ${showCompleted ? 'bg-emerald-500' : 'bg-white/10'}`}
              >
                <div className={`absolute top-0.5 w-3 h-3 bg-white rounded-full transition-all ${showCompleted ? 'left-4' : 'left-0.5'}`} />
              </div>
              Show completed
            </label>
          </div>
        </div>
      </div>

      {/* Results count */}
      <p className="text-sm text-white/40">
        Showing <span className="text-white font-semibold">{filtered.length}</span> of {tasks.length} tasks
        {search && <> matching &quot;<span className="text-violet-400">{search}</span>&quot;</>}
      </p>

      {/* Task groups */}
      {isLoading ? (
        <div className="space-y-4">
          {Array(5).fill(0).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-xl bg-white/5" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="glass-panel p-16 text-center flex flex-col items-center border border-white/5">
          <CheckSquare className="w-14 h-14 text-white/10 mb-4" />
          <h3 className="text-lg font-semibold text-white/50">No tasks match your filters</h3>
          <p className="text-sm text-white/30 mt-1">Try adjusting your search or filter settings.</p>
        </div>
      ) : (
        <div className="space-y-8">
          {Array.from(grouped.entries()).map(([group, groupTasks]) => {
            const meta = groupBy === 'time'
              ? GROUP_META[group as TimeGroup]
              : { label: group.replace('_', ' '), color: 'text-white/70', dot: 'bg-white/30' };

            return (
              <motion.div
                key={group}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                className="space-y-3"
              >
                {/* Group header */}
                <div className="flex items-center gap-3 sticky top-0 bg-background/80 backdrop-blur-sm py-2 -mx-1 px-1 z-10 rounded-lg">
                  <div className={`w-2 h-2 rounded-full ${meta.dot}`} />
                  <h2 className={`text-sm font-bold uppercase tracking-widest ${meta.color}`}>
                    {meta.label}
                  </h2>
                  <div className="flex-1 h-px bg-white/5" />
                  <span className="text-xs text-white/30 font-medium">{groupTasks.length}</span>
                </div>

                {/* Tasks in group */}
                <div className="space-y-3">
                  <AnimatePresence>
                    {groupTasks.map(task => {
                      const taskDate = task.eventDate || task.deadline || task.dueDate;
                      return (
                        <div key={task.id}>
                          {/* Today countdown badge above card */}
                          {group === 'TODAY' && (
                            <div className="flex justify-end mb-1">
                              <CountdownBadge dateRaw={taskDate} />
                            </div>
                          )}
                          <TaskCard
                            task={task}
                            onToggleComplete={(id, status) =>
                              toggleComplete.mutate({ id, status })
                            }
                          />
                        </div>
                      );
                    })}
                  </AnimatePresence>
                </div>
              </motion.div>
            );
          })}
        </div>
      )}
    </div>
  );
}
