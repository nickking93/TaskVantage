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
  dueDate: string = ''; // Holds the due date as an ISO string
  dueTime: string = ''; // Holds the due time in HH:mm format
  scheduledStartDate: string = ''; // Holds the scheduled start date as an ISO string
  scheduledStartTime: string = ''; // Holds the scheduled start time in HH:mm format

  dueDateInvalid: boolean = false;
  scheduledStartDateInvalid: boolean = false;
  formInvalid: boolean = false;

  // Instantiate the Task object using the constructor
  newTask: Task = new Task(
    '',               // title
    '',               // description
    'Medium',         // priority
    false,            // recurring
    undefined,        // dueDate (optional, will be updated later)
    '',               // userId (will be set later)
    'Pending',        // status
    undefined,        // scheduledStart (optional, will be updated later)
    undefined,        // completionDateTime (optional, will be updated later)
    undefined,        // duration (optional, will be updated later)
    undefined,        // lastModifiedDate (optional, will be updated later)
    undefined         // startDate (optional, will be updated later)
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

  validateDates(): void {
    const now = new Date();
  
    // Combine scheduled start date and time
    const scheduledStart = new Date(`${this.scheduledStartDate}T${this.scheduledStartTime}`);
    this.scheduledStartDateInvalid = scheduledStart <= now;
  
    // Combine due date and time
    const due = new Date(`${this.dueDate}T${this.dueTime}`);
    this.dueDateInvalid = due <= now || due <= scheduledStart;
  
    // Disable form submission if there are any errors
    this.formInvalid = this.dueDateInvalid || this.scheduledStartDateInvalid;
  }  

    createTask(): void {
      this.newTask.userId = this.userId;
    
      // Log the input values to debug their format
      console.log('Due Date:', this.dueDate);
      console.log('Due Time:', this.dueTime);
    
      // Ensure dueDate and dueTime are valid before creating the Date object
      if (this.dueDate && this.dueTime) {
        try {
          // Parse the dueDate and dueTime separately
          const dueDateObj = new Date(this.dueDate);  // DueDate comes as a Date object
    
          if (isNaN(dueDateObj.getTime())) {
            console.error('Invalid Due Date:', this.dueDate);
            throw new Error('Invalid due date');
          }
    
          // Split the dueTime string into hours and minutes
          const [hours, minutes] = this.dueTime.split(':').map(Number);
    
          if (isNaN(hours) || isNaN(minutes)) {
            console.error('Invalid Due Time:', this.dueTime);
            throw new Error('Invalid due time');
          }
    
          // Combine date and time in UTC
          const dueDateTime = new Date(Date.UTC(
            dueDateObj.getFullYear(),
            dueDateObj.getMonth(),
            dueDateObj.getDate(),
            hours,
            minutes
          ));
    
          if (isNaN(dueDateTime.getTime())) {
            console.error('Parsed Due Date and Time is invalid:', dueDateTime);
            throw new Error('Invalid due date or time');
          }
    
          // Convert the Date object to ISO string for backend use in UTC
          this.newTask.dueDate = dueDateTime.toISOString();
          console.log('Converted Due Date ISO:', this.newTask.dueDate);
        } catch (error) {
          if (error instanceof Error) {
            console.error('Invalid Due Date or Time:', error.message);
          } else {
            console.error('Unexpected error:', error);
          }
          return;
        }
      } else {
        console.warn('Due date or time is missing');
        return;
      }
    
      // Log the input values to debug their format for the scheduled start
      console.log('Scheduled Start Date:', this.scheduledStartDate);
      console.log('Scheduled Start Time:', this.scheduledStartTime);
    
      // Ensure scheduledStartDate and scheduledStartTime are valid before creating the Date object
      if (this.scheduledStartDate && this.scheduledStartTime) {
        try {
          // Parse the scheduledStartDate and scheduledStartTime separately
          const scheduledStartDateObj = new Date(this.scheduledStartDate);
    
          if (isNaN(scheduledStartDateObj.getTime())) {
            console.error('Invalid Scheduled Start Date:', this.scheduledStartDate);
            throw new Error('Invalid scheduled start date');
          }
    
          // Split the scheduledStartTime into hours and minutes
          const [startHours, startMinutes] = this.scheduledStartTime.split(':').map(Number);
    
          if (isNaN(startHours) || isNaN(startMinutes)) {
            console.error('Invalid Scheduled Start Time:', this.scheduledStartTime);
            throw new Error('Invalid scheduled start time');
          }
    
          // Combine date and time in UTC
          const scheduledStartDateTime = new Date(Date.UTC(
            scheduledStartDateObj.getFullYear(),
            scheduledStartDateObj.getMonth(),
            scheduledStartDateObj.getDate(),
            startHours,
            startMinutes
          ));
    
          if (isNaN(scheduledStartDateTime.getTime())) {
            console.error('Parsed Scheduled Start Date and Time is invalid:', scheduledStartDateTime);
            throw new Error('Invalid scheduled start date or time');
          }
    
          // Convert the Date object to ISO string for backend use in UTC
          this.newTask.scheduledStart = scheduledStartDateTime.toISOString();
          console.log('Converted Scheduled Start Date ISO:', this.newTask.scheduledStart);
        } catch (error) {
          if (error instanceof Error) {
            console.error('Invalid Scheduled Start Date or Time:', error.message);
          } else {
            console.error('Unexpected error:', error);
          }
          return;
        }
      } else {
        console.warn('Scheduled start date or time is missing');
        return;
      }
    
      // Log the task object before sending to backend
      console.log('Task to be created:', this.newTask);
    
      // Call the service to add the task
      this.taskService.createTask(this.newTask).subscribe(
        response => {
          console.log('Task created successfully:', response);
          this.dialog.open(SuccessDialogComponent, { data: { message: 'Task added successfully!' } });
          this.router.navigate(['/home', this.userId]);
        },
        error => {
          console.error('Error adding task:', error);
        }
      );
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