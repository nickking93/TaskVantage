import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';  // Import Router
import { FormsModule } from '@angular/forms';
import { TaskService } from '../services/task.service'; 
import { Task } from '../models/task.model'; 
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { PageEvent } from '@angular/material/paginator';
import { MatPaginatorModule } from '@angular/material/paginator';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatPaginatorModule
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

  constructor(private taskService: TaskService, private dialog: MatDialog, private router: Router) {}  // Inject Router

  ngOnInit(): void {
    console.log('TasksComponent initialized with userId:', this.userId);
    this.loadTasks();  
  }

  loadTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      this.tasks = tasks;
      this.filterTasks(this.selectedFilter);
    });
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.loadTasks());
  }

  // Now renamed to updateTask (previously editTask)
  updateTask(task: Task): void {
    // Navigate to the update-task route with the task ID
    this.router.navigate(['/home', this.userId, 'update-task', task.id]);
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
          if (!dueDate || isNaN(dueDate.getTime()) || task.status === 'Complete') return false;
          
          return dueDate >= startOfToday && dueDate <= endOfToday;
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && !isNaN(dueDate.getTime()) && dueDate < startOfToday && task.status !== 'Complete';
        });
        break;
      case 'inProgress':
        this.filteredTasks = this.tasks.filter(task => task.status === 'In Progress');
        break;
      case 'pending':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Pending');
        break;
      case 'complete':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Complete');
        break;
      default:
        this.filteredTasks = this.tasks.filter(task => task.status !== 'Complete');
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
}