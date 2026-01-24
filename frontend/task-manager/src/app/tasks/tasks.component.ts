import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { PageEvent } from '@angular/material/paginator';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatPaginatorModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './tasks.component.html',
  styleUrls: ['./tasks.component.css']
})
export class TasksComponent implements OnInit {

  @Input() userId!: string;
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  paginatedTasks: Task[] = [];
  selectedFilter: string = 'today';
  currentPage: number = 1;
  tasksPerPage: number = 10;
  totalPages: number = 0;
  priorities: string[] = ['High', 'Medium', 'Low'];

  constructor(
    private taskService: TaskService,
    private dialog: MatDialog,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // If userId not provided via Input, get it from AuthService
    if (!this.userId) {
      this.authService.getUserDetails().subscribe({
        next: (user) => {
          this.userId = String(user.id);
          console.log('TasksComponent initialized with userId:', this.userId);
          this.loadTasks();
        },
        error: (err) => {
          console.error('Error getting user details:', err);
          this.router.navigate(['/login']);
        }
      });
    } else {
      console.log('TasksComponent initialized with userId:', this.userId);
      this.loadTasks();
    }
  }

  loadTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      this.tasks = tasks.map(task => {
        // Normalize priority to match dropdown options (High, Medium, Low)
        if (task.priority) {
          task.priority = task.priority.charAt(0).toUpperCase() + task.priority.slice(1).toLowerCase();
        }
        return task;
      });
      this.filterTasks(this.selectedFilter);
    });
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.loadTasks());
  }

  // Now renamed to updateTask (previously editTask)
  updateTask(task: Task): void {
    // Navigate to the update-task route with the task ID
    this.router.navigate(['/home/update-task', task.id]);
  }

  // Now renamed to editTask (previously updateTask)
  editTask(task: Task): void {
    const updatedTask: Partial<Task> = {
      title: task.title,
      description: task.description,
      priority: task.priority,
      status: task.status,
      dueDate: task.dueDate
    };

    this.taskService.updateTask(task).subscribe(
      (updatedTask: Task) => {
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) {
          this.tasks[index] = updatedTask;
          this.filterTasks(this.selectedFilter);
          console.log('Task updated:', updatedTask);
        }
      },
      (error) => {
        console.error('Failed to update task:', error);
      }
    );
  }

  deleteTask(task: Task): void {
    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent);

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.taskService.deleteTask(task.id!).subscribe(
          () => {
            this.tasks = this.tasks.filter(t => t.id !== task.id);
            this.filterTasks(this.selectedFilter);
            console.log('Task deleted:', task);
          },
          (error) => {
            console.error('Failed to delete task:', error);
          }
        );
      }
    });
  }

  filterTasks(filter: string): void {
    this.selectedFilter = filter;
  
    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
  
    switch (filter) {
      case 'today':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          if (!dueDate || isNaN(dueDate.getTime()) || this.taskService.isCompletedStatus(task.status)) return false;
          
          return dueDate >= startOfToday && dueDate <= endOfToday;
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && !isNaN(dueDate.getTime()) && dueDate < startOfToday && !this.taskService.isCompletedStatus(task.status);
        });
        break;
      case 'inProgress':
        this.filteredTasks = this.tasks.filter(task => task.status === 'In Progress');
        break;
      case 'pending':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Pending');
        break;
      case 'complete':
        this.filteredTasks = this.tasks.filter(task => this.taskService.isCompletedStatus(task.status));
        break;
      default:
        this.filteredTasks = this.tasks.filter(task => !this.taskService.isCompletedStatus(task.status));
    }
  
    this.currentPage = 1;
    this.updatePagination();
  }  
  
  convertUTCToLocal(date: Date): Date {
    const localDate = new Date(date.getTime() + date.getTimezoneOffset() * 60000);
    return localDate;
  }

  setPage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1;
    this.tasksPerPage = event.pageSize; 
    this.updatePagination();
  }

  updatePagination(): void {
    const start = (this.currentPage - 1) * this.tasksPerPage;
    const end = start + this.tasksPerPage;
    this.paginatedTasks = this.filteredTasks.slice(start, end);
  }

  markTaskAsCompleted(task: Task): void {
    this.taskService.handleMarkTaskAsCompleted(task, () => this.loadTasks());
  }

  saveTaskField(task: Task): void {
    this.taskService.updateTask(task).subscribe(
      (updatedTask: Task) => {
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) {
          this.tasks[index] = updatedTask;
          this.filterTasks(this.selectedFilter);
        }
      },
      (error) => {
        console.error('Failed to update task:', error);
      }
    );
  }

  onDueDateChange(task: Task, event: any): void {
    if (event.value) {
      task.dueDate = event.value.toISOString();
      this.saveTaskField(task);
    }
  }

  getDueDateAsDate(task: Task): Date | null {
    return task.dueDate ? new Date(task.dueDate) : null;
  }
}
