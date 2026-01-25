import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { TaskService } from '../services/task.service';
import { TaskGroupService } from '../services/task-group.service';
import { Task } from '../models/task.model';
import { TaskGroup } from '../models/task-group.model';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../services/auth.service';

@Component({
    selector: 'app-tasks',
    imports: [
        CommonModule,
        RouterModule,
        FormsModule,
        DragDropModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatMenuModule,
        MatButtonModule,
        MatSnackBarModule
    ],
    templateUrl: './tasks.component.html',
    styleUrls: ['./tasks.component.css']
})
export class TasksComponent implements OnInit {

  @Input() userId!: string;
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  groups: TaskGroup[] = [];
  selectedFilter: string = 'today';
  priorities: string[] = ['High', 'Medium', 'Low'];

  // Tasks organized by group
  uncategorizedTasks: Task[] = [];
  groupedTasks: Map<number, Task[]> = new Map();

  // Editing state
  editingGroupId: number | null = null;
  editingGroupName: string = '';

  // Color picker state
  showColorPicker: number | null = null;
  availableColors: string[] = [
    '#2365B2', '#28a745', '#ffc107', '#dc3545',
    '#6f42c1', '#fd7e14', '#20c997', '#e83e8c'
  ];

  constructor(
    private taskService: TaskService,
    private taskGroupService: TaskGroupService,
    private dialog: MatDialog,
    private router: Router,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    if (!this.userId) {
      this.authService.getUserDetails().subscribe({
        next: (user) => {
          this.userId = String(user.id);
          this.loadData();
        },
        error: (err) => {
          this.router.navigate(['/login']);
        }
      });
    } else {
      this.loadData();
    }
  }

  loadData(): void {
    this.loadTasks();
    this.loadGroups();
  }

  loadTasks(): void {
    this.taskService.fetchTasks(this.userId, (tasks) => {
      this.tasks = tasks.map(task => {
        if (task.priority) {
          task.priority = task.priority.charAt(0).toUpperCase() + task.priority.slice(1).toLowerCase();
        }
        return task;
      });
      this.filterTasks(this.selectedFilter);
    });
  }

  loadGroups(): void {
    this.taskGroupService.getGroups(Number(this.userId)).subscribe({
      next: (groups) => {
        this.groups = groups;
        this.organizeTasks();
      },
      error: () => {
        this.snackBar.open('Failed to load groups', 'Close', { duration: 3000 });
      }
    });
  }

  organizeTasks(): void {
    this.uncategorizedTasks = this.filteredTasks.filter(t => !t.groupId);
    this.groupedTasks.clear();
    this.groups.forEach(group => {
      this.groupedTasks.set(group.id!,
        this.filteredTasks.filter(t => t.groupId === group.id)
      );
    });
  }

