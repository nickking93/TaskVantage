import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
  tasksPerPage: number = 10;  // This will be updated based on the paginator settings
  totalPages: number = 0;

  constructor(private taskService: TaskService, private dialog: MatDialog) {}

  ngOnInit(): void {
    console.log('TasksComponent initialized with userId:', this.userId);
    this.loadTasks();  
  }

  loadTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      this.tasks = tasks;
      this.filterTasks(this.selectedFilter);
      // console.log('Fetched tasks:', this.tasks);
    });
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.loadTasks());
  }

  editTask(task: Task): void {
    const updatedTask: Partial<Task> = {
      title: task.title,
      description: task.description,
    };

    this.taskService.editTask(task.id!, updatedTask).subscribe(
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
    const nowLocal = new Date(now.getTime() - now.getTimezoneOffset() * 60000); // Convert UTC to local
  
    switch (filter) {
      case 'today':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          const dueDateLocal = dueDate ? new Date(dueDate.getTime() - dueDate.getTimezoneOffset() * 60000) : null;
          return dueDateLocal && dueDateLocal.toDateString() === nowLocal.toDateString() && task.status !== 'Complete';
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          const dueDateLocal = dueDate ? new Date(dueDate.getTime() - dueDate.getTimezoneOffset() * 60000) : null;
          return dueDateLocal && dueDateLocal < nowLocal && task.status !== 'Complete';
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
  
    this.currentPage = 1; // Reset to first page after filtering
    this.updatePagination();
  }  

  setPage(event: PageEvent): void {
    this.currentPage = event.pageIndex + 1; // Angular Material uses zero-based indexing
    this.tasksPerPage = event.pageSize; // Update the tasksPerPage based on the selected page size
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
