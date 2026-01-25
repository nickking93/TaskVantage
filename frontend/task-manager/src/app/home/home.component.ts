import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { FirebaseMessagingService } from '../services/firebase-messaging.service';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TasksComponent } from '../tasks/tasks.component';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { Chart, registerables } from 'chart.js';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { getApp } from 'firebase/app';
import { RecommendationsComponent } from '../recommendations/recommendations.component';
import { HomeModule } from './home.module';
import { TaskCalendarComponent } from '../task-calendar/task-calendar.component';

interface User {
  id: number | string;
  username: string;
}

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.css'],
    imports: [
        FormsModule,
        RouterModule,
        CommonModule,
        MatFormFieldModule,
        MatInputModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatSelectModule,
        MatCheckboxModule,
        MatButtonModule,
        MatIconModule,
        MatDialogModule,
        TasksComponent,
        RecommendationsComponent,
        TaskCalendarComponent
    ]
})
export class HomeComponent implements OnInit, OnDestroy {
  username: string = '';
  userId: string = '';
  isSidebarCollapsed: boolean = false;
  totalTasks: number = 0;
  totalSubtasks: number = 0;
  pastDeadlineTasks: number = 0;
  completedTasksThisMonth: number = 0;
  totalTasksThisMonth: number = 0;
  tasksDueToday: Task[] = [];
  recentCompletedTasks: { title: string, timeAgo: string, lastModifiedDate: string }[] = [];
  taskStatusChart: Chart | undefined;
  private routeSub: Subscription = new Subscription();

  // PWA Install Prompt fields
  deferredPrompt: any;
  canPromptPwaInstall: boolean = false;

  constructor(
    private authService: AuthService,
    private taskService: TaskService,
    private firebaseMessagingService: FirebaseMessagingService,
    public router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog
  ) {
    Chart.register(...registerables);

    window.addEventListener('beforeinstallprompt', (event: any) => {
      event.preventDefault();
      this.deferredPrompt = event;
      this.canPromptPwaInstall = true;
    });

    window.addEventListener('appinstalled', () => {
      this.canPromptPwaInstall = false;
    });
  }

  async ngOnInit(): Promise<void> {
    try {
      const userResponse = await this.authService.getUserDetails().toPromise() as User | null;

      if (!userResponse || !this.isValidUser(userResponse)) {
        await this.logout();
        return;
      }

      this.userId = String(userResponse.id);
      this.username = userResponse.username;

      // Store userId for services that need it (e.g., GoogleAuthService)
      localStorage.setItem('google_auth_user_id', this.userId);

      // Ensure Firebase messaging is initialized
      try {
        await this.firebaseMessagingService.initialize();
      } catch (error) {
      }

      this.reloadData();
    } catch (err) {
      await this.router.navigate(['/login']);
    }

    this.setupRouteListener();
  }

  private isValidUser(user: any): user is User {
    return user 
      && typeof user.id === 'number'
      && typeof user.username === 'string';
  }

