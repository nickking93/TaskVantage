export class Task {
  id?: string; // Unique identifier for the task
  title: string; // Title of the task
  description: string; // Description of the task
  dueDate?: string; // ISO string for due date (backend expects LocalDateTime)
  priority: string; // Priority of the task
  recurring: boolean; // Indicates if the task is recurring
  userId?: string; // ID of the user to whom the task belongs
  status?: string; // Current status of the task
  scheduledStart?: string; // ISO string for scheduled start time (backend expects LocalDateTime)
  completionDateTime?: string; // ISO string for completion date and time (backend expects LocalDateTime)
  duration?: number; // Duration of the task in minutes
  lastModifiedDate?: string; // ISO string for last modification date
  startDate?: string; // ISO string for start date
  notifyBeforeStart?: boolean; // Whether to notify before start
  public actualStart?: string // Add this line

  constructor(
    title: string,
    description: string,
    priority: string,
    recurring: boolean,
    dueDate?: string,
    userId?: string,
    status?: string,
    scheduledStart?: string,
    completionDateTime?: string,
    duration?: number,
    lastModifiedDate?: string,
    startDate?: string,
    notifyBeforeStart?: boolean,
    actualStart?: string // Add the actualStart to the constructor
  ) {
    this.title = title;
    this.description = description;
    this.priority = priority;
    this.recurring = recurring;
    this.dueDate = dueDate;
    this.userId = userId;
    this.status = status;
    this.scheduledStart = scheduledStart;
    this.completionDateTime = completionDateTime;
    this.duration = duration;
    this.lastModifiedDate = lastModifiedDate;
    this.startDate = startDate;
    this.notifyBeforeStart = notifyBeforeStart;
    this.actualStart = actualStart; // Initialize actualStart in the constructor
  }  

  // Methods to help with date formatting or conversions (if needed)
  public static toISO(date: Date | string | undefined): string | undefined {
    if (!date) return undefined;
    return typeof date === 'string' ? new Date(date).toISOString() : date.toISOString();
  }

  public static fromISO(isoString: string | undefined): Date | undefined {
    return isoString ? new Date(isoString) : undefined;
  }
}