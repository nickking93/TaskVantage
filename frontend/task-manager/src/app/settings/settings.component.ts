import { Component, OnInit } from '@angular/core';
import { GoogleAuthService } from '../services/google-auth.service';
import { UserService } from '../services/user.service';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { LoadingDialogComponent } from '../../app/loading-dialog.component';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {
  isGoogleConnected: boolean = false;
  isTaskSyncEnabled: boolean = false;

  constructor(
    private googleAuthService: GoogleAuthService,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    // First, check the connection status regardless of query parameters
    this.checkConnectionStatus();
    
    // Then handle any OAuth redirect responses
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

  checkConnectionStatus(): void {
    const userId = this.route.snapshot.params['userId'] || localStorage.getItem('google_auth_user_id');
  
    if (userId) {
      this.googleAuthService.checkGoogleCalendarConnection(userId)
        .subscribe({
          next: (response) => {
            this.isGoogleConnected = response.connected;
            if (this.isGoogleConnected) {
              this.loadUserSettings();
            }
          },
          error: (error) => {
            if (error.status !== 401) { // Don't show error for auth failures
              this.snackBar.open('Failed to check connection status', 'Close', {
                duration: 3000
              });
            }
          }
        });
    }
  }

  private loadUserSettings(): void {
    console.log('SettingsComponent: Loading user settings');
    this.userService.getUserSettings().subscribe({
      next: (settings) => {
        console.log('SettingsComponent: Received settings:', settings);
        this.isTaskSyncEnabled = settings.taskSyncEnabled || settings.enabled; // Check both properties
      },
      error: (error) => {
        console.error('SettingsComponent: Error loading user settings:', error);
        this.snackBar.open('Failed to load settings', 'Close', {
          duration: 3000
        });
      }
    });
}

  connectGoogleCalendar(): void {
    // Attempt to retrieve userId from route parameters
    let userId = this.route.snapshot.params['userId'];
  
    // Fallback to localStorage if userId is not in route parameters
    if (!userId) {
      userId = localStorage.getItem('google_auth_user_id');
    }
  
    console.log('User ID:', userId); // Debugging line to check the userId
  
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
    console.log('SettingsComponent: Toggling task sync to:', enabled);
    this.userService.updateTaskSync(enabled).subscribe({
      next: (response) => {
        console.log('SettingsComponent: Update response:', response);
        this.isTaskSyncEnabled = enabled;
        
        // Verify the setting was saved by fetching current state
        this.loadUserSettings();
        
        this.snackBar.open(
          `Task sync ${enabled ? 'enabled' : 'disabled'}`,
          'Close',
          { duration: 3000 }
        );
      },
      error: (error) => {
        console.error('SettingsComponent: Error updating task sync:', error);
        this.snackBar.open('Failed to update task sync settings', 'Close', {
          duration: 3000 });
        // Revert the toggle if the update failed
        this.isTaskSyncEnabled = !enabled;
      }
    });
}

  disconnectGoogleCalendar(): void {
    // Open the loading dialog
    const dialogRef = this.dialog.open(LoadingDialogComponent, {
      disableClose: true,  // Prevent closing by clicking outside
      data: {
        message: 'Disconnecting Google Calendar...'
      }
    });
  
    this.googleAuthService.disconnectGoogleCalendar().subscribe({
      next: () => {
        dialogRef.close();  // Close the dialog on success
        this.isGoogleConnected = false;
        this.isTaskSyncEnabled = false;
        this.snackBar.open('Google Calendar disconnected', 'Close', {
          duration: 3000
        });
      },
      error: (error) => {
        dialogRef.close();  // Close the dialog on error
        console.error('Error disconnecting Google Calendar:', error);
        this.snackBar.open('Failed to disconnect Google Calendar', 'Close', {
          duration: 3000
        });
      }
    });
  }
}