export class Task {
  id?: string;
  title: string;
  description: string;
  dueDate?: string;
  priority: string;
  recurring: boolean;
  userId?: string;
  groupId?: number | null;
  status?: string; 
  scheduledStart?: string; 
  completionDateTime?: string; 
  duration?: number; 
  lastModifiedDate?: string; 
  startDate?: string; 
  notifyBeforeStart?: boolean; 
  isAllDay!: boolean; 
  public actualStart?: string 

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
    actualStart?: string, 
    isAllDay = false    
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
    this.actualStart = actualStart; 
    this.isAllDay = isAllDay;
  }  

  // Methods to help with date formatting or conversions 
  public static toISO(date: Date | string | undefined): string | undefined {
    if (!date) return undefined;
    return typeof date === 'string' ? new Date(date).toISOString() : date.toISOString();
  }

  public static fromISO(isoString: string | undefined): Date | undefined {
    return isoString ? new Date(isoString) : undefined;
  }
}