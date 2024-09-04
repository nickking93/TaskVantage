import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { TaskService } from '../services/task.service';
import { MatDialog } from '@angular/material/dialog';
import { Task } from '../models/task.model';
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
export class AddTaskComponent implements OnInit {

  userId: string = ''; // The user ID will be retrieved from the route parameters
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
    this.userId, // userId will be set later
    'Pending', // status
    '', // scheduledStart
    '', // completion_date_time
    '', // duration
    '', // lastModifiedDate
    '',  // start_date
    false // notifyBeforeStart - initialize to false
  );

  constructor(
    private route: ActivatedRoute, // Inject ActivatedRoute to access route parameters
    private router: Router, // Inject Router to navigate after task creation
    private taskService: TaskService,
    private dialog: MatDialog  
  ) {}

  ngOnInit(): void {
    // Retrieve the userId from the parent route parameters
    this.route.parent?.paramMap.subscribe(params => {
      this.userId = params.get('userId') || '';
      this.newTask.userId = this.userId; // Assign userId to the newTask object
    });
  }

  createTask(): void {
    // Ensure the userId is correctly set before creating the task
    this.newTask.userId = this.userId;
  
    // Combine date and time into a full datetime string for dueDate
    this.newTask.dueDate = this.combineDateAndTime(this.dueDate, this.dueTime);
  
    // Combine date and time into a full datetime string for scheduledStart
    this.newTask.scheduledStart = this.combineDateAndTime(this.scheduledStartDate, this.scheduledStartTime);
  
    // Log the task before sending to the backend
    console.log('Task before sending to backend:', this.newTask);
    console.log('Due Date in UTC:', this.newTask.dueDate);
    console.log('Scheduled Start in UTC:', this.newTask.scheduledStart);
  
    // Send the task to the backend
    this.taskService.createTask(this.newTask).subscribe(
      () => {
        this.openSuccessDialog();
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

    console.log("Local date/time:", localDateTime.toISOString());
    console.log("UTC date/time:", utcDateTime.toISOString()); // Log to check in production

    return utcDateTime.toISOString(); // Returning the UTC datetime string
}

  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { message: 'Task created successfully!' }
    }).afterClosed().subscribe(() => {
      // Navigate back to the main home page after the dialog is closed
      this.router.navigate([`/home/${this.userId}`]);
    });
  }
}