  filterTasks(filter: string): void {
    this.selectedFilter = filter;

    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0);
    const endOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);

    switch (filter) {
      case 'today':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          if (!dueDate || isNaN(dueDate.getTime()) || this.taskService.isCompletedStatus(task.status)) return false;
          return dueDate >= startOfToday && dueDate <= endOfToday;
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && !isNaN(dueDate.getTime()) && dueDate < startOfToday && !this.taskService.isCompletedStatus(task.status);
        });
        break;
      case 'inProgress':
        this.filteredTasks = this.tasks.filter(task => task.status === 'In Progress');
        break;
      case 'pending':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Pending');
        break;
      case 'complete':
        this.filteredTasks = this.tasks.filter(task => this.taskService.isCompletedStatus(task.status));
        break;
      default:
        this.filteredTasks = this.tasks.filter(task => !this.taskService.isCompletedStatus(task.status));
    }

    this.organizeTasks();
  }

  onTaskDrop(event: CdkDragDrop<Task[]>, targetGroupId: number | null): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const task = event.previousContainer.data[event.previousIndex];
      const previousGroupId = task.groupId;
      const currentIndex = event.currentIndex;
      task.groupId = targetGroupId;

      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        currentIndex
      );

      this.taskService.updateTaskGroup(task.id!, targetGroupId).subscribe({
        next: () => {
          // Task group updated successfully
        },
        error: () => {
          // Revert the optimistic update
          task.groupId = previousGroupId;
          transferArrayItem(
            event.container.data,
            event.previousContainer.data,
            event.container.data.indexOf(task),
            event.previousIndex
          );
          this.snackBar.open('Failed to move task', 'Close', { duration: 3000 });
        }
      });
    }
  }

  addGroup(): void {
    const newGroup: TaskGroup = {
      userId: Number(this.userId),
      name: 'New Group',
      color: '#2365B2'
    };
    this.taskGroupService.createGroup(newGroup).subscribe({
      next: (response) => {
        this.groups.push(response.group);
        this.groupedTasks.set(response.group.id!, []);
        this.editingGroupId = response.group.id!;
        this.editingGroupName = response.group.name;
      },
      error: () => {
        this.snackBar.open('Failed to create group', 'Close', { duration: 3000 });
      }
    });
  }

  startEditingGroupName(group: TaskGroup): void {
    this.editingGroupId = group.id!;
    this.editingGroupName = group.name;
  }

  saveGroupName(group: TaskGroup): void {
    if (this.editingGroupName.trim()) {
      group.name = this.editingGroupName.trim();
      this.taskGroupService.updateGroup(group).subscribe({
        next: () => {
          // Group name saved successfully
        },
        error: () => {
          this.snackBar.open('Failed to save group name', 'Close', { duration: 3000 });
        }
      });
    }
    this.editingGroupId = null;
  }

  toggleColorPicker(groupId: number): void {
    if (this.showColorPicker === groupId) {
      this.showColorPicker = null;
    } else {
      this.showColorPicker = groupId;
    }
  }

  setGroupColor(group: TaskGroup, color: string): void {
    group.color = color;
    this.taskGroupService.updateGroup(group).subscribe({
      next: () => {
        // Group color updated successfully
      },
      error: () => {
        this.snackBar.open('Failed to update group color', 'Close', { duration: 3000 });
      }
    });
    this.showColorPicker = null;
  }

  deleteGroup(group: TaskGroup): void {
    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent, {
      data: { message: 'Are you sure you want to delete this group? Associated tasks will be moved to Uncategorized.' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.taskGroupService.deleteGroup(group.id!).subscribe({
          next: () => {
            this.loadData();
          },
          error: () => {
            this.snackBar.open('Failed to delete group', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  getConnectedLists(): string[] {
    return ['uncategorized-list', ...this.groups.map(g => `group-${g.id}`)];
  }

  startTask(task: Task): void {
    this.taskService.handleStartTask(task, () => this.loadTasks());
  }

  updateTask(task: Task): void {
    this.router.navigate(['/home/update-task', task.id]);
  }

  editTask(task: Task): void {
    this.taskService.updateTask(task).subscribe(
      (updatedTask: Task) => {
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) {
          this.tasks[index] = updatedTask;
          this.filterTasks(this.selectedFilter);
        }
      },
      () => {
        this.snackBar.open('Failed to update task', 'Close', { duration: 3000 });
      }
    );
  }

  deleteTask(task: Task): void {
    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent);

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.taskService.deleteTask(task.id!).subscribe(
          () => {
            this.tasks = this.tasks.filter(t => t.id !== task.id);
            this.filterTasks(this.selectedFilter);
          },
          () => {
            this.snackBar.open('Failed to delete task', 'Close', { duration: 3000 });
          }
        );
      }
    });
  }

  markTaskAsCompleted(task: Task): void {
    this.taskService.handleMarkTaskAsCompleted(task, () => this.loadTasks());
  }

  saveTaskField(task: Task): void {
    this.taskService.updateTask(task).subscribe(
      (updatedTask: Task) => {
        const index = this.tasks.findIndex(t => t.id === task.id);
        if (index !== -1) {
          this.tasks[index] = updatedTask;
          this.filterTasks(this.selectedFilter);
        }
      },
      () => {
        this.snackBar.open('Failed to save changes', 'Close', { duration: 3000 });
      }
    );
  }

  onDueDateChange(task: Task, event: any): void {
    if (event.value) {
      task.dueDate = event.value.toISOString();
      this.saveTaskField(task);
    }
  }

  getDueDateAsDate(task: Task): Date | null {
    return task.dueDate ? new Date(task.dueDate) : null;
  }

  convertUTCToLocal(date: Date): Date {
    const localDate = new Date(date.getTime() + date.getTimezoneOffset() * 60000);
    return localDate;
  }
}
