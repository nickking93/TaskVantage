import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { TaskService } from '../services/task.service';
import { Task } from '../models/task.model';
import { FormsModule } from '@angular/forms'; 
import { RouterModule } from '@angular/router'; 
import { CommonModule } from '@angular/common'; 

// Import Angular Material Modules
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';  // Import SuccessDialogComponent

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
    MatDialogModule  // Add MatDialogModule to imports
  ]
})
export class HomeComponent implements OnInit {
  username: string = '';
  userId: string = '';
  isAddTaskModalOpen: boolean = false;
  newTask: Task = {
    title: '',
    description: '',
    dueDate: '',
    priority: 'Medium',
    recurring: false,
  };

  constructor(
    private authService: AuthService,
    private taskService: TaskService,
    private router: Router,
    private route: ActivatedRoute,
    private dialog: MatDialog  // Inject MatDialog
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('userId') || '';
      this.authService.getUserDetails().subscribe(user => {
        if (user.id.toString() === this.userId) {
          this.username = user.username;
        } else {
          this.logout();
        }
      }, err => {
        console.error('Error fetching user details:', err);
        this.router.navigate(['/login']);
      });
    });
  }

  openAddTaskModal(event: Event): void {
    event.preventDefault();
    this.isAddTaskModalOpen = true;
  }

  closeAddTaskModal(): void {
    this.isAddTaskModalOpen = false;
  }

  createTask(): void {
    this.newTask.userId = this.userId;
    this.taskService.createTask(this.newTask).subscribe(
      () => {
        this.closeAddTaskModal();
        this.openSuccessDialog();  // Open success dialog on successful task creation
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

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  // Method to determine the active state
  isActive(route: string): boolean {
    return this.router.url === route;
  }
}
