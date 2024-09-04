import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MatDialog } from '@angular/material/dialog'; 
import { LoadingDialogComponent } from '../loading-dialog.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  hide = true;
  signin: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,  
    private router: Router,  
    private dialog: MatDialog
  ) {
    this.signin = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {}

  onSubmit() {
    if (this.signin.valid) {
      const loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
        disableClose: true
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
          alert('Login failed. Please check your credentials and try again.');
        }
      );
    } else {
      alert('Please fill out the form correctly.');
    }
  }
}