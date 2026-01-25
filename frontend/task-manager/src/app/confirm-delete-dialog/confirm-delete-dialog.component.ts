import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
    selector: 'app-confirm-delete-dialog',
    template: `
    <h1 mat-dialog-title>Confirm Delete</h1>
    <div mat-dialog-content class="confirm-content">
      <p>{{ data?.message || 'Are you sure you want to delete this task?' }}</p>
    </div>
    <div mat-dialog-actions class="confirm-actions">
      <button mat-button color="warn" (click)="onConfirm()">Yes</button>
      <button mat-button (click)="onCancel()">No</button>
    </div>
  `,
    styles: [
        `
      .confirm-content {
        padding: 16px; /* Adds padding on all sides */
      }

      .confirm-actions {
        display: flex;
        justify-content: flex-end;
      }

      .confirm-actions button:first-child {
        margin-right: 8px; /* Adds spacing between the buttons */
      }
    `
    ],
    standalone: false
})
export class ConfirmDeleteDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
