import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Task } from '../models/task.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';  // Import environment settings

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private tasksUrl = `${environment.apiUrl}/api/tasks`;  // URL to the backend API for tasks

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Method to create a new task
  createTask(task: Task): Observable<Task> {
    const headers = this.authService.getAuthHeaders();
    const token = headers.get('Authorization');
  
    // Log the token for debugging
    console.log('JWT Token being sent:', token);
  
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

  // Handle error response
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('TaskService Error:', error.message);
    return throwError(() => new Error('TaskService Error: ' + error.message));
  }
}
