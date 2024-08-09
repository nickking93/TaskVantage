import { Component, OnInit } from '@angular/core';
import { TaskService } from '../task.service';

@Component({
  selector: 'app-task',
  templateUrl: './task.component.html',
  styleUrls: ['./task.component.css']
})
export class TaskComponent implements OnInit {

  message: string = '';

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.taskService.testBackend().subscribe(response => {
      this.message = response;
    });
  }
}