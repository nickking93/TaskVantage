import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';

import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';

@Component({
    selector: 'app-update-task',
    templateUrl: './update-task.component.html',
    styleUrls: ['./update-task.component.css'],
    imports: [
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
  taskData: any;
  dueDateInvalid: boolean = false;
  scheduledStartDateInvalid: boolean = false;
  formInvalid: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private router: Router,
    private fb: FormBuilder,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.initForm();
    
    const taskId = this.route.snapshot.paramMap.get('taskId');
    if (taskId) {
      this.taskService.getTaskById(taskId).subscribe((response: any) => {
        this.taskData = response;
        this.populateForm();
      });
    }
  }

  initForm(): void {
    this.taskForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      dueDate: ['', Validators.required],
      dueTime: [{ value: '', disabled: false }, Validators.required],
      scheduledStart: ['', Validators.required],
      scheduledStartTime: [{ value: '', disabled: false }, Validators.required],
      priority: ['', Validators.required],
      recurring: [false],
      notifyBeforeStart: [false],
      isAllDay: [false]
    });

    this.taskForm.get('isAllDay')?.valueChanges.subscribe(isAllDay => {
      this.handleAllDayChange(isAllDay);
    });
  }

  handleAllDayChange(isAllDay: boolean): void {
    const dueTimeControl = this.taskForm.get('dueTime');
    const scheduledStartTimeControl = this.taskForm.get('scheduledStartTime');
    
    if (isAllDay) {
      dueTimeControl?.disable();
      scheduledStartTimeControl?.disable();
      dueTimeControl?.setValue('23:59');
      scheduledStartTimeControl?.setValue('00:00');
    } else {
      dueTimeControl?.enable();
      scheduledStartTimeControl?.enable();
      
      if (!dueTimeControl?.value) {
        dueTimeControl?.setValue('23:59');
      }
      if (!scheduledStartTimeControl?.value) {
        scheduledStartTimeControl?.setValue('00:00');
      }
    }
    
    this.validateDates();
  }

  populateForm(): void {
    if (!this.taskData?.task) {
      return;
    }

    const taskDetails = this.taskData.task;
    const isAllDay = taskDetails.allDay || false;

    const dueDate = taskDetails.dueDate;
    const scheduledStart = taskDetails.scheduledStart;
    const priority = (taskDetails.priority || 'Medium').charAt(0).toUpperCase() + 
                    taskDetails.priority?.slice(1).toLowerCase();

    const formValues = {
      title: taskDetails.title,
      description: taskDetails.description,
      dueDate: dueDate ? new Date(dueDate) : null,
      scheduledStart: scheduledStart ? new Date(scheduledStart) : null,
      priority: priority,
      recurring: taskDetails.recurring || false,
      notifyBeforeStart: taskDetails.notifyBeforeStart || false,
      isAllDay: isAllDay
    };

    this.taskForm.patchValue(formValues);

    if (!isAllDay) {
      const dueDateObj = dueDate ? new Date(dueDate) : null;
      const scheduledStartObj = scheduledStart ? new Date(scheduledStart) : null;

      this.taskForm.patchValue({
        dueTime: dueDateObj ? this.convertUTCToLocalTime(dueDateObj) : '23:59',
        scheduledStartTime: scheduledStartObj ? this.convertUTCToLocalTime(scheduledStartObj) : '00:00'
      });
    }

    this.handleAllDayChange(isAllDay);
  }

  validateDates(): void {
    const now = new Date();
    const isAllDay = this.taskForm.get('isAllDay')?.value;

    // Reset validation flags
    this.scheduledStartDateInvalid = false;
    this.dueDateInvalid = false;
    
    const scheduledStartDate = this.taskForm.get('scheduledStart')?.value;
    const dueDate = this.taskForm.get('dueDate')?.value;

    if (!scheduledStartDate || !dueDate) {
      return;
    }

    if (isAllDay) {
      const startDate = new Date(scheduledStartDate);
      startDate.setHours(0, 0, 0, 0);
      
      const endDate = new Date(dueDate);
      endDate.setHours(23, 59, 59, 999);
      
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      this.scheduledStartDateInvalid = startDate < today;
      this.dueDateInvalid = endDate < today || endDate < startDate;
    } else {
      const startTime = this.taskForm.get('scheduledStartTime')?.value;
      const dueTime = this.taskForm.get('dueTime')?.value;

      if (!startTime || !dueTime) {
        return;
      }
      
      const [startHours, startMinutes] = startTime.split(':').map(Number);
      const [dueHours, dueMinutes] = dueTime.split(':').map(Number);
      
      const startDateTime = new Date(scheduledStartDate);
      startDateTime.setHours(startHours, startMinutes, 0, 0);
      
      const dueDateTime = new Date(dueDate);
      dueDateTime.setHours(dueHours, dueMinutes, 0, 0);
      
      this.scheduledStartDateInvalid = startDateTime < now;
      this.dueDateInvalid = dueDateTime < now || dueDateTime <= startDateTime;
    }

    this.formInvalid = this.dueDateInvalid || this.scheduledStartDateInvalid;
  }

  convertUTCToLocalDate(date: Date): string {
    return date.toLocaleDateString('en-CA');
  }

  convertUTCToLocalTime(date: Date): string {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  updateTask(): void {
    if (this.taskForm.valid && !this.formInvalid) {
      const formValues = this.taskForm.getRawValue();
      const isAllDay = formValues.isAllDay;
  
      let dueDateTime: string;
      let scheduledStartDateTime: string;
  
      if (isAllDay) {
        // For all-day tasks, set times to start/end of day
        const dueDate = new Date(formValues.dueDate);
        dueDate.setHours(23, 59, 59);
        dueDateTime = dueDate.toISOString();
  
        const scheduledStart = new Date(formValues.scheduledStart);
        scheduledStart.setHours(0, 0, 0);
        scheduledStartDateTime = scheduledStart.toISOString();
      } else {
        // Regular tasks with specific times
        const dueDateObj = new Date(formValues.dueDate);
        const [dueHours, dueMinutes] = formValues.dueTime.split(':').map(Number);
        dueDateObj.setHours(dueHours, dueMinutes, 0);
        dueDateTime = dueDateObj.toISOString();
  
        const scheduledStartObj = new Date(formValues.scheduledStart);
        const [startHours, startMinutes] = formValues.scheduledStartTime.split(':').map(Number);
        scheduledStartObj.setHours(startHours, startMinutes, 0);
        scheduledStartDateTime = scheduledStartObj.toISOString();
      }
  
      const updatedTask = {
        ...this.taskData.task,
        title: formValues.title,
        description: formValues.description,
        dueDate: dueDateTime,
        scheduledStart: scheduledStartDateTime,
        priority: formValues.priority.toUpperCase(),
        recurring: formValues.recurring,
        notifyBeforeStart: formValues.notifyBeforeStart,
        isAllDay: formValues.isAllDay
      };

      if (!updatedTask.id) {
        return;
      }
  
      this.taskService.updateTask(updatedTask).subscribe(
        (response) => {
          this.openSuccessDialog();
        },
        (error) => {
          this.dialog.open(SuccessDialogComponent, {
            width: '300px',
            data: { title: 'Error', message: 'Failed to update task. Please try again.' }
          });
        }
      );
    }
  }

  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { title: 'Success', message: 'Task updated successfully!' }
    }).afterClosed().subscribe(() => {
      this.router.navigate(['/home/tasks']);
    });
  }
}