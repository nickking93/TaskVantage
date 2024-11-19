import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SuccessDialogComponent } from './success-dialog/success-dialog.component';
import { WelcomeDialogComponent } from './welcome-dialog/welcome-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button'; 

@NgModule({
  declarations: [
    SuccessDialogComponent,
    WelcomeDialogComponent, 
  ],
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,  
  ],
  exports: [
    SuccessDialogComponent,
    WelcomeDialogComponent, 
    MatDialogModule,
    MatButtonModule,  
  ]
})
export class SharedModule {}