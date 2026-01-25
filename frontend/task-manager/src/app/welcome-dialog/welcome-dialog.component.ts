import { Component } from '@angular/core';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-welcome-dialog',
    templateUrl: './welcome-dialog.component.html',
    styleUrls: ['./welcome-dialog.component.css'],
    imports: [CommonModule, MatDialogModule, MatButtonModule]
})
export class WelcomeDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<WelcomeDialogComponent>
  ) {
    dialogRef.disableClose = true;
  }

  onDecline(): void {
    this.dialogRef.close(false);
    
    // Try to go back to previous page if it exists
    if (window.history.length > 1) {
      window.history.back();
    } else {
      // Try to close the window
      try {
        window.close();
      } catch (e) {
        // If closing fails, show goodbye message
        document.body.innerHTML = `
          <div style="
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            text-align: center;
            padding: 20px;
            background-color: #f5f5f5;
          ">
            <h1 style="color: #333; margin-bottom: 20px;">Thank you for your interest</h1>
            <p style="color: #666;">You can safely close this window or navigate away from the page.</p>
          </div>
        `;
      }
    }
  }

  onAccept(): void {
    this.dialogRef.close(true);
  }
}