export class Task {
  id?: string;
  title: string;
  description: string;
  dueDate: string;
  priority: string;
  recurring: boolean;
  userId?: string;
  status?: string; 
  scheduledStart?: string;
  completion_date_time?: string;
  duration?: string;
  lastModifiedDate?: string; 
  startDate?: string;

  constructor(
    title: string,
    description: string,
    dueDate: string,
    priority: string,
    recurring: boolean,
    userId?: string,
    status?: string, 
    scheduledStart?: string,
    completion_date_time?: string,
    duration?: string,
    lastModifiedDate?: string,
    startDate?: string
  ) {
    this.title = title;
    this.description = description;
    this.dueDate = dueDate;
    this.priority = priority;
    this.recurring = recurring;
    this.userId = userId;
    this.status = status; 
    this.scheduledStart = scheduledStart;
    this.completion_date_time = completion_date_time;
    this.duration = duration;
    this.lastModifiedDate = lastModifiedDate; 
    this.startDate = startDate;
  }
}
