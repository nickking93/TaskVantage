import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-verify-email',
    templateUrl: './verify-email.component.html',
    styleUrls: ['./verify-email.component.css'],
    imports: [CommonModule]
})
export class VerifyEmailComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Get the token from the query params
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      if (token) {
        this.verifyEmail(token);
      }
    });
  }

  verifyEmail(token: string): void {
    this.authService.verifyEmail(token).subscribe(
      response => {
        // Redirect to the login page with a query parameter 'verified'
        this.router.navigate(['/login'], { queryParams: { verified: 'true' } });
      },
      error => {
        // Redirect to the login page even if verification fails
        this.router.navigate(['/login'], { queryParams: { verified: 'false' } });
      }
    );
  }
}