  private setupRouteListener(): void {
    this.routeSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        if (event.urlAfterRedirects === '/home') {
          this.reloadData();
          this.resetPwaPrompt();
        }
      });
  }

  resetPwaPrompt(): void {
    this.deferredPrompt = null;
    this.canPromptPwaInstall = false;
  }

  promptPwaInstall(event: Event): void {
    event.preventDefault();

    if (this.deferredPrompt) {
      this.deferredPrompt.prompt();
      this.deferredPrompt.userChoice.then((choiceResult: { outcome: string }) => {
        this.deferredPrompt = null;
        this.canPromptPwaInstall = false;
      });
    } else {
      this.dialog.open(SuccessDialogComponent, {
        width: '300px',
        data: {
          title: 'Unavailable',
          message: 'Install prompt is not available. Please check if TaskVantage is already installed.'
        }
      });
    }
  }

  reloadData(): void {
    this.fetchTaskSummary();
    this.fetchTasksDueToday();
    this.fetchRecentCompletedTasks();
    this.loadWeeklyTaskStatusChart();
  }

  ngOnDestroy(): void {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  fetchTasksDueToday(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const now = new Date();
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
      const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
  
      this.tasksDueToday = tasks.filter(task => {
        if (!task.dueDate || this.taskService.isCompletedStatus(task.status)) return false;
  
        const dueDate = new Date(task.dueDate);
        return dueDate >= startOfToday && dueDate <= endOfToday;
      });
    });
  }   

  fetchTaskSummary(): void {
    this.taskService.getTaskSummary(this.userId).subscribe(summary => {
      this.totalTasks = summary.totalTasks;
      this.totalSubtasks = summary.totalSubtasks;
      this.pastDeadlineTasks = summary.pastDeadlineTasks;
      this.completedTasksThisMonth = summary.completedTasksThisMonth;
      this.totalTasksThisMonth = summary.totalTasksThisMonth;
    });
  }

  fetchRecentCompletedTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const completedTasks = tasks
        .filter(task => this.taskService.isCompletedStatus(task.status))
        .sort((a, b) => new Date(b.lastModifiedDate!).getTime() - new Date(a.lastModifiedDate!).getTime())
        .slice(0, 10);

      this.recentCompletedTasks = completedTasks.map(task => ({
        title: task.title,
        timeAgo: this.calculateTimeAgo(new Date(task.lastModifiedDate!)),
        lastModifiedDate: this.convertUTCToLocal(task.lastModifiedDate!)
      }));
    });
  }

  convertUTCToLocal(dateTimeString: string | undefined): string {
    if (!dateTimeString) {
      return ''; 
    }
    const date = new Date(dateTimeString + 'Z'); 
    return date.toLocaleString(); 
  }

  calculateTimeAgo(lastModifiedDate: Date): string {
    const now = new Date();
    const diff = now.getTime() - lastModifiedDate.getTime();
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (seconds < 60) {
      return seconds === 1 ? '1 second ago' : `${seconds} seconds ago`;
    } else if (minutes < 60) {
      return minutes === 1 ? '1 minute ago' : `${minutes} minutes ago`;
    } else if (hours < 24) {
      return hours === 1 ? '1 hour ago' : `${hours} hours ago`;
    } else {
      return days === 1 ? '1 day ago' : `${days} days ago`;
    }    
  }

  loadWeeklyTaskStatusChart(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const startOfWeek = this.startOfWeek();
      const endOfWeek = this.endOfWeek();

      const completed = tasks.filter(
        (task) => this.taskService.isCompletedStatus(task.status) && task.dueDate
          && new Date(task.dueDate).getTime() >= startOfWeek
          && new Date(task.dueDate).getTime() <= endOfWeek
      );

      const inProgress = tasks.filter(
        (task) => task.status === 'In Progress' && task.dueDate
          && new Date(task.dueDate).getTime() >= startOfWeek
          && new Date(task.dueDate).getTime() <= endOfWeek
      );

      const pending = tasks.filter(
        (task) => task.status === 'Pending' && task.dueDate
          && new Date(task.dueDate).getTime() >= startOfWeek
          && new Date(task.dueDate).getTime() <= endOfWeek
      );

      const totalTasksForWeek = completed.length + inProgress.length + pending.length;

      if (this.taskStatusChart) {
        this.taskStatusChart.destroy();
      }

      this.taskStatusChart = new Chart('taskStatusChart', {
        type: 'bar',
        data: {
          labels: ['Completed', 'In Progress', 'Pending'],
          datasets: [
            {
              data: [completed.length, inProgress.length, pending.length],
              backgroundColor: ['#4caf50', '#ffeb3b', '#f44336'],
            },
          ],
        },
        options: {
          scales: {
            y: {
              beginAtZero: true,
              max: totalTasksForWeek,
            },
          },
          plugins: {
            legend: {
              display: false,
            },
          },
        },
      });
    });
  }

  endOfWeek(): number {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const end = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate() + (6 - dayOfWeek)
    );
    return end.getTime();
  }

  startOfWeek(): number {
    const now = new Date();
    const dayOfWeek = now.getDay();
    const start = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate() - dayOfWeek
    );
    return start.getTime();
  }

  async logout(): Promise<void> {
    try {
      if (this.username) {
        const authToken = localStorage.getItem('jwtToken');
        if (authToken) {
          await this.firebaseMessagingService
            .clearTokenFromServer(this.username, authToken)
            .toPromise();
        }
      }
      
      await this.authService.logout().toPromise();
      await this.router.navigate(['/login']);
    } catch (error) {
      await this.router.navigate(['/login']);
    }
  }

  isActive(route: string): boolean {
    // Exact match for /home, startsWith for child routes
    if (route === '/home') {
      return this.router.url === '/home';
    }
    return this.router.url === route || this.router.url.startsWith(route + '/');
  }

  get currentUrl(): string {
    return this.router.url;
  }

  markTaskAsCompleted(task: Task): void {
    this.taskService.handleMarkTaskAsCompleted(task, () => this.reloadData());
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.reloadData());
  }  

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}
