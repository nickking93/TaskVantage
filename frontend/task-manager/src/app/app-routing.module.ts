import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AuthGuard } from './guards/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'home/:userId',
    loadChildren: () => import('./home/home.module').then(m => m.HomeModule), // Lazy load the home module
    canActivate: [AuthGuard],
  },
  { path: 'privacy-policy', loadChildren: () => import('./privacy-policy/privacy-policy.module').then(m => m.PrivacyPolicyModule) },
  { path: 'help-page', loadChildren: () => import('./help-page/help-page.module').then(m => m.HelpPageModule) },
  { path: 'settings/:userId', loadChildren: () => import('./settings/settings.module').then(m => m.SettingsModule) },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
