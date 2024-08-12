import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';  // Import AuthService

@Component({
  selector: 'app-auth-callback',
  templateUrl: './auth-callback.component.html'
})
export class AuthCallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService  // Inject AuthService
  ) {}

  ngOnInit(): void {
    // Handle the query params for other OAuth logins (e.g., Google, Facebook)
    this.route.queryParams.subscribe(params => {
      const authCode = params['code'];
      if (authCode) {
        this.handleAuthCode(authCode, 'google');  // For Google or Facebook
      }
    });

    // Handle the fragment if using response_mode=fragment for Apple
    this.route.fragment.subscribe(fragment => {
      if (fragment) {
        const params = new URLSearchParams(fragment);
        const appleAuthCode = params.get('code');  // Get the Apple authorization code
        if (appleAuthCode) {
          this.handleAuthCode(appleAuthCode, 'apple');  // For Apple
        }
      }
    });
  }

  handleAuthCode(authCode: string, provider: string): void {
    this.authService.verifySocialLogin(authCode, provider).subscribe({
      next: (response) => {
        // Assuming response contains user information and authentication token
        this.authService.setUserDetails(response);
        this.router.navigate(['/home']);  // Navigate to home on successful login
      },
      error: (error) => {
        console.error('Authentication failed:', error);
        this.router.navigate(['/login']);  // Redirect to login on failure
      }
    });
  }
}
