import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
    selector: 'app-success-dialog',
    templateUrl: './success-dialog.component.html',
    styleUrls: ['./success-dialog.component.css'],
    standalone: false
})
export class SuccessDialogComponent {

  constructor(
    public dialogRef: MatDialogRef<SuccessDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { title: string, message: string } // Added title to the data
  ) {}

  onClose(): void {
    this.dialogRef.close();
  }
}