import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { LoadingDialogComponent } from '../loading-dialog.component';

// Strong Password Validator
export function strongPasswordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.value;
    if (!password) {
      return null;
    }

    const errors: ValidationErrors = {};

    const hasLowerCase = /[a-z]/.test(password);
    if (!hasLowerCase) {
      errors['lowercase'] = true;
    }

    const hasNumeric = /[0-9]/.test(password);
    if (!hasNumeric) {
      errors['numeric'] = true;
    }

    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>-]/.test(password);
    if (!hasSpecialChar) {
      errors['specialChar'] = true;
    }

    const hasMinLength = password.length >= 8;
    if (!hasMinLength) {
      errors['minLength'] = true;
    }

    return Object.keys(errors).length > 0 ? errors : null;
  };
}

// Password Match Validator
export function passwordMatchValidator(group: FormGroup): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (confirmPassword && password !== confirmPassword) {
    group.get('confirmPassword')?.setErrors({ mismatch: true });
    return { mismatch: true };
  } else {
    group.get('confirmPassword')?.setErrors(null);
    return null;
  }
}

@Component({
    selector: 'app-register',
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.css'],
    standalone: false
})
export class RegisterComponent implements OnInit {
  hide = true;
  hideConfirm = true;
  registerForm: FormGroup;

  // Carousel content and configuration
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

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog
  ) {
    this.registerForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, strongPasswordValidator()]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  ngOnInit(): void {
    // Trigger validation updates on password and confirmPassword changes
    this.registerForm.get('confirmPassword')?.valueChanges.subscribe(() => {
      this.registerForm.updateValueAndValidity();
    });
    this.registerForm.get('password')?.valueChanges.subscribe(() => {
      this.registerForm.updateValueAndValidity();
    });
  }

  // Enable button based on form validity
  isButtonEnabled(): boolean {
    return this.registerForm.valid;
  }

  onSubmit(): void {
    // Prevent submission if the form is invalid
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched(); // Show all validation errors
      return;
    }

    // Proceed with form submission if valid
    const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
      disableClose: true,
      data: { message: 'Creating your account...' }
    });

    const email = this.registerForm.get('email')?.value;
    const password = this.registerForm.get('password')?.value;

    this.authService.register({ username: email, password }).subscribe(
      response => {
        loadingDialogRef.close();
        if (response) {
          this.openSuccessDialog();
        }
      },
      error => {
        loadingDialogRef.close();
      }
    );
  }

  openSuccessDialog(): void {
    const dialogRef = this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { title: 'Success', message: 'Account created! Please check your email for a verification link.' }
    });

    dialogRef.afterClosed().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  // Called when the carousel slide changes
  onSlideChange(event: any): void {
    this.currentSlide = event.currentSlide;
  }
}