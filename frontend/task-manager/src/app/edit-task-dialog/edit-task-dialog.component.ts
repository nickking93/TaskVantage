import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';

@Component({
  selector: 'app-edit-task-dialog',
  standalone: true,
  templateUrl: './edit-task-dialog.component.html',
  styleUrls: ['./edit-task-dialog.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule
  ]
})
export class EditTaskDialogComponent implements OnInit {
  taskForm!: FormGroup;
  dueDateInvalid: boolean = false;
  scheduledStartDateInvalid: boolean = false;
  formInvalid: boolean = false;
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private taskService: TaskService,
    private dialogRef: MatDialogRef<EditTaskDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { task: Task }
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.populateForm();
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
    const task = this.data.task;
    if (!task) return;

    const isAllDay = task.isAllDay || false;
    const priorityBase = task.priority?.trim() ? task.priority : 'Medium';
    const priority = priorityBase.charAt(0).toUpperCase() +
                    priorityBase.slice(1).toLowerCase();

    const formValues = {
      title: task.title,
      description: task.description,
      dueDate: task.dueDate ? new Date(task.dueDate) : null,
      scheduledStart: task.scheduledStart ? new Date(task.scheduledStart) : null,
      priority: priority,
      recurring: task.recurring || false,
      notifyBeforeStart: task.notifyBeforeStart || false,
      isAllDay: isAllDay
    };

    this.taskForm.patchValue(formValues);

    if (!isAllDay) {
      const dueDateObj = task.dueDate ? new Date(task.dueDate) : null;
      const scheduledStartObj = task.scheduledStart ? new Date(task.scheduledStart) : null;

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

  convertUTCToLocalTime(date: Date): string {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  }

  updateTask(): void {
    if (this.taskForm.valid && !this.formInvalid) {
      this.isLoading = true;
      this.errorMessage = '';

      const formValues = this.taskForm.getRawValue();
      const isAllDay = formValues.isAllDay;

      let dueDateTime: string;
      let scheduledStartDateTime: string;

      if (isAllDay) {
        const dueDate = new Date(formValues.dueDate);
        dueDate.setHours(23, 59, 59);
        dueDateTime = dueDate.toISOString();

        const scheduledStart = new Date(formValues.scheduledStart);
        scheduledStart.setHours(0, 0, 0);
        scheduledStartDateTime = scheduledStart.toISOString();
      } else {
        const dueDateObj = new Date(formValues.dueDate);
        const [dueHours, dueMinutes] = formValues.dueTime.split(':').map(Number);
        dueDateObj.setHours(dueHours, dueMinutes, 0);
        dueDateTime = dueDateObj.toISOString();

        const scheduledStartObj = new Date(formValues.scheduledStart);
        const [startHours, startMinutes] = formValues.scheduledStartTime.split(':').map(Number);
        scheduledStartObj.setHours(startHours, startMinutes, 0);
        scheduledStartDateTime = scheduledStartObj.toISOString();
      }

      const updatedTask: Task = {
        ...this.data.task,
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
        this.isLoading = false;
        return;
      }

      this.taskService.updateTask(updatedTask).subscribe({
        next: () => {
          this.isLoading = false;
          this.dialogRef.close({ updated: true, task: updatedTask });
        },
        error: () => {
          this.isLoading = false;
          this.errorMessage = 'Failed to update task. Please try again.';
        }
      });
    }
  }

  cancel(): void {
    this.dialogRef.close({ updated: false });
  }
}
