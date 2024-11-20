import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { GoogleAuthService } from '../services/google-auth.service';
import { UserService } from '../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { LoadingDialogComponent } from '../../app/loading-dialog.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatDividerModule,
    MatSnackBarModule,
    MatDialogModule
  ]
})
export class SettingsComponent implements OnInit {
  isGoogleConnected: boolean = false;
  isTaskSyncEnabled: boolean = false;
  isLoading: boolean = true;

  constructor(
    private googleAuthService: GoogleAuthService,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    // Load settings immediately
    this.loadAllSettings();
    
    // Handle OAuth redirect responses
    this.route.queryParams.subscribe(params => {
      if (params['status'] === 'success') {
        this.isGoogleConnected = true;
        this.snackBar.open('Successfully connected Google Calendar!', 'Close', {
          duration: 3000
        });
      } else if (params['error']) {
        let errorMessage = 'Failed to connect Google Calendar';
        switch (params['error']) {
          case 'auth_failed':
            errorMessage = 'Authentication failed';
            break;
          case 'client_null':
            errorMessage = 'Authorization failed';
            break;
          case 'user_not_found':
            errorMessage = 'User not found';
            break;
          default:
            errorMessage = `Failed to connect: ${params['error']}`;
        }
        this.snackBar.open(errorMessage, 'Close', {
          duration: 3000
        });
      }
    });
  }

private loadAllSettings(): void {
  const userId = this.route.snapshot.params['userId'] || localStorage.getItem('google_auth_user_id');
  
  if (userId) {
    import('rxjs').then(({ forkJoin }) => {
      forkJoin({
        connection: this.googleAuthService.checkGoogleCalendarConnection(userId),
        settings: this.userService.getUserSettings()
      }).subscribe({
        next: (results) => {
          this.isGoogleConnected = results.connection.connected;
          // Change this line to match backend response
          this.isTaskSyncEnabled = results.settings.enabled;  // <-- Changed from taskSyncEnabled to enabled
          this.isLoading = false;
        },
        error: (error) => {
          if (error.status !== 401) {
            this.snackBar.open('Failed to load settings', 'Close', {
              duration: 3000
            });
          }
          this.isLoading = false;
        }
      });
    });
  } else {
    this.isLoading = false;
  }
}

  connectGoogleCalendar(): void {
    let userId = this.route.snapshot.params['userId'];
  
    if (!userId) {
      userId = localStorage.getItem('google_auth_user_id');
    }
  
    console.log('User ID:', userId);
  
    if (userId) {
      this.googleAuthService.connectGoogleCalendar(userId);
    } else {
      console.error('User ID is not available');
      this.snackBar.open('User ID is not available', 'Close', {
        duration: 3000
      });
    }
  }

  toggleTaskSync(enabled: boolean): void {
    const previousState = this.isTaskSyncEnabled;
    this.isTaskSyncEnabled = enabled;  // Optimistically update

    this.userService.updateTaskSync(enabled).subscribe({
      next: (response) => {
        this.snackBar.open(
          `Task sync ${enabled ? 'enabled' : 'disabled'}`,
          'Close',
          { duration: 3000 }
        );
      },
      error: (error) => {
        console.error('Error updating task sync:', error);
        this.isTaskSyncEnabled = previousState;  // Revert on error
        this.snackBar.open('Failed to update task sync settings', 'Close', {
          duration: 3000
        });
      }
    });
  }

  disconnectGoogleCalendar(): void {
    const dialogRef = this.dialog.open(LoadingDialogComponent, {
      disableClose: true,
      data: {
        message: 'Disconnecting Google Calendar...'
      }
    });
  
    this.googleAuthService.disconnectGoogleCalendar().subscribe({
      next: () => {
        dialogRef.close();
        this.isGoogleConnected = false;
        this.isTaskSyncEnabled = false;
        this.snackBar.open('Google Calendar disconnected', 'Close', {
          duration: 3000
        });
      },
      error: (error) => {
        dialogRef.close();
        console.error('Error disconnecting Google Calendar:', error);
        this.snackBar.open('Failed to disconnect Google Calendar', 'Close', {
          duration: 3000
        });
      }
    });
  }
}