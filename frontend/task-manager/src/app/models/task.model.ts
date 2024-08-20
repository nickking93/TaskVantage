export class Task {
    id?: string;
    title: string;
    description: string;
    dueDate: string;
    priority: string;
    recurring: boolean;
    userId?: string;
  
    constructor(
      title: string,
      description: string,
      dueDate: string,
      priority: string,
      recurring: boolean,
      userId?: string
    ) {
      this.title = title;
      this.description = description;
      this.dueDate = dueDate;
      this.priority = priority;
      this.recurring = recurring;
      this.userId = userId;
    }
  }
  