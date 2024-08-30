import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { TaskService } from '../services/task.service'; // Adjust the import path as necessary
import { MatDialog } from '@angular/material/dialog';
import { Task } from '../models/task.model'; // Adjust the import path as necessary
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';

@Component({
  selector: 'app-add-task',
  standalone: true,
  imports: [
    CommonModule,
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
export class AddTaskComponent {

  userId: string = ''; // The user ID should be passed in or obtained from a service
  dueDate: string = '';
  dueTime: string = '';
  scheduledStartDate: string = '';
  scheduledStartTime: string = '';

  // Instantiate the Task object using the constructor
  newTask: Task = new Task(
    '', // title
    '', // description
    '', // dueDate
    'Medium', // priority
    false, // recurring
    this.userId, // userId
    'Pending', // status
    '', // scheduledStart
    '', // completion_date_time
    '', // duration
    '', // lastModifiedDate
    ''  // start_date
  );

  constructor(
    private taskService: TaskService,
    private dialog: MatDialog  
  ) {}

  createTask(): void {
    this.newTask.userId = this.userId;

    // Combine date and time into a full datetime string for dueDate
    this.newTask.dueDate = this.combineDateAndTime(this.dueDate, this.dueTime);

    // Combine date and time into a full datetime string for scheduledStart
    this.newTask.scheduledStart = this.combineDateAndTime(this.scheduledStartDate, this.scheduledStartTime);

    // Send the task to the backend
    this.taskService.createTask(this.newTask).subscribe(
      () => {
        this.openSuccessDialog();
        // Reset the form or navigate back after successful task creation
      },
      error => {
        console.error('Failed to create task:', error);
      }
    );
  }

  // Helper method to combine date and time into a full datetime string
  combineDateAndTime(date: any, time: string): string {
    let year, month, day;

    // Check if the date is a string or Date object and extract the components
    if (typeof date === 'string') {
      [year, month, day] = date.split('-').map(Number);
    } else if (date instanceof Date) {
      year = date.getFullYear();
      month = date.getMonth() + 1;
      day = date.getDate();
    } else {
      throw new Error('Invalid date format');
    }

    // Ensure month and day are two digits
    const formattedMonth = month < 10 ? `0${month}` : `${month}`;
    const formattedDay = day < 10 ? `0${day}` : `${day}`;

    // Create a Date object with the provided date and time in local time
    const localDateTime = new Date(`${year}-${formattedMonth}-${formattedDay}T${time}:00`);

    // Convert to UTC and return the ISO string
    const utcDateTime = new Date(localDateTime.getTime() - (localDateTime.getTimezoneOffset() * 60000));

    return utcDateTime.toISOString(); // Returning the UTC datetime string
  }

  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { message: 'Task created successfully!' }
    });
  }
}
