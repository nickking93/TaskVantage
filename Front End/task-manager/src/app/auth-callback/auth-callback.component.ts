import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  templateUrl: './auth-callback.component.html',
  styleUrls: ['./auth-callback.component.css']
})
export class AuthCallbackComponent implements OnInit {

  constructor(private route: ActivatedRoute, private router: Router) { }

  ngOnInit(): void {
    // Example logic to handle Google OAuth response
    this.route.queryParams.subscribe(params => {
      const googleAuthCode = params['code'];
      if (googleAuthCode) {
        // Handle Google OAuth code, typically by sending it to your backend
        console.log('Google Auth Code:', googleAuthCode);
        // Redirect to home or another page after processing
        this.router.navigate(['/privacy-policy']);
      }

      const appleAuthCode = params['id_token'];
      if (appleAuthCode) {
        // Handle Apple Sign-In token, typically by sending it to your backend
        console.log('Apple ID Token:', appleAuthCode);
        // Redirect to home or another page after processing
        this.router.navigate(['/privacy-policy']);
      }
    });
  }
}
