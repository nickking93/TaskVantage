import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';
import { LoadingDialogComponent } from '../loading-dialog.component';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

// Strong Password Validator
export function strongPasswordValidator(): ValidationErrors | null {
  return (control: { value: any; }) => {
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
  const newPassword = group.get('newPassword')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;

  if (confirmPassword && newPassword !== confirmPassword) {
    return { passwordMismatch: true };
  }
  return null;
}

@Component({
    selector: 'app-forgot-password',
    templateUrl: './forgot-password.component.html',
    styleUrls: ['./forgot-password.component.css'],
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        RouterModule,
        MatIconModule
    ]
})
export class ForgotPasswordComponent implements OnInit {

  resetPassword: FormGroup;
  isResetMode: boolean = false;
  hide = true;
  hideNewPassword: boolean = true;
  hideConfirmPassword: boolean = true;
  token: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private route: ActivatedRoute
  ) {
    this.resetPassword = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      newPassword: ['', [Validators.required, strongPasswordValidator()]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['token']) {
        this.isResetMode = true;
        this.token = params['token'];

        // Remove validators for email when in reset mode
        this.resetPassword.get('email')?.clearValidators();
        this.resetPassword.get('email')?.updateValueAndValidity();
      }
    });

    // Trigger validation on password fields when either changes
    this.resetPassword.get('confirmPassword')?.valueChanges.subscribe(() => {
      this.resetPassword.updateValueAndValidity();
    });
    this.resetPassword.get('newPassword')?.valueChanges.subscribe(() => {
      this.resetPassword.updateValueAndValidity();
    });
  }

  // Use this to check for mismatched passwords and display the error
  get passwordMismatch(): boolean {
    const confirmPasswordControl = this.resetPassword.get('confirmPassword');
    return confirmPasswordControl?.value.length > 0 && this.resetPassword.hasError('passwordMismatch');
  }  

// Enable button based on control validity
isButtonEnabled(): boolean {
  if (this.isResetMode) {
    return (this.resetPassword.get('newPassword')?.valid ?? false) && (this.resetPassword.get('confirmPassword')?.valid ?? false);
  }
  return this.resetPassword.get('email')?.valid ?? false;
}

  onSubmit(): void {
    // Prevent submission if form is invalid
    if (this.resetPassword.invalid) {
      if (this.passwordMismatch) {
        this.resetPassword.get('confirmPassword')?.setErrors({ passwordMismatch: true });
      } else {
        this.resetPassword.get('confirmPassword')?.setErrors(null);
      }
      return; // Prevent form submission if invalid
    }

    if (this.isResetMode) {
      const newPassword = this.resetPassword.value.newPassword;
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        data: { message: 'Processing request...' }
      });

      this.authService.updatePassword(this.token!, newPassword).subscribe(
        () => {
          loadingDialogRef.close();
          this.dialog.open(SuccessDialogComponent, {
            width: '300px',
            data: { title: 'Success', message: 'Your password has been updated successfully.' }
          });
          this.router.navigate(['/login']);
        },
        (error) => {
          loadingDialogRef.close();
          alert('An error occurred while updating the password. Please try again later.');
        }
      );
    } else {
      const email = this.resetPassword.value.email;
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        data: { message: 'Processing request...' }
      });

      this.authService.sendResetPasswordLink(email).subscribe(
        () => {
          loadingDialogRef.close();
          this.dialog.open(SuccessDialogComponent, {
            width: '300px',
            data: { title: 'Success', message: 'Password reset link sent to your email.' }
          });
        },
        (error) => {
          loadingDialogRef.close();
          alert('An error occurred while sending the reset link. Please try again later.');
        }
      );
    }
  }
}