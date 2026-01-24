import { Component, OnInit, Input } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TaskRecommendation, RecommendationResponse } from './recommendations.interface';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-recommendations',
  templateUrl: './recommendations.component.html',
  styleUrls: ['./recommendations.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ]
})
export class RecommendationsComponent implements OnInit {
  @Input() userId!: string; 
  recommendations: TaskRecommendation[] = [];
  loading = true;
  error: string | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    if (this.userId) {
      this.fetchRecommendations();
    } else {
      this.error = 'User ID is missing';
      this.loading = false;
    }
  }

  scheduleTask(taskRecommendation: TaskRecommendation): void {
    this.router.navigate(['/home', this.userId, 'update-task', taskRecommendation.id.toString()]);
  }

  fetchRecommendations(): void {
    this.loading = true;
    this.http.get<RecommendationResponse>(`${environment.apiUrl}/api/recommendations/user/${this.userId}`)
      .subscribe({
        next: (response) => {
          if (response.status === 'success') {
            this.recommendations = response.recommendations || [];
          } else {
            this.error = response.message;
          }
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load recommendations';
          this.loading = false;
        }
      });
  }

  getRecommendationIcon(recommendedBy: string): string {
    switch (recommendedBy) {
      case 'WEEKDAY_MATCH':
      case 'SAME_DAY':
        return 'event';
      case 'SIMILAR_CONTENT':
        return 'thumb_up';
      case 'POPULAR':
        return 'trending_up';
      default:
        return 'schedule';
    }
  }
}
