import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';

interface SimilarTask {
  task: {
    id: number;
    title: string;
    description: string;
    status: string;
    priority: string;
    dueDate: string;
    completionDateTime?: string;
  };
  similarityScore: number;
  reason: string;
}

interface SimilarTasksResponse {
  similarTasks: SimilarTask[];
  count: number;
}

@Component({
  selector: 'app-similar-tasks-dialog',
  templateUrl: './similar-tasks-dialog.component.html',
  styleUrls: ['./similar-tasks-dialog.component.css'],
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ]
})
export class SimilarTasksDialogComponent implements OnInit {
  loading = true;
  error: string | null = null;
  similarTasks: SimilarTask[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { taskId: number; taskTitle: string; userId: string },
    private dialogRef: MatDialogRef<SimilarTasksDialogComponent>,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.fetchSimilarTasks();
  }

  fetchSimilarTasks(): void {
    this.loading = true;
    const token = localStorage.getItem('jwtToken');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.http.get<SimilarTasksResponse>(
      `${environment.apiUrl}/api/tasks/${this.data.taskId}/similar?userId=${this.data.userId}&limit=3`,
      { headers }
    ).subscribe({
      next: (response) => {
        this.similarTasks = response.similarTasks || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Error fetching similar tasks:', err);
        this.error = 'Failed to load similar tasks';
        this.loading = false;
      }
    });
  }

  viewTask(taskId: number): void {
    this.dialogRef.close();
    this.router.navigate(['/home/update-task', taskId.toString()]);
  }

  close(): void {
    this.dialogRef.close();
  }

  getStatusColor(status: string): string {
    switch (status?.toLowerCase()) {
      case 'completed':
        return '#4caf50';
      case 'in progress':
        return '#ff9800';
      case 'pending':
        return '#f44336';
      default:
        return '#9e9e9e';
    }
  }

  getPriorityColor(priority: string): string {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
        return '#f44336';
      case 'MEDIUM':
        return '#ff9800';
      case 'LOW':
        return '#4caf50';
      default:
        return '#9e9e9e';
    }
  }
}
