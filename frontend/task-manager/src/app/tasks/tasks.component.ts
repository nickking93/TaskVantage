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
        console.log('Fetched tasks:', this.tasks);
      },
      (error) => {
        console.error('Failed to fetch tasks:', error);
      }
    );
  }
}
