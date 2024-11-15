import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router'; // ActivatedRoute added to get URL parameters
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog'; 
import { LoadingDialogComponent } from '../loading-dialog.component';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component'; // Import the dialog component

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
    // Add a slight delay to ensure queryParams are captured correctly
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
    }, 500); // Adding a 500ms delay
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
          message: 'Logging in...'  // Pass the dynamic message here
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
          
          localStorage.setItem('token', response.token);
          const userId = response.id;
          console.log('User ID after login:', userId);
  
          loadingDialogRef.close();
          this.router.navigate([`/home/${userId}`]);
        },
        error => {
          loadingDialogRef.close();
          console.error('Login failed', error);
  
          // Open the dialog on login failure
          this.dialog.open(SuccessDialogComponent, {
            data: {
              title: 'Error', // Dynamic title for error
              message: error.message // Use the message from the error handler
            }
          });
        }
      );
    } else {
      // Validation errors in the form
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