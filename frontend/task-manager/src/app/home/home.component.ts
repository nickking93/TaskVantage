import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { FirebaseMessagingService } from '../services/firebase-messaging.service'; 
import { getMessaging, getToken } from 'firebase/messaging'; 
import { environment } from '../../environments/environment'; 
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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { Chart, registerables } from 'chart.js';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { getApp } from 'firebase/app';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css'],
  standalone: true,
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
    MatDialogModule,
    TasksComponent,
  ]
})
export class HomeComponent implements OnInit, OnDestroy {

  username: string = '';
  userId: string = '';
  isSidebarCollapsed: boolean = false;
  fcmToken: string | null = null; 

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
    private dialog: MatDialog // Inject MatDialog
  ) {
    Chart.register(...registerables);

    // Attach the `beforeinstallprompt` event listener
    window.addEventListener('beforeinstallprompt', (event: any) => {
      event.preventDefault(); // Prevent the default prompt
      this.deferredPrompt = event;
      this.canPromptPwaInstall = true; // Enable the install button
      console.log('beforeinstallprompt event fired');
    });

    // Handle app install success or failure
    window.addEventListener('appinstalled', () => {
      console.log('App installed');
      this.canPromptPwaInstall = false; // Disable install button
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.userId = params['userId'];  
  
      if (typeof this.userId !== 'string' || !this.userId) {
        this.logout(); 
        return;
      }
  
      this.authService.getUserDetails().subscribe(user => {
        if (user.id.toString() === this.userId) {
          this.username = user.username;
          this.reloadData(); 
          this.initializeFirebase();
        } else {
          this.logout();
        }
      }, err => {
        this.router.navigate(['/login']);
      });
    });
  
    this.routeSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        if (event.urlAfterRedirects === `/home/${this.userId}`) {
          this.reloadData();  
          this.resetPwaPrompt(); // Reset prompt state on route change
        }
      });
  }

  // Reset PWA prompt state
  resetPwaPrompt(): void {
    this.deferredPrompt = null;
    this.canPromptPwaInstall = false;
  }

  // PWA install prompt function
  promptPwaInstall(event: Event): void {
    event.preventDefault(); // Prevent the default anchor behavior

    if (this.deferredPrompt) {
      this.deferredPrompt.prompt();
      this.deferredPrompt.userChoice.then((choiceResult: { outcome: string }) => {
        if (choiceResult.outcome === 'accepted') {
          console.log('User accepted the A2HS prompt');
        } else {
          console.log('User dismissed the A2HS prompt');
        }
        this.deferredPrompt = null; // Reset the deferredPrompt after use
        this.canPromptPwaInstall = false; // Disable the install button after prompt
      });
    } else {
      console.log('Install prompt is not available');
      this.dialog.open(SuccessDialogComponent, {
        width: '300px',
        data: { title: 'Unavailable', message: 'Install prompt is not available. Please check if TaskVantage is already installed.' }
      });
    }
  }

  initializeFirebase(): void {
    const app = getApp(); 
    const messaging = getMessaging(app);
  
    // Check notification permission status
    if (Notification.permission === 'denied') {
      console.log('Notifications are blocked. Please enable them in your browser settings.');
      return;
    }
  
    // Request FCM token
    getToken(messaging, { vapidKey: environment.firebaseConfig.vapidKey })
      .then((currentToken) => {
        if (currentToken) {
          this.fcmToken = currentToken;
          const authToken = this.authService.getAuthToken();
          
          // Check if fcmToken and authToken exist
          if (this.fcmToken && authToken) {
            this.firebaseMessagingService.sendTokenToServer(this.userId, this.fcmToken, authToken)
              .subscribe(
                response => console.log('FCM token sent successfully'),
                error => console.error('Error sending FCM token to server:', error)
              );
          }
        } else {
          console.log('No registration token available. Request permission to generate one.');
          this.requestNotificationPermission();
        }
      })
      .catch((err) => {
        if (err.code === 'messaging/permission-blocked') {
          console.warn('Notifications are blocked. Please enable them in your browser settings.');
          alert('Notifications are blocked. Please enable them in your browser settings.');
        } else {
          console.error('An error occurred while retrieving token: ', err);
        }
      });
  }

  requestNotificationPermission(): void {
    Notification.requestPermission().then((permission) => {
      if (permission === 'granted') {
        this.initializeFirebase();
      } else {
        console.log('User denied the notification permission');
      }
    }).catch((error) => {
      console.error('Failed to request notification permission', error);
    });
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
        if (!task.dueDate || task.status === 'Complete') return false;
  
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
        .filter(task => task.status === 'Complete')
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
        (task) => task.status === 'Complete' && task.dueDate
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
          labels: ['Complete', 'In Progress', 'Pending'],
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

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  isActive(route: string): boolean {
    return this.router.url === route;
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