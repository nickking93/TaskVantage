import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { SuccessDialogComponent } from '../success-dialog/success-dialog.component';

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
      password: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordMatchValidator });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.registerForm.valid && !this.passwordMatchError) {
      const email = this.registerForm.get('email')?.value;
      const password = this.registerForm.get('password')?.value;
      this.authService.register({ username: email, password }).subscribe(
        response => {
          if (response) {
            this.openSuccessDialog();
          } else {
            console.log('Registration failed');
          }
        },
        error => {
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
      data: { message: 'Account created successfully!' }
    });

    dialogRef.afterClosed().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }

  get passwordMatchError(): boolean {
    return this.registerForm.hasError('mismatch') && this.registerForm.get('confirmPassword')?.touched === true;
  }
}
