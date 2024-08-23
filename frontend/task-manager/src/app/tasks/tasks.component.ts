import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../services/task.service'; 
import { Task } from '../models/task.model'; 

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule
  ],
  templateUrl: './tasks.component.html',
  styleUrls: ['./tasks.component.css']
})
export class TasksComponent implements OnInit {
  @Input() userId!: string;
  tasks: Task[] = [];
  filteredTasks: Task[] = [];
  selectedFilter: string = 'today';

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    console.log('TasksComponent initialized with userId:', this.userId);
    this.fetchTasks();  
  }

  // Method to fetch tasks for the user
  fetchTasks(): void {
    this.taskService.getTasks(this.userId).subscribe(
      (tasks: Task[]) => {
        this.tasks = tasks;
        this.filterTasks(this.selectedFilter); // Apply the default filter on load
        console.log('Fetched tasks:', this.tasks);
      },
      (error) => {
        console.error('Failed to fetch tasks:', error);
      }
    );
  }

  // Method to filter tasks based on the selected category
  filterTasks(filter: string): void {
    this.selectedFilter = filter;
    const now = new Date();

    switch (filter) {
      case 'today':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && dueDate.toDateString() === now.toDateString();
        });
        break;
      case 'overdue':
        this.filteredTasks = this.tasks.filter(task => {
          const dueDate = task.dueDate ? new Date(task.dueDate) : null;
          return dueDate && dueDate < now;
        });
        break;
      case 'inProgress':
        this.filteredTasks = this.tasks.filter(task => task.status === 'In Progress');
        break;
      case 'pending':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Pending');
        break;
      case 'completed':
        this.filteredTasks = this.tasks.filter(task => task.status === 'Completed');
        break;
      default:
        this.filteredTasks = this.tasks;
    }
  }
}
