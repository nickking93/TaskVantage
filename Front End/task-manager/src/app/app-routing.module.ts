import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { TaskListComponent } from './task-list/task-list.component';
import { TaskDetailsComponent } from './task-details/task-details.component';
// import { AuthGuard } from './auth/auth.guard'; // AuthGuard for protecting routes

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  // { path: 'tasks', component: TaskListComponent, canActivate: [AuthGuard] },
  // { path: 'tasks/:id', component: TaskDetailsComponent, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '/login' } // Wildcard route for a 404 page
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
