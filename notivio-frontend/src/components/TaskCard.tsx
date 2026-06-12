'use client';

import { ExtractedTask } from '@/types';
import { motion, AnimatePresence, useMotionValue, useTransform, PanInfo } from 'framer-motion';
import {
  CalendarClock, MapPin, Mail, User, BookOpen, Zap,
  ChevronDown, CheckCircle2, Circle, Clock,
  Sparkles, AlertTriangle, BarChart2, BellRing, Trash2, Check
} from 'lucide-react';
import { format, formatDistanceToNow, isPast } from 'date-fns';
import { useState } from 'react';

interface TaskCardProps {
  task: ExtractedTask;
  onToggleComplete?: (id: string, currentStatus: boolean) => void;
}

const TYPE_CONFIG: Record<string, { color: string; bg: string; border: string; glow: string }> = {
  INTERVIEW:        { color: 'text-violet-400',  bg: 'bg-violet-500/15',  border: 'border-violet-500/30',  glow: 'shadow-[0_0_15px_rgba(139,92,246,0.25)]' },
  EXAM:             { color: 'text-red-400',      bg: 'bg-red-500/15',     border: 'border-red-500/30',     glow: 'shadow-[0_0_15px_rgba(239,68,68,0.25)]' },
  ASSIGNMENT:       { color: 'text-amber-400',    bg: 'bg-amber-500/15',   border: 'border-amber-500/30',   glow: 'shadow-[0_0_15px_rgba(245,158,11,0.25)]' },
  SUBMISSION:       { color: 'text-orange-400',   bg: 'bg-orange-500/15',  border: 'border-orange-500/30',  glow: 'shadow-[0_0_15px_rgba(249,115,22,0.25)]' },
  DEADLINE:         { color: 'text-rose-400',     bg: 'bg-rose-500/15',    border: 'border-rose-500/30',    glow: 'shadow-[0_0_15px_rgba(244,63,94,0.25)]' },
  MEETING:          { color: 'text-sky-400',      bg: 'bg-sky-500/15',     border: 'border-sky-500/30',     glow: 'shadow-[0_0_15px_rgba(14,165,233,0.25)]' },
  EVENT:            { color: 'text-cyan-400',     bg: 'bg-cyan-500/15',    border: 'border-cyan-500/30',    glow: 'shadow-[0_0_15px_rgba(6,182,212,0.25)]' },
  INTERNSHIP:       { color: 'text-emerald-400',  bg: 'bg-emerald-500/15', border: 'border-emerald-500/30', glow: 'shadow-[0_0_15px_rgba(52,211,153,0.25)]' },
  PLACEMENT:        { color: 'text-teal-400',     bg: 'bg-teal-500/15',    border: 'border-teal-500/30',    glow: 'shadow-[0_0_15px_rgba(45,212,191,0.25)]' },
  GENERAL_REMINDER: { color: 'text-blue-400',    bg: 'bg-blue-500/15',    border: 'border-blue-500/30',    glow: 'shadow-[0_0_15px_rgba(59,130,246,0.25)]' },
  OTHER:            { color: 'text-slate-400',    bg: 'bg-slate-500/15',   border: 'border-slate-500/30',   glow: 'shadow-[0_0_15px_rgba(148,163,184,0.15)]' },
};

const PRIORITY_CONFIG = {
  HIGH:   { color: 'text-rose-400',   icon: <AlertTriangle className="w-3 h-3" />,  label: 'High Priority' },
  MEDIUM: { color: 'text-amber-400',  icon: <BarChart2 className="w-3 h-3" />,      label: 'Medium' },
  LOW:    { color: 'text-emerald-400', icon: <BarChart2 className="w-3 h-3" />,     label: 'Low' },
};

function parseDate(raw: string | null | undefined): Date | null {
  if (!raw) return null;
  const d = new Date(raw);
  return isNaN(d.getTime()) ? null : d;
}

