import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { HomeComponent } from './home.component';
import { TasksComponent } from '../tasks/tasks.component';
import { SettingsComponent } from '../settings/settings.component';
import { SharedModule } from '../shared.module';  
import { HelpPageComponent } from '../help-page/help-page.component';

// Import Angular Material Modules
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild([
      {
        path: '',
        component: HomeComponent,
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
        ],
      },
    ]),
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatDialogModule,
    SharedModule,
    HomeComponent,   
    TasksComponent,  
    SettingsComponent,
    HelpPageComponent
  ]
})
export class HomeModule {}
