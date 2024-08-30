import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared.module'; // Import SharedModule

import { HomeComponent } from './home.component';  // Import HomeComponent explicitly
import { TasksComponent } from '../tasks/tasks.component';
import { SettingsComponent } from '../settings/settings.component';
import { HelpPageComponent } from '../help-page/help-page.component';
import { AddTaskComponent } from '../add-task/add-task.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild([
      {
        path: '',
        component: HomeComponent,  // Now HomeComponent is recognized
        children: [
          {
            path: 'tasks',
            component: TasksComponent,
          },
          {
            path: 'settings',
            component: SettingsComponent,
          },
          {
            path: 'help-page',
            component: HelpPageComponent,
          },
          {
            path: 'add-task',
            component: AddTaskComponent,
          },
        ],
      },
    ]),
    SharedModule, // Import SharedModule to make SuccessDialogComponent available
  ],
})
export class HomeModule {}
