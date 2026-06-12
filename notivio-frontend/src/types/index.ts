export interface ExtractedTask {
  id: string;
  title: string;
  description: string;
  taskType: 'ASSIGNMENT' | 'EXAM' | 'INTERVIEW' | 'MEETING' | 'EVENT' | 'INTERNSHIP' | 'PLACEMENT' | 'SUBMISSION' | 'DEADLINE' | 'GENERAL_REMINDER' | 'OTHER';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE' | 'CANCELLED';
  deadline: string | null;      // ISO — the due date/prep deadline
  eventDate: string | null;     // ISO — when the event actually happens
  location: string | null;
  organizer: string | null;
  courseName: string | null;
  sourceEmailSender: string | null;
  aiConfidence: number | null;
  aiSummary: string | null;
  isCompleted: boolean;         // derived from status
  isReminderCreated: boolean;
  createdAt: string;
  updatedAt: string;

  // Legacy / compat — keep so old code doesn't break
  dueDate?: string;
  companyName?: string;
  interviewMode?: string;
  interviewRound?: string;
  emailSubject?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
