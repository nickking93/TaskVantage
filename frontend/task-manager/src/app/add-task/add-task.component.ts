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

  userId: string = ''; 
  dueDate: Date | null = null; 
  dueTime: string = ''; 
  scheduledStartDate: Date | null = null; 
  scheduledStartTime: string = ''; 

  dueDateInvalid: boolean = false;
  scheduledStartDateInvalid: boolean = false;
  formInvalid: boolean = false;

  newTask: Task = new Task(
    '',               
    '',               
    'Medium',         
    false,            
    undefined,        
    '',               
    'Pending',        
    undefined,        
    undefined,        
    undefined,        
    undefined,        
    undefined         
  );

  constructor(
    private route: ActivatedRoute, 
    private router: Router, 
    private taskService: TaskService,
    private dialog: MatDialog  
  ) {}

  ngOnInit(): void {
    this.route.parent?.paramMap.subscribe(params => {
      this.userId = params.get('userId') || '';
      this.newTask.userId = this.userId;
    });
  }

  validateDates(): void {
    const now = new Date();
  
    if (this.scheduledStartDate && this.scheduledStartTime) {
      const scheduledStart = new Date(this.scheduledStartDate);
      const [startHours, startMinutes] = this.scheduledStartTime.split(':').map(Number);
      scheduledStart.setHours(startHours, startMinutes, 0);
      this.scheduledStartDateInvalid = scheduledStart <= now ? true : false;  // Ensure only boolean values are assigned
    } else {
      this.scheduledStartDateInvalid = false;  // Default to false if no start date
    }
  
    if (this.dueDate && this.dueTime) {
      const due = new Date(this.dueDate);
      const [dueHours, dueMinutes] = this.dueTime.split(':').map(Number);
      due.setHours(dueHours, dueMinutes, 0);
      this.dueDateInvalid = due <= now || (this.scheduledStartDate && due <= this.scheduledStartDate) ? true : false;  // Ensure only boolean values are assigned
    } else {
      this.dueDateInvalid = false;  // Default to false if no due date
    }
  
    this.formInvalid = this.dueDateInvalid || this.scheduledStartDateInvalid;
  }

  createTask(): void {
    this.newTask.userId = this.userId;

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

    console.log('Task to be created:', this.newTask);

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
      this.router.navigate([`/home/${this.userId}`]);
    });
  }
}