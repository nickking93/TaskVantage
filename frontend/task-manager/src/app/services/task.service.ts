import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Task } from '../models/task.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';  
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private tasksUrl = `${environment.apiUrl}/api/tasks`;  

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private dialog: MatDialog
  ) {}

  // Method to fetch tasks for a specific user and handle filtering
  fetchTasks(userId: string, filterCallback: (tasks: Task[]) => void): void {
    this.getTasks(userId).subscribe(
      (tasks: Task[]) => {
        filterCallback(tasks);
      },
      (error) => {
        console.error('Failed to fetch tasks:', error);
      }
    );
  }

  // Method to create a new task
  createTask(task: Task): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    return this.http.post<Task>(this.tasksUrl, task, { headers }).pipe(
      map(response => {
        console.log('Task created successfully:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Method to fetch task summary for a specific user
  getTaskSummary(userId: string): Observable<any> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/summary/${userId}`;
    return this.http.get<any>(url, { headers }).pipe(
      map(response => {
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Method to fetch tasks for a specific user
  getTasks(userId: string): Observable<Task[]> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/user/${userId}`;
    return this.http.get<Task[]>(url, { headers }).pipe(
      map(response => {
        console.log('Fetched tasks:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Handle error response
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('TaskService Error:', error.message);
    return throwError(() => new Error('TaskService Error: ' + error.message));
  }

  // Method to start a task (change status to 'In Progress')
  startTask(taskId: string): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}/start`;

    return this.http.patch<Task>(url, null, { headers }).pipe(
      map(response => {
        console.log('Task started successfully:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Shared method to handle the starting of a task and show a dialog
  handleStartTask(task: Task, refreshCallback: () => void): void {
    this.startTask(task.id!).subscribe(
      () => {
        this.dialog.open(SuccessDialogComponent, {
          data: {
            message: 'Task has been started successfully!'
          }
        }).afterClosed().subscribe(() => {
          refreshCallback(); // Call the refresh function passed by the component
        });
      },
      (error) => {
        console.error('Failed to start task:', error);
      }
    );
  }

  // Method to update/edit a task
  editTask(taskId: string, updatedTask: Partial<Task>): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}`;

    return this.http.put<Task>(url, updatedTask, { headers }).pipe(
      map(response => {
        console.log('Task updated successfully:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Method to mark a task as completed
  markTaskAsCompleted(taskId: string): Observable<void> {
    const headers = new HttpHeaders().set('Authorization', 'Bearer ' + localStorage.getItem('token'));
    const url = `${this.tasksUrl}/${taskId}/complete`;

    return this.http.patch<void>(url, null, { headers }).pipe(
      map(response => {
        console.log('Task marked as completed:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }

  // Shared method to handle the completion of a task and show a dialog
  handleMarkTaskAsCompleted(task: Task, refreshCallback: () => void): void {
    this.markTaskAsCompleted(task.id!).subscribe(
      () => {
        this.dialog.open(SuccessDialogComponent, {
          data: {
            message: 'Task has been marked as completed!'
          }
        }).afterClosed().subscribe(() => {
          refreshCallback(); // Call the refresh function passed by the component
        });
      },
      (error) => {
        console.error('Failed to mark task as completed:', error);
      }
    );
  }

  // Method to delete a task
  deleteTask(taskId: string): Observable<void> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}`;

    return this.http.delete<void>(url, { headers }).pipe(
      map(response => {
        console.log('Task deleted successfully');
        return response;
      }),
      catchError(this.handleError)
    );
  }
}
