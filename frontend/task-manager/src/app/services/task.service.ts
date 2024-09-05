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
        filterCallback(tasks); // Pass the tasks directly without conversions
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

  startTask(task: Task): void {
    if (!task || !task.id) {
      console.error('Task or Task ID is undefined. Cannot start task.');
      return;
    }
  
    const taskId = task.id;
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${taskId}/start`;
  
    // Updating task status and start date before sending
    task.status = 'In Progress';
    task.startDate = new Date().toISOString();
  
    // Send the full Task object as the body for the PUT request
    this.http.put<Task>(url, task, { headers }).subscribe(
      (response) => {
        console.log('Task started successfully:', response);
      },
      (error) => {
        if (error.status === 400) {
          console.error('Bad Request: ', error.error.message);
        } else if (error.status === 404) {
          console.error('Task not found (404): ', error.error.message);
        } else {
          console.error('An unexpected error occurred:', error);
        }
      }
    );
  }   

  handleStartTask(task: Task, callback: () => void): void {
    // Update task status to 'In Progress' and set the actual start date
    task.status = 'In Progress';
    task.startDate = new Date().toISOString();
  
    this.editTask(task).subscribe(
      (response: Task | null) => {
        if (response) {
          console.log('Task started successfully:', response);
          callback(); // Reload data after successful update
        } else {
          console.error('Failed to start task. Response was null.');
        }
      },
      (error) => {
        console.error('Error starting task:', error);
      }
    );
  }   

  editTask(task: Task): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.tasksUrl}/${task.id}`;
  
    // Use PUT for full update
    return this.http.put<Task>(url, task, { headers }).pipe(
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
            title: 'Success',
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

  // Save FCM token
  saveToken(token: string): Observable<any> {
    const url = `${this.tasksUrl}/save-token`;  
    const headers = this.authService.getAuthHeaders();
    return this.http.post(url, { token }, { headers }).pipe(
      map(response => {
        console.log('Token saved successfully:', response);
        return response;
      }),
      catchError(this.handleError)
    );
  }
}