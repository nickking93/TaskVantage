import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';

import { TaskService } from '../services/task.service';
import { MatDialog } from '@angular/material/dialog';
import { Task } from '../models/task.model';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';

@Component({
    selector: 'app-add-task',
    imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule
],
    templateUrl: './add-task.component.html',
    styleUrls: ['./add-task.component.css']
})
export class AddTaskComponent implements OnInit {
  userId: string = ''; 
  dueDate: Date | null = null; 
  dueTime: string = ''; 
  scheduledStartDate: Date | null = null; 
  scheduledStartTime: string = ''; 

  dueDateInvalid: boolean = false;
  scheduledStartDateInvalid: boolean = false;
  formInvalid: boolean = false;

  newTask: Task = new Task(
    '',               // title
    '',               // description
    'Medium',         // priority
    false,            // recurring
    undefined,        // dueDate
    '',               // userId
    'Pending',        // status
    undefined,        // scheduledStart
    undefined,        // completionDateTime
    undefined,        // duration
    undefined,        // lastModifiedDate
    undefined,        // startDate
    false,            // notifyBeforeStart
    undefined,        // actualStart
    false             // isAllDay
  );

  constructor(
    private router: Router,
    private taskService: TaskService,
    private dialog: MatDialog,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.getUserDetails().subscribe({
      next: (user) => {
        this.userId = String(user.id);
        this.newTask.userId = this.userId;
      },
      error: (err) => {
        this.router.navigate(['/login']);
      }
    });
  }

  validateDates(): void {
    const now = new Date();
  
    if (this.newTask?.isAllDay) {
      // All-day task validation
      now.setHours(0, 0, 0, 0);
  
      if (this.scheduledStartDate) {
        const scheduledStart = new Date(this.scheduledStartDate);
        scheduledStart.setHours(0, 0, 0, 0);
        this.scheduledStartDateInvalid = scheduledStart < now;
      } else {
        this.scheduledStartDateInvalid = false;
      }
  
      if (this.dueDate) {
        const due = new Date(this.dueDate);
        due.setHours(0, 0, 0, 0);
        this.dueDateInvalid =
          due < now ||
          (this.scheduledStartDate
            ? due < new Date(this.scheduledStartDate)
            : false);
      } else {
        this.dueDateInvalid = false;
      }
    } else {
      // Regular task validation
      if (this.scheduledStartDate && this.scheduledStartTime) {
        const scheduledStart = new Date(this.scheduledStartDate);
        const [startHours, startMinutes] = this.scheduledStartTime
          .split(":")
          .map(Number);
        scheduledStart.setHours(startHours, startMinutes, 0);
        this.scheduledStartDateInvalid = scheduledStart <= now;
      } else {
        this.scheduledStartDateInvalid = false;
      }
  
      if (this.dueDate && this.dueTime) {
        const due = new Date(this.dueDate);
        const [dueHours, dueMinutes] = this.dueTime.split(":").map(Number);
        due.setHours(dueHours, dueMinutes, 0);
        this.dueDateInvalid =
          due <= now ||
          (this.scheduledStartDate && this.scheduledStartTime
            ? due <= new Date(this.scheduledStartDate)
            : false);
      } else {
        this.dueDateInvalid = false;
      }
    }
  
    this.formInvalid = !!this.dueDateInvalid || !!this.scheduledStartDateInvalid;
  }
  

  createTask(): void {
    this.newTask.userId = this.userId;
  
    if (this.newTask.isAllDay) {
      // For all-day tasks, set times to start/end of day
      if (this.dueDate) {
        const dueDateObj = new Date(this.dueDate);
        dueDateObj.setHours(23, 59, 59);
        this.newTask.dueDate = dueDateObj.toISOString();
      }
      
      if (this.scheduledStartDate) {
        const scheduledStartObj = new Date(this.scheduledStartDate);
        scheduledStartObj.setHours(0, 0, 0);
        this.newTask.scheduledStart = scheduledStartObj.toISOString();
      }
    } else {
      // Regular tasks with specific times
      if (this.dueDate && this.dueTime) {
        const dueDateObj = new Date(this.dueDate);
        const [hours, minutes] = this.dueTime.split(':').map(Number);
        dueDateObj.setHours(hours, minutes, 0);
        this.newTask.dueDate = dueDateObj.toISOString();
      }
    
      if (this.scheduledStartDate && this.scheduledStartTime) {
        const scheduledStartObj = new Date(this.scheduledStartDate);
        const [startHours, startMinutes] = this.scheduledStartTime.split(':').map(Number);
        scheduledStartObj.setHours(startHours, startMinutes, 0);
        this.newTask.scheduledStart = scheduledStartObj.toISOString();
      }
    }
  
    this.taskService.createTask(this.newTask).subscribe({
      next: (response) => {
        this.dialog.open(SuccessDialogComponent, {
          width: '300px',
          data: { title: 'Success', message: 'Task added successfully!' }
        });
        this.router.navigate(['/home']);
      },
      error: (error) => {
        this.dialog.open(SuccessDialogComponent, {
          width: '300px',
          data: { title: 'Error', message: 'Failed to create task. Please try again.' }
        });
      }
    });
  }

  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { title: 'Success', message: 'Task created successfully!' }
    }).afterClosed().subscribe(() => {
      this.router.navigate(['/home']);
    });
  }
}