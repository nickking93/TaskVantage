import { Component } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatProgressSpinnerModule,
    CommonModule
  ],
  template: `
    <div style="text-align: center; padding: 20px;">
      <mat-spinner></mat-spinner>
      <p>Logging in...</p>
    </div>
  `
})
export class LoadingDialogComponent {}
