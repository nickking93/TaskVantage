import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { LoadingDialogComponent } from '../loading-dialog.component';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { WelcomeDialogComponent } from '../../app/welcome-dialog/welcome-dialog.component';
import { FirebaseMessagingService } from '../../app/services/firebase-messaging.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  currentSlide = 0;
  returnUrl: string = '/';
  isPwa: boolean = false;
  isReturningPwaUser: boolean = false;

  carouselHeadings = [
    'Manage Your Tasks with Ease',
    'Seamless Task Tracking',
    'Collaboration Made Simple'
  ];

  carouselDescriptions = [
    'Stay on top of your tasks, deadlines, and productivity with TaskVantage.',
    'Track your tasks across devices with ease and flexibility.',
    'Work together with your team effortlessly with TaskVantage.'
  ];

  hide = true;
  signin: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private firebaseMessagingService: FirebaseMessagingService
  ) {
    this.signin = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      rememberMe: [false]
    });
  }

  async ngOnInit(): Promise<void> {
    // Check if running as PWA
    this.isPwa = window.matchMedia('(display-mode: standalone)').matches ||
                 window.navigator.standalone ||
                 document.referrer.includes('android-app://');

    // Check if returning PWA user
    if (this.isPwa) {
      this.isReturningPwaUser = await this.authService.isAuthenticated();
      if (this.isReturningPwaUser) {
        const userDetails = await this.authService.getUserDetails().toPromise();
        if (userDetails) {
          this.router.navigate(['/home']);
          return;
        }
      }
    }

    // Show welcome dialog for non-PWA users
    if (!this.isPwa) {
      const welcomeDialogRef = this.dialog.open(WelcomeDialogComponent, {
        disableClose: true,
        width: '500px',
        maxWidth: '90vw',
        panelClass: 'welcome-dialog'
      });

      welcomeDialogRef.afterClosed().subscribe(result => {
        if (result === false) {
          return;
        }
        this.checkVerification();
      });
    }

    // Get return URL from route parameters or default to '/'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';

    // Check for session expiry message
    const reason = this.route.snapshot.queryParams['reason'];
    if (reason === 'session_expired') {
      this.dialog.open(SuccessDialogComponent, {
        data: {
          title: 'Session Expired',
          message: 'Your session has expired. Please log in again.'
        }
      });
    }
  }

  private checkVerification(): void {
    setTimeout(() => {
      this.route.queryParams.subscribe(params => {
        const verified = params['verified'];
        if (verified === 'true') {
          this.dialog.open(SuccessDialogComponent, {
            data: {
              title: 'Email Verified',
              message: 'Your email has been successfully verified. You can now log in.'
            }
          });
        } else if (verified === 'false') {
          this.dialog.open(SuccessDialogComponent, {
            data: {
              title: 'Verification Failed',
              message: 'Email verification failed. Please try again.'
            }
          });
        }
      });
    }, 500);
  }

  verifyEmail(token: string) {
    this.authService.verifyEmail(token).subscribe(
      (response) => {
        this.dialog.open(SuccessDialogComponent, {
          data: {
            title: 'Email Verified',
            message: 'Your email has been successfully verified. You can now log in.'
          }
        });
      },
      (error) => {
        console.error('Email verification failed:', error);
        this.dialog.open(SuccessDialogComponent, {
          data: {
            title: 'Verification Failed',
            message: 'Email verification failed. Please try again.'
          }
        });
      }
    );
  }

  async onSubmit() {
    if (this.signin.valid) {
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        disableClose: true,
        data: {
          message: 'Logging in...'
        }
      });
  
      const email = this.signin.get('email')?.value;
      const password = this.signin.get('password')?.value;
      const rememberMe = this.signin.get('rememberMe')?.value;
  
      try {
        const response = await this.authService.login({
          username: email,
          password
        }).toPromise();
  
        if (!response) {
          throw new Error('Login response was empty');
        }
  
        console.log('Login response:', response);
        
        if (this.isPwa || rememberMe) {
          // Store persistent login state
          await this.authService.setPersistentLogin(true);
        }
  
        try {
          // Initialize Firebase messaging
          await this.firebaseMessagingService.initialize();
          
          // Request notification permission if not already granted
          if (Notification.permission === 'default') {
            await this.firebaseMessagingService.requestPermissionAndGetToken();
          }
        } catch (error) {
          console.error('Error initializing notifications:', error);
        }
  
        loadingDialogRef.close();

        // Navigate to home - userId is retrieved from AuthService
        this.router.navigate(['/home']);
      } catch (error: any) {
        loadingDialogRef.close();
        console.error('Login failed', error);
  
        this.dialog.open(SuccessDialogComponent, {
          data: {
            title: 'Error',
            message: error.message || 'Login failed. Please try again.'
          }
        });
      }
    } else {
      this.dialog.open(SuccessDialogComponent, {
        data: {
          title: 'Error',
          message: 'Please fill out the form correctly.'
        }
      });
    }
  }

  onSlideChange(event: any): void {
    this.currentSlide = event.currentSlide;
  }

  // Add biometric authentication for PWA if supported
  async checkBiometricAvailability(): Promise<boolean> {
    if (!this.isPwa) return false;
    
    // Check if Web Authentication API is available
    if (window.PublicKeyCredential) {
      try {
        // Check if platform authenticator is available
        const available = await PublicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable();
        return available;
      } catch (error) {
        console.error('Error checking biometric availability:', error);
        return false;
      }
    }
    return false;
  }

  // Handle biometric authentication
  async authenticateWithBiometric(): Promise<void> {
    // Implementation would go here - requires additional backend support
    // This is just a placeholder for future implementation
    console.log('Biometric authentication not yet implemented');
  }
}