export function TaskCard({ task, onToggleComplete }: TaskCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  
  // Swipe gesture controls
  const x = useMotionValue(0);
  const opacity = useTransform(x, [-200, -100, 0, 100, 200], [0.5, 1, 1, 1, 0.5]);
  const scale = useTransform(x, [-200, 0, 200], [0.95, 1, 0.95]);
  const deleteIndicatorOpacity = useTransform(x, [-150, -50, 0], [1, 0.3, 0]);
  const completeIndicatorOpacity = useTransform(x, [0, 50, 150], [0, 0.3, 1]);

  // Resolve the best available date — prefer eventDate for events, else deadline, else dueDate
  const displayDate =
    parseDate(task.eventDate) ||
    parseDate(task.deadline) ||
    parseDate(task.dueDate ?? null);

  const isOverdue = displayDate ? isPast(displayDate) && !task.isCompleted : false;
  const typeKey = task.taskType || 'OTHER';
  const cfg = TYPE_CONFIG[typeKey] || TYPE_CONFIG.OTHER;
  const priorityCfg = PRIORITY_CONFIG[task.priority || 'MEDIUM'];
  const isCompleted = task.isCompleted || task.status === 'COMPLETED';

  const handleDragEnd = (_event: MouseEvent | TouchEvent | PointerEvent, info: PanInfo) => {
    const threshold = 100;
    if (info.offset.x < -threshold) {
      // Swiped left - delete action
      setIsDeleting(true);
      setTimeout(() => {
        // Handle delete action here
        console.log('Delete task:', task.id);
      }, 300);
    } else if (info.offset.x > threshold) {
      // Swiped right - complete action
      onToggleComplete?.(task.id, isCompleted);
      x.set(0);
    } else {
      x.set(0);
    }
  };

  if (isDeleting) {
    return (
      <motion.div
        initial={{ opacity: 1, height: 'auto' }}
        animate={{ opacity: 0, height: 0, marginBottom: 0 }}
        transition={{ duration: 0.3 }}
      />
    );
  }

  return (
    <div className="relative">
      {/* Background action indicators */}
      <motion.div
        style={{ opacity: completeIndicatorOpacity }}
        className="absolute inset-y-0 left-0 right-0 rounded-3xl bg-gradient-to-r from-emerald-500/20 to-transparent flex items-center justify-start pl-8 pointer-events-none z-0"
      >
        <Check className="w-8 h-8 text-emerald-400" />
      </motion.div>
      <motion.div
        style={{ opacity: deleteIndicatorOpacity }}
        className="absolute inset-y-0 left-0 right-0 rounded-3xl bg-gradient-to-l from-rose-500/20 to-transparent flex items-center justify-end pr-8 pointer-events-none z-0"
      >
        <Trash2 className="w-8 h-8 text-rose-400" />
      </motion.div>

      {/* Main draggable card */}
      <motion.div
        drag="x"
        dragConstraints={{ left: -200, right: 200 }}
        dragElastic={0.2}
        onDragEnd={handleDragEnd}
        style={{ x, opacity, scale }}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        whileTap={{ cursor: 'grabbing' }}
        transition={{ type: 'spring', stiffness: 300, damping: 30 }}
        className="relative z-10"
      >
        <motion.div
          whileHover={{ y: -4, transition: { duration: 0.2 } }}
          whileTap={{ scale: 0.98 }}
          className={`
            relative overflow-hidden rounded-3xl border transition-all duration-300
            ${isCompleted ? 'opacity-60' : ''}
            ${isOverdue ? 'border-rose-500/50 bg-gradient-to-br from-rose-950/20 to-rose-900/10' : `bg-gradient-to-br from-white/[0.08] to-white/[0.03] ${cfg.border}`}
            ${isExpanded ? `${cfg.glow} shadow-2xl` : 'shadow-lg'}
            backdrop-blur-2xl
          `}
        >
          {/* Animated gradient overlay for overdue tasks */}
          {isOverdue && (
            <div className="absolute inset-0 bg-gradient-to-br from-rose-500/10 via-transparent to-orange-500/10 animate-gradient pointer-events-none" />
          )}
          
          {/* Glowing left accent bar with animation */}
          <motion.div 
            className={`absolute left-0 top-0 bottom-0 w-1 rounded-l-3xl ${cfg.bg.replace('/15', '/80')}`}
            animate={{ scaleY: [1, 1.05, 1], opacity: [0.8, 1, 0.8] }}
            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
          />

          {/* Main card content */}
          <div className="px-6 py-5 pl-7 relative">
            <div className="flex items-start gap-4">
              
              {/* Completion toggle with haptic feel */}
              <motion.button
                onClick={() => onToggleComplete?.(task.id, isCompleted)}
                className="mt-0.5 flex-shrink-0 group relative"
                whileTap={{ scale: 0.9 }}
                whileHover={{ scale: 1.1 }}
              >
                {isCompleted ? (
                  <motion.div
                    initial={{ scale: 0, rotate: -180 }}
                    animate={{ scale: 1, rotate: 0 }}
                    transition={{ type: 'spring', stiffness: 500, damping: 25 }}
                  >
                    <CheckCircle2 className="w-7 h-7 text-emerald-400 drop-shadow-[0_0_12px_rgba(52,211,153,0.8)]" />
                  </motion.div>
                ) : (
                  <Circle className="w-7 h-7 text-white/30 group-hover:text-white/60 transition-all" />
                )}
                {/* Ripple effect */}
                {!isCompleted && (
                  <motion.div
                    className="absolute inset-0 rounded-full bg-white/20"
                    initial={{ scale: 1, opacity: 0 }}
                    whileTap={{ scale: 2, opacity: [0.5, 0] }}
                    transition={{ duration: 0.4 }}
                  />
                )}
              </motion.button>

              <div className="flex-1 min-w-0">
                
                {/* Header row */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex-1 min-w-0">
                    <motion.h3 
                      className={`font-semibold text-base leading-snug ${isCompleted ? 'line-through text-white/40' : 'text-white'}`}
                      layoutId={`title-${task.id}`}
                    >
                      {task.title}
                    </motion.h3>
                    {task.organizer && (
                      <motion.p 
                        initial={{ opacity: 0, y: -5 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="text-sm text-white/50 mt-1 flex items-center gap-1.5"
                      >
                        <User className="w-3.5 h-3.5" />
                        {task.organizer}
                      </motion.p>
                    )}
                  </div>

                  <div className="flex flex-col items-end gap-2 flex-shrink-0">
                    {/* Priority badge with glow */}
                    <motion.span 
                      className={`flex items-center gap-1.5 text-[10px] font-bold ${priorityCfg.color} bg-black/30 backdrop-blur-sm px-2.5 py-1.5 rounded-full border border-white/10`}
                      whileHover={{ scale: 1.05 }}
                    >
                      {priorityCfg.icon}
                      {priorityCfg.label}
                    </motion.span>
                    {/* Type badge */}
                    <motion.span 
                      className={`text-[11px] font-bold px-3 py-1.5 rounded-full ${cfg.bg} ${cfg.color} ${cfg.border} border backdrop-blur-sm`}
                      whileHover={{ scale: 1.05 }}
                      animate={{ 
                        boxShadow: [
                          '0 0 10px rgba(255,255,255,0.1)',
                          '0 0 20px rgba(255,255,255,0.2)',
                          '0 0 10px rgba(255,255,255,0.1)'
                        ]
                      }}
                      transition={{ duration: 2, repeat: Infinity }}
                    >
                      {typeKey.replace('_', ' ')}
                    </motion.span>
                  </div>
                </div>

                {/* Date row with enhanced styling */}
                <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm mb-3">
                  {displayDate ? (
                    <>
                      <motion.span 
                        className={`flex items-center gap-2 font-semibold px-3 py-1.5 rounded-full ${
                          isOverdue 
                            ? 'text-rose-400 bg-rose-500/20 border border-rose-500/30 animate-badge-pulse' 
                            : 'text-sky-400 bg-sky-500/15 border border-sky-500/25'
                        }`}
                        animate={isOverdue ? { scale: [1, 1.02, 1] } : {}}
                        transition={{ duration: 1.5, repeat: Infinity }}
                      >
                        <CalendarClock className="w-4 h-4" />
                        {isOverdue ? '⚠ Overdue · ' : ''}{formatDistanceToNow(displayDate, { addSuffix: true })}
                      </motion.span>
                      <span className="text-white/40 text-xs">
                        {format(displayDate, 'EEE, dd MMM · h:mm a')}
                      </span>
                    </>
                  ) : (
                    <span className="flex items-center gap-2 text-white/30 text-sm px-3 py-1.5 rounded-full bg-white/5">
                      <Clock className="w-4 h-4" /> No due date
                    </span>
                  )}

                  {task.location && (
                    <motion.span 
                      className="flex items-center gap-1.5 text-white/50 text-xs bg-white/5 px-3 py-1.5 rounded-full border border-white/10"
                      whileHover={{ scale: 1.05, backgroundColor: 'rgba(255,255,255,0.1)' }}
                    >
                      <MapPin className="w-3.5 h-3.5" />
                      {task.location}
                    </motion.span>
                  )}
                </div>

                {/* AI summary with shimmer effect */}
                {task.aiSummary && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    className="relative overflow-hidden"
                  >
                    <p className="text-xs text-white/60 italic leading-relaxed line-clamp-2 bg-gradient-to-r from-violet-500/10 to-purple-500/10 border border-violet-500/20 rounded-xl px-3 py-2">
                      <Sparkles className="w-3.5 h-3.5 inline mr-1.5 text-violet-400" />
                      {task.aiSummary}
                    </p>
                  </motion.div>
                )}

              </div>
            </div>

            {/* Expand/Collapse button with smooth animation */}
            <motion.div className="mt-4 flex justify-center">
              <motion.button
                onClick={() => setIsExpanded(!isExpanded)}
                className={`
                  flex items-center gap-2 text-xs font-bold px-4 py-2 rounded-full transition-all duration-300
                  ${isExpanded
                    ? `${cfg.bg} ${cfg.color} ${cfg.border} border backdrop-blur-sm`
                    : 'text-white/40 hover:text-white/70 bg-white/5 hover:bg-white/10 border border-white/10'}
                `}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                <motion.div
                  animate={{ rotate: isExpanded ? 180 : 0 }}
                  transition={{ duration: 0.3, ease: 'easeInOut' }}
                >
                  <ChevronDown className="w-4 h-4" />
                </motion.div>
                {isExpanded ? 'Hide Details' : 'View Details'}
              </motion.button>
            </motion.div>
          </div>

          {/* Expandable detail panel with spring animation */}
          <AnimatePresence initial={false}>
            {isExpanded && (
              <motion.div
                key="details"
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ 
                  height: { type: 'spring', stiffness: 300, damping: 30 },
                  opacity: { duration: 0.2 }
                }}
                className="overflow-hidden"
              >
                <div className={`mx-5 mb-5 rounded-2xl border ${cfg.border} ${cfg.bg} backdrop-blur-xl p-5 space-y-5`}>
                  
                  {/* AI Summary */}
                  {task.aiSummary && (
                    <motion.div
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.1 }}
                    >
                      <p className={`text-[10px] uppercase tracking-widest font-bold mb-2 ${cfg.color} flex items-center gap-1.5`}>
                        <Sparkles className="w-3.5 h-3.5" /> AI Summary
                      </p>
                      <p className="text-sm text-white/80 leading-relaxed bg-black/20 rounded-xl p-3">{task.aiSummary}</p>
                    </motion.div>
                  )}

                  {/* Full Description */}
                  {task.description && (
                    <motion.div
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.15 }}
                    >
                      <p className={`text-[10px] uppercase tracking-widest font-bold mb-2 ${cfg.color} flex items-center gap-1.5`}>
                        <BookOpen className="w-3.5 h-3.5" /> Full Description
                      </p>
                      <p className="text-sm text-white/70 leading-relaxed whitespace-pre-wrap bg-black/20 rounded-xl p-3">{task.description}</p>
                    </motion.div>
                  )}

                  {/* Metadata grid with staggered animation */}
                  <motion.div 
                    className="grid grid-cols-2 gap-3"
                    initial="hidden"
                    animate="visible"
                    variants={{
                      visible: { transition: { staggerChildren: 0.05 } }
                    }}
                  >
                    {task.eventDate && (
                      <DetailChip icon={<CalendarClock className="w-4 h-4" />} label="Event Date"
                        value={format(new Date(task.eventDate), 'EEE, dd MMM yyyy · h:mm a')} color={cfg.color} />
                    )}
                    {task.deadline && task.deadline !== task.eventDate && (
                      <DetailChip icon={<Clock className="w-4 h-4" />} label="Deadline"
                        value={format(new Date(task.deadline), 'EEE, dd MMM yyyy · h:mm a')} color={cfg.color} />
                    )}
                    {task.location && (
                      <DetailChip icon={<MapPin className="w-4 h-4" />} label="Location"
                        value={task.location} color={cfg.color} />
                    )}
                    {task.organizer && (
                      <DetailChip icon={<User className="w-4 h-4" />} label="Organizer"
                        value={task.organizer} color={cfg.color} />
                    )}
                    {task.sourceEmailSender && (
                      <DetailChip icon={<Mail className="w-4 h-4" />} label="From"
                        value={task.sourceEmailSender} color={cfg.color} />
                    )}
                    {task.courseName && (
                      <DetailChip icon={<BookOpen className="w-4 h-4" />} label="Course"
                        value={task.courseName} color={cfg.color} />
                    )}
                    {task.aiConfidence != null && (
                      <DetailChip icon={<Zap className="w-4 h-4" />} label="AI Confidence"
                        value={`${Math.round(task.aiConfidence)}%`} color={cfg.color} />
                    )}
                    {task.isReminderCreated && (
                      <DetailChip icon={<BellRing className="w-4 h-4" />} label="Reminder"
                        value="Scheduled ✓" color="text-emerald-400" />
                    )}
                  </motion.div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>
      </motion.div>
    </div>
  );
}

function DetailChip({
  icon, label, value, color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  color: string;
}) {
  return (
    <motion.div 
      className="bg-black/30 backdrop-blur-sm rounded-xl p-3 border border-white/5"
      variants={{
        hidden: { opacity: 0, y: 10 },
        visible: { opacity: 1, y: 0 }
      }}
      whileHover={{ scale: 1.02, backgroundColor: 'rgba(0,0,0,0.4)' }}
    >
      <p className={`text-[9px] uppercase tracking-widest font-bold mb-1 ${color} flex items-center gap-1.5`}>
        {icon} {label}
      </p>
      <p className="text-xs text-white/80 font-medium leading-relaxed">{value}</p>
    </motion.div>
  );
}
