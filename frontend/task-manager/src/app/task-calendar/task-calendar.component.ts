import { Component, OnInit, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, Subscription } from 'rxjs';
import { CalendarModule, CalendarEvent, CalendarView, CalendarMonthViewDay, DateAdapter, CalendarUtils, CalendarA11y, CalendarDateFormatter, CalendarEventTitleFormatter } from 'angular-calendar';
import { adapterFactory } from 'angular-calendar/date-adapters/date-fns';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { EditTaskDialogComponent } from '../edit-task-dialog/edit-task-dialog.component';
import { isSameDay, isSameMonth } from 'date-fns';

interface TaskCalendarEvent extends CalendarEvent {
  task: Task;
}

@Component({
    selector: 'app-task-calendar',
    templateUrl: './task-calendar.component.html',
    styleUrls: ['./task-calendar.component.css'],
    imports: [
        CommonModule,
        CalendarModule,
        MatButtonModule,
        MatIconModule
    ],
    providers: [
        {
            provide: DateAdapter,
            useFactory: adapterFactory
        },
        CalendarUtils,
        CalendarA11y,
        CalendarDateFormatter,
        CalendarEventTitleFormatter
    ]
})
export class TaskCalendarComponent implements OnInit, OnDestroy, OnChanges {
  @Input() userId: string = '';

  viewDate: Date = new Date();
  view: CalendarView = CalendarView.Month;
  CalendarView = CalendarView;
  events: TaskCalendarEvent[] = [];
  refresh = new Subject<void>();
  activeDayIsOpen: boolean = false;

  private taskSubscription?: Subscription;

  constructor(
    private taskService: TaskService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    if (this.userId) {
      this.loadTasks();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['userId'] && changes['userId'].currentValue) {
      this.loadTasks();
    }
  }

  ngOnDestroy(): void {
    if (this.taskSubscription) {
      this.taskSubscription.unsubscribe();
    }
  }

  loadTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      this.events = tasks
        .filter(task => task.scheduledStart && !this.taskService.isCompletedStatus(task.status))
        .map(task => this.taskToEvent(task));
      this.refresh.next();
    });
  }

  private taskToEvent(task: Task): TaskCalendarEvent {
    const startDate = new Date(task.scheduledStart!);
    const endDate = task.dueDate ? new Date(task.dueDate) : startDate;

    return {
      start: startDate,
      end: endDate,
      title: task.title,
      color: this.getColorForPriority(task.priority),
      allDay: task.isAllDay,
      task: task
    };
  }

  private getColorForPriority(priority: string): { primary: string; secondary: string } {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
        return { primary: '#f44336', secondary: '#ffebee' };
      case 'MEDIUM':
        return { primary: '#ff9800', secondary: '#fff3e0' };
      case 'LOW':
        return { primary: '#4caf50', secondary: '#e8f5e9' };
      default:
        return { primary: '#11A3F8', secondary: '#e3f2fd' };
    }
  }

  previousMonth(): void {
    const date = new Date(this.viewDate);
    date.setMonth(date.getMonth() - 1);
    this.viewDate = date;
    this.activeDayIsOpen = false;
  }

  nextMonth(): void {
    const date = new Date(this.viewDate);
    date.setMonth(date.getMonth() + 1);
    this.viewDate = date;
    this.activeDayIsOpen = false;
  }

  today(): void {
    this.viewDate = new Date();
    this.activeDayIsOpen = false;
  }

  dayClicked({ day, sourceEvent }: { day: CalendarMonthViewDay; sourceEvent: MouseEvent | KeyboardEvent }): void {
    if (isSameMonth(day.date, this.viewDate)) {
      if (isSameDay(this.viewDate, day.date) && this.activeDayIsOpen) {
        this.activeDayIsOpen = false;
      } else if (day.events.length > 0) {
        this.activeDayIsOpen = true;
        this.viewDate = day.date;
      } else {
        // Close panel when clicking a day with no events
        this.activeDayIsOpen = false;
        this.viewDate = day.date;
      }
    }
  }

  eventClicked(event: TaskCalendarEvent): void {
    const dialogRef = this.dialog.open(EditTaskDialogComponent, {
      width: '500px',
      maxWidth: '95vw',
      data: { task: event.task }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.updated) {
        this.loadTasks();
      }
    });
  }

  beforeMonthViewRender({ body }: { body: CalendarMonthViewDay[] }): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    body.forEach(day => {
      const classes: string[] = [];

      if (day.events.length > 0) {
        classes.push('has-events');
      }

      if (day.date < today) {
        classes.push('cal-day-past');
      }

      if (classes.length > 0) {
        day.cssClass = classes.join(' ');
      }
    });
  }

  isTaskOverdue(event: TaskCalendarEvent): boolean {
    if (!event.task) return false;
    const now = new Date();
    const scheduledStart = new Date(event.task.scheduledStart!);
    const status = event.task.status?.toLowerCase();
    // Task is overdue if scheduled start has passed and status is still pending
    return scheduledStart < now && status === 'pending';
  }

  getTextColor(bgColor: string | undefined): string {
    if (!bgColor) return '#fff';
    // Convert hex to RGB and calculate luminance
    const hex = bgColor.replace('#', '');
    const r = parseInt(hex.substr(0, 2), 16);
    const g = parseInt(hex.substr(2, 2), 16);
    const b = parseInt(hex.substr(4, 2), 16);
    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.5 ? '#000' : '#fff';
  }
}
