import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../services/task.service'; 
import { Task } from '../models/task.model'; 
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ],
  templateUrl: './tasks.component.html',
  styleUrls: ['./tasks.component.css']
})
export class TasksComponent implements OnInit {

  @Input() userId!: string;
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  selectedFilter: string = 'today';

  constructor(private taskService: TaskService, private dialog: MatDialog) {}

  ngOnInit(): void {
    console.log('TasksComponent initialized with userId:', this.userId);
    this.fetchTasks();  
  }

  fetchTasks(): void {
    this.taskService.getTasks(this.userId).subscribe(
      (tasks: Task[]) => {
        this.tasks = tasks;
        this.filterTasks(this.selectedFilter);
        console.log('Fetched tasks:', this.tasks);
      },
      (error) => {
        console.error('Failed to fetch tasks:', error);
      }
    );
  }

  startTask(task: Task): void {
    if (task.status !== 'In Progress') {
      this.taskService.startTask(task.id!).subscribe(
        () => {
          this.dialog.open(SuccessDialogComponent, {
            data: {
              message: 'Task has been started successfully!'
            }
          }).afterClosed().subscribe(() => {
            this.fetchTasks();
          });
        },
        (error) => {
          console.error('Failed to start task:', error);
        }
      );
    }
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
  
    switch (filter) {
      case 'today':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && dueDate.toDateString() === now.toDateString();
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && dueDate < now && task.status !== 'Completed';
        });
        break;
      case 'inProgress':
        this.filteredTasks = this.tasks.filter(task => task.status === 'In Progress');
        break;
      case 'pending':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Pending');
        break;
      case 'completed':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Completed');
        break;
      default:
        this.filteredTasks = this.tasks;
    }
  }
}
