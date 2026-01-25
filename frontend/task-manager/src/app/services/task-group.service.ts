import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TaskGroup } from '../models/task-group.model';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TaskGroupService {
  private groupsUrl = `${environment.apiUrl}/api/task-groups`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getGroups(userId: number): Observable<TaskGroup[]> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.groupsUrl}/user/${userId}`;
    return this.http.get<TaskGroup[]>(url, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  createGroup(group: TaskGroup): Observable<{ group: TaskGroup }> {
    const headers = this.authService.getAuthHeaders();
    return this.http.post<{ group: TaskGroup }>(this.groupsUrl, group, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  updateGroup(group: TaskGroup): Observable<{ group: TaskGroup }> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.groupsUrl}/${group.id}`;
    return this.http.put<{ group: TaskGroup }>(url, group, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  deleteGroup(groupId: number): Observable<{ message: string }> {
    const headers = this.authService.getAuthHeaders();
    const url = `${this.groupsUrl}/${groupId}`;
    return this.http.delete<{ message: string }>(url, { headers }).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    return throwError(() => new Error('TaskGroupService Error: ' + error.message));
  }
}
