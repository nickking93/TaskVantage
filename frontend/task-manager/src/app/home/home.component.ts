import { Component, OnInit, OnDestroy } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
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
    TasksComponent
  ]
})
export class HomeComponent implements OnInit, OnDestroy {
  username: string = '';
  userId: string = '';
  isAddTaskModalOpen: boolean = false;
  isSidebarCollapsed: boolean = false;
  newTask: Task = {
    title: '',
    description: '',
    dueDate: '',
    priority: 'Medium',
    recurring: false,
  };

  // Variables to hold the summary data
  totalTasks: number = 0;
  totalSubtasks: number = 0;
  pastDeadlineTasks: number = 0;
  completedTasksThisMonth: number = 0;
  totalTasksThisMonth: number = 0;

  // Variable to hold tasks due today
  tasksDueToday: Task[] = [];

  // Variable to hold recent completed tasks for the Activity section
  recentCompletedTasks: { title: string, timeAgo: string }[] = [];

  // Variable to hold chart instance
  taskStatusChart: Chart | undefined;

  // Subscription to track route changes
  private routeSub: Subscription = new Subscription();

  constructor(
    private authService: AuthService,
    private taskService: TaskService,
    public router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog  
  ) {
    Chart.register(...registerables);
  }

  ngOnInit(): void {
    // Listen for changes in the route parameters
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('userId') || '';
      this.authService.getUserDetails().subscribe(user => {
        if (user.id.toString() === this.userId) {
          this.username = user.username;
          this.fetchTaskSummary();  
          this.fetchTasksDueToday(); // Fetch tasks due today
          this.fetchRecentCompletedTasks();  // Fetch recent completed tasks
          this.loadWeeklyTaskStatusChart();  // Load the weekly task status chart
        } else {
          this.logout();
        }
      }, err => {
        console.error('Error fetching user details:', err);
        this.router.navigate(['/login']);
      });
    });

    // Listen for route change events
    this.routeSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        if (event.urlAfterRedirects === `/home/${this.userId}`) {
          this.loadWeeklyTaskStatusChart();  // Reload the chart when navigating back to /home
        }
      });
  }

  ngOnDestroy(): void {
    // Unsubscribe from routeSub to prevent memory leaks
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }

  fetchTasksDueToday(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const today = new Date().toISOString().split('T')[0];
      this.tasksDueToday = tasks.filter(task => task.dueDate && task.dueDate.startsWith(today) && task.status !== 'Completed');
    });
  }

  isTasksRoute(): boolean {
    return this.router.url === `/home/${this.userId}/tasks`;
  }

  openAddTaskModal(event: Event): void {
    event.preventDefault();
    this.isAddTaskModalOpen = true;
  }

  closeAddTaskModal(): void {
    this.isAddTaskModalOpen = false;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    console.log('Sidebar collapsed state:', this.isSidebarCollapsed);
  }

  createTask(): void {
    this.newTask.userId = this.userId;
    this.taskService.createTask(this.newTask).subscribe(
      () => {
        this.closeAddTaskModal();
        this.openSuccessDialog();  
        this.fetchTaskSummary();  
        this.fetchTasksDueToday();  // Refresh tasks due today
        this.fetchRecentCompletedTasks();  // Refresh recent completed tasks
        this.loadWeeklyTaskStatusChart();  // Refresh the chart data
      },
      error => {
        console.error('Failed to create task:', error);
      }
    );
  }

  openSuccessDialog(): void {
    this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { message: 'Task created successfully!' }
    });
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.fetchTasksDueToday());
  }

  fetchTaskSummary(): void {
    this.taskService.getTaskSummary(this.userId).subscribe(summary => {
      this.totalTasks = summary.totalTasks;
      this.totalSubtasks = summary.totalSubtasks;
      this.pastDeadlineTasks = summary.pastDeadlineTasks;
      this.completedTasksThisMonth = summary.completedTasksThisMonth;
      this.totalTasksThisMonth = summary.totalTasksThisMonth;
    }, error => {
      console.error('Failed to fetch task summary:', error);
    });
  }

  fetchRecentCompletedTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const completedTasks = tasks
        .filter(task => task.status === 'Completed')
        .sort((a, b) => {
          const dateA = a.lastModifiedDate ? new Date(a.lastModifiedDate) : new Date();
          const dateB = b.lastModifiedDate ? new Date(b.lastModifiedDate) : new Date();
          return dateB.getTime() - dateA.getTime();
        })
        .slice(0, 10);
  
      this.recentCompletedTasks = completedTasks.map(task => ({
        title: task.title,
        timeAgo: this.calculateTimeAgo(task.lastModifiedDate ? new Date(task.lastModifiedDate) : new Date())
      }));
    });
  }

  calculateTimeAgo(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(hours / 24);

    if (hours < 24) {
      return `${hours} hours ago`;
    } else {
      return `${days} days ago`;
    }
  }

  loadWeeklyTaskStatusChart(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      const startOfWeek = this.startOfWeek();
      const endOfWeek = this.endOfWeek();
  
      const completed = tasks.filter(
        (task) =>
          task.status === 'Completed' &&
          task.dueDate &&
          new Date(task.dueDate).getTime() >= startOfWeek &&
          new Date(task.dueDate).getTime() <= endOfWeek
      ).length;
  
      const inProgress = tasks.filter(
        (task) =>
          task.status === 'In Progress' &&
          task.dueDate &&
          new Date(task.dueDate).getTime() >= startOfWeek &&
          new Date(task.dueDate).getTime() <= endOfWeek
      ).length;
  
      const pending = tasks.filter(
        (task) =>
          task.status === 'Pending' &&
          task.dueDate &&
          new Date(task.dueDate).getTime() >= startOfWeek &&
          new Date(task.dueDate).getTime() <= endOfWeek
      ).length;
  
      const totalTasksForWeek = completed + inProgress + pending;
  
      // Destroy the existing chart if it exists
      if (this.taskStatusChart) {
        this.taskStatusChart.destroy();
      }
  
      // Recreate the chart
      this.taskStatusChart = new Chart('taskStatusChart', {
        type: 'bar',
        data: {
          labels: ['Completed', 'In Progress', 'Pending'],
          datasets: [
            {
              data: [completed, inProgress, pending],
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
    this.taskService.handleMarkTaskAsCompleted(task, () => this.fetchTasksDueToday());
  }
}
