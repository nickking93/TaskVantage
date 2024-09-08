import { Component, Inject } from '@angular/core';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
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
      <p>{{ data.message }}</p> <!-- Dynamically display the passed message -->
    </div>
  `
})
export class LoadingDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { message: string } // Accept dynamic message
  ) {}
}