import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Task } from '../models/task.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';  
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private tasksUrl = `${environment.apiUrl}/api/tasks`; // API URL for tasks

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  isCompletedStatus(status?: string | null): boolean {
    if (!status) {
      return false;
    }

    const normalized = status.toLowerCase();
    return normalized === 'completed' || normalized === 'complete';
  }

  // Method to fetch tasks for a specific user and handle filtering
  fetchTasks(userId: string, filterCallback: (tasks: Task[]) => void): void {
    this.getTasks(userId).subscribe(
      (tasks: Task[]) => {
        filterCallback(tasks);
      },
      () => {
        this.snackBar.open('Failed to load tasks', 'Close', { duration: 3000 });
      }
    );
  }

  // Method to fetch completed task count for a user
  getCompletedTaskCount(userId: string): Observable<number> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/user/${userId}/completed-tasks-count`;
    return this.http.get<{ completedTaskCount: number }>(url, { headers, responseType: 'json' }).pipe(
      map(response => response.completedTaskCount),
      catchError(this.handleError)
    );
  }

  // Method to fetch a single task by ID
  getTaskById(taskId: string): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}`;
    return this.http.get<Task>(url, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Method to create a new task
  createTask(task: Task): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    return this.http.post<Task>(this.tasksUrl, task, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Method to fetch task summary for a specific user
  getTaskSummary(userId: string): Observable<any> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/summary/${userId}`;
    return this.http.get<any>(url, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Method to fetch tasks for a specific user
  getTasks(userId: string): Observable<Task[]> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/user/${userId}`;
    return this.http.get<Task[]>(url, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Handle error response
  private handleError(error: HttpErrorResponse): Observable<never> {
    return throwError(() => new Error('TaskService Error: ' + error.message));
  }

  // Method to start a task by updating its status and start date
  startTask(task: Task): void {
    if (!task || !task.id) {
      return;
    }

    const taskId = task.id;
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}/start`;

    task.status = 'In Progress';
    task.startDate = new Date().toISOString();

    this.http.put<Task>(url, task, { headers, responseType: 'json' }).subscribe(
      () => {
        // Task started successfully
      },
      () => {
        this.snackBar.open('Failed to start task', 'Close', { duration: 3000 });
      }
    );
  }

  // Method to handle starting a task and reloading the data after success
  handleStartTask(task: Task, callback: () => void): void {
    task.status = 'In Progress';
    task.startDate = new Date().toISOString();

    this.updateTask(task).subscribe(
      (response: Task | null) => {
        if (response) {
          callback();
        }
      },
      () => {
        this.snackBar.open('Failed to start task', 'Close', { duration: 3000 });
      }
    );
  }

  // Method to update an existing task
  updateTask(task: Task): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${task.id}`;
    return this.http.put<Task>(url, task, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Method to update only the groupId of a task (for drag-and-drop operations)
  updateTaskGroup(taskId: string, groupId: number | null): Observable<any> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}/group`;
    return this.http.patch(url, { groupId }, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Method to mark a task as completed
  markTaskAsCompleted(taskId: string): Observable<void> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}/complete`;
    return this.http.patch<void>(url, null, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Shared method to handle the completion of a task and show a dialog
  handleMarkTaskAsCompleted(task: Task, refreshCallback: () => void): void {
    this.markTaskAsCompleted(task.id!).subscribe(
      () => {
        this.dialog.open(SuccessDialogComponent, {
          data: {
            title: 'Success',
            message: 'Task has been marked as completed!'
          }
        }).afterClosed().subscribe(() => {
          refreshCallback();
        });
      },
      () => {
        this.snackBar.open('Failed to complete task', 'Close', { duration: 3000 });
      }
    );
  }

  // Method to delete a task
  deleteTask(taskId: string): Observable<void> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}`;
    return this.http.delete<void>(url, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }

  // Save FCM token
  saveToken(token: string): Observable<any> {
    const url = `${this.tasksUrl}/save-token`;  
    const headers = this.authService.getAuthHeaders();
    return this.http.post(url, { token }, { headers, responseType: 'json' }).pipe(
      catchError(this.handleError)
    );
  }
}
