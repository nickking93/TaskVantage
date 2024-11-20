import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AuthGuard } from './guards/auth.guard';
import { VerifyEmailComponent } from './verify-email/verify-email.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ForgotPasswordComponent },
  {
    path: 'home/:userId',
    loadChildren: () => import('./home/home.module').then(m => m.HomeModule),
    canActivate: [AuthGuard],
  },
  { path: 'privacy-policy', loadChildren: () => import('./privacy-policy/privacy-policy.module').then(m => m.PrivacyPolicyModule) },
  { path: 'verify-email', component: VerifyEmailComponent },
  {
    path: 'home/:userId/settings',  
    loadChildren: () => import('./settings/settings.module').then(m => m.SettingsModule),
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '/login' }  
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }