import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select'; // For priority dropdown
import { MatCheckboxModule } from '@angular/material/checkbox'; // For checkboxes
import { MatDialog } from '@angular/material/dialog'; // Import MatDialog
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component'; // Import SuccessDialogComponent

@Component({
  selector: 'app-update-task',
  standalone: true,
  templateUrl: './update-task.component.html',
  styleUrls: ['./update-task.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatCheckboxModule
  ]
})
export class UpdateTaskComponent implements OnInit {
  taskForm!: FormGroup;
  task!: any;

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private router: Router,
    private fb: FormBuilder,
    private dialog: MatDialog // Inject MatDialog
  ) {}

  ngOnInit(): void {
    this.initForm();

    const taskId = this.route.snapshot.paramMap.get('taskId');
    if (taskId) {
      this.taskService.getTaskById(taskId).subscribe((task: any) => {
        console.log('Fetched Task:', task);
        this.task = task;
        this.populateForm();
      });
    }
  }

  // Initialize form with fields
  initForm(): void {
    this.taskForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      dueDate: ['', Validators.required],
      dueTime: ['', Validators.required],
      scheduledStart: ['', Validators.required],
      scheduledStartTime: ['', Validators.required],
      priority: ['', Validators.required],  // Priority dropdown
      recurring: [false],  // Recurring checkbox
      notifyBeforeStart: [false]  // Push notifications checkbox
    });
  }

  // Populate form with task values
  populateForm(): void {
    console.log('Setting values from task:', this.task?.task);

    const title = this.task?.task?.title || 'Default Title';
    const description = this.task?.task?.description || 'Default Description';

    const dueDate = this.task?.task?.dueDate ? new Date(this.task.task.dueDate) : null;
    const localDueDate = dueDate ? this.convertUTCToLocalDate(dueDate) : '';
    const dueTime = dueDate ? this.convertUTCToLocalTime(dueDate) : '';

    const scheduledStart = this.task?.task?.scheduledStart ? new Date(this.task.task.scheduledStart) : null;
    const localScheduledStart = scheduledStart ? this.convertUTCToLocalDate(scheduledStart) : '';
    const scheduledStartTime = scheduledStart ? this.convertUTCToLocalTime(scheduledStart) : '';

    const priority = this.task?.task?.priority || '';
    const recurring = this.task?.task?.recurring || false;
    const notifyBeforeStart = this.task?.task?.notifyBeforeStart || false;

    this.taskForm.setValue({
      title: title,
      description: description,
      dueDate: localDueDate,
      dueTime: dueTime,
      scheduledStart: localScheduledStart,
      scheduledStartTime: scheduledStartTime,
      priority: priority,
      recurring: recurring,
      notifyBeforeStart: notifyBeforeStart
    });

    console.log('Form values after setting:', this.taskForm.value);
  }

  // Convert UTC date to local date (YYYY-MM-DD)
  convertUTCToLocalDate(date: Date): string {
    return date.toLocaleDateString('en-CA');  // ISO 8601 format (YYYY-MM-DD)
  }

  // Convert UTC time to local time (HH:MM)
  convertUTCToLocalTime(date: Date): string {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

// Combine local date and time and convert to UTC
combineLocalDateAndTimeToUTC(date: string, time: string): string {
  const [hours, minutes] = time.split(':');
  const localDate = new Date(`${date}T${hours}:${minutes}:00`);

  // Convert the local time to UTC, ensuring the backend only receives a single UTC adjustment.
  return new Date(localDate).toISOString();  // Avoid unnecessary timezone manipulation
}

  // Update task method
  updateTask(): void {
    if (this.taskForm.valid) {
      const formValues = this.taskForm.value;
  
      // Combine date and time fields into UTC for due date and scheduled start
      const dueDateTime = this.combineLocalDateAndTimeToUTC(formValues.dueDate, formValues.dueTime);
      const scheduledStartDateTime = this.combineLocalDateAndTimeToUTC(formValues.scheduledStart, formValues.scheduledStartTime);
  
      // Log the dates to verify the correct UTC values are being sent
      console.log('Due DateTime (UTC):', dueDateTime);
      console.log('Scheduled Start DateTime (UTC):', scheduledStartDateTime);
  
      // Update the task object
      this.task = {
        ...this.task.task,
        title: formValues.title,
        description: formValues.description,
        dueDate: dueDateTime,
        scheduledStart: scheduledStartDateTime,
        priority: formValues.priority,
        recurring: formValues.recurring,
        notifyBeforeStart: formValues.notifyBeforeStart,
        notificationSent: false
      };
  
      if (!this.task.id) {
        console.error('Task ID is undefined!');
        return; // Prevent the update if the task.id is missing
      }
  
      this.taskService.updateTask(this.task).subscribe(
        (response) => {
          console.log('Task updated successfully:', response);
          this.openSuccessDialog();  // Open the success dialog
        },
        (error) => {
          console.error('Error updating task:', error);
        }
      );
    }
  }
  
  // Open success dialog after task update
  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { title: 'Success', message: 'Task updated successfully!' }
    }).afterClosed().subscribe(() => {
      this.router.navigate(['/home', this.task.userId, 'tasks']);
    });
  }
}