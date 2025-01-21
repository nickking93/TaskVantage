import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared.module';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { HomeComponent } from './home.component';
import { TasksComponent } from '../tasks/tasks.component';
import { SettingsComponent } from '../settings/settings.component';
import { HelpPageComponent } from '../help-page/help-page.component';
import { AddTaskComponent } from '../add-task/add-task.component';
import { UpdateTaskComponent } from '../update-task/update-task.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatSnackBarModule,
    RouterModule.forChild([
      {
        path: '',
        component: HomeComponent,
        children: [
          { path: 'tasks', component: TasksComponent },
          { path: 'settings', component: SettingsComponent },
          { path: 'help-page', component: HelpPageComponent },
          { path: 'add-task', component: AddTaskComponent },
          { path: 'update-task/:taskId', component: UpdateTaskComponent }
        ]
      }
    ])
  ]
})
export class HomeModule {}