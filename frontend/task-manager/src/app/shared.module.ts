// shared.module.ts
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SuccessDialogComponent } from './success-dialog/success-dialog.component';
import { MatDialogModule } from '@angular/material/dialog';

@NgModule({
  declarations: [
    SuccessDialogComponent,
  ],
  imports: [
    CommonModule,
    MatDialogModule
  ],
  exports: [
    SuccessDialogComponent,
    MatDialogModule
  ]
})
export class SharedModule {}
