import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { LoadingDialogComponent } from '../loading-dialog.component'; // Import the LoadingDialogComponent

// Strong Password Validator
export function strongPasswordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.value;
    if (!password) {
      return null;
    }

    const errors: ValidationErrors = {};

    // Check for lowercase letters
    const hasLowerCase = /[a-z]/.test(password);
    if (!hasLowerCase) {
      errors['lowercase'] = true;
    }

    // Check for numeric characters
    const hasNumeric = /[0-9]/.test(password);
    if (!hasNumeric) {
      errors['numeric'] = true;
    }

    // Check for special characters
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>-]/.test(password);
    if (!hasSpecialChar) {
      errors['specialChar'] = true;
    }

    // Check for minimum length
    const hasMinLength = password.length >= 8;
    if (!hasMinLength) {
      errors['minLength'] = true;
    }

    // Return errors object, making sure that all errors are considered
    return Object.keys(errors).length > 0 ? errors : null;
  };
}

// Password Match Validator
export function passwordMatchValidator(control: AbstractControl): { [key: string]: boolean } | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');
  if (password && confirmPassword && password.value !== confirmPassword.value) {
    return { 'mismatch': true };
  }
  return null;
}

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  hide = true;
  hideConfirm = true;
  registerForm: FormGroup;

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

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.registerForm.valid && !this.passwordMatchError) {
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        disableClose: true,
        data: {
          message: 'Creating your account...' // Dynamic message for creating account
        }
      });

      const email = this.registerForm.get('email')?.value;
      const password = this.registerForm.get('password')?.value;
      this.authService.register({ username: email, password }).subscribe(
        response => {
          loadingDialogRef.close();
          if (response) {
            // Inform the user to check their email for confirmation
            this.openSuccessDialog();
          } else {
            console.log('Registration failed');
          }
        },
        error => {
          loadingDialogRef.close();
          console.error('Registration error', error);
        }
      );
    } else {
      if (this.passwordMatchError) {
        alert('Passwords do not match. Please correct and try again.');
      } else {
        console.log('Form has errors', this.registerForm.errors);
      }
    }
  }

  openSuccessDialog(): void {
    const dialogRef = this.dialog.open(SuccessDialogComponent, {
      width: '300px',
      data: { title: 'Success', message: 'Account created! Before you can log in, please check your email and click on the verification link.' }
    });

    dialogRef.afterClosed().subscribe(() => {
      this.router.navigate(['/login']); // Redirect to login page after dialog closes
    });
  }

  get passwordMatchError(): boolean {
    return this.registerForm.hasError('mismatch') && this.registerForm.get('confirmPassword')?.touched === true;
  }
}