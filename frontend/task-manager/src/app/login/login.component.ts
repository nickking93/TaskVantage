import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog'; 
import { LoadingDialogComponent } from '../loading-dialog.component';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component'; 
import { WelcomeDialogComponent } from '../../app/welcome-dialog/welcome-dialog.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  currentSlide = 0;

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
    private route: ActivatedRoute // ActivatedRoute to capture query params
  ) {
    this.signin = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Show the welcome dialog
    const welcomeDialogRef = this.dialog.open(WelcomeDialogComponent, {
      disableClose: true,
      width: '500px',
      maxWidth: '90vw',
      panelClass: 'welcome-dialog'
    });
  
    welcomeDialogRef.afterClosed().subscribe(result => {
      if (result === false) {
        // The decline logic is now handled in the dialog component
        return;
      }
      // Continue with normal flow for accepted users
      this.checkVerification();
    });
  }
  
  // Separate method for verification check
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

  // Method to verify the email using the token
  verifyEmail(token: string) {
    this.authService.verifyEmail(token).subscribe(
      (response) => {
        // Show success dialog after successful verification
        this.dialog.open(SuccessDialogComponent, {
          data: {
            title: 'Email Verified',
            message: 'Your email has been successfully verified. You can now log in.'
          }
        });
      },
      (error) => {
        // Handle the error case, e.g., invalid token
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

  onSubmit() {
    if (this.signin.valid) {
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        disableClose: true,
        data: {
          message: 'Logging in...'
        }
      });
  
      const email = this.signin.get('email')?.value;
      const password = this.signin.get('password')?.value;
  
      this.authService.login({
        username: email,
        password
      }).subscribe(
        (response) => {
          console.log('Login response:', response);
          
          // Changed from 'token' to 'jwtToken' to match AuthService
          localStorage.setItem('jwtToken', response.token);
          const userId = response.id;
          console.log('User ID after login:', userId);
  
          loadingDialogRef.close();
          this.router.navigate([`/home/${userId}`]);
        },
        error => {
          loadingDialogRef.close();
          console.error('Login failed', error);
  
          this.dialog.open(SuccessDialogComponent, {
            data: {
              title: 'Error',
              message: error.message
            }
          });
        }
      );
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
}