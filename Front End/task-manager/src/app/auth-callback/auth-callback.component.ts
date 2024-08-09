import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-auth-callback',
  templateUrl: './auth-callback.component.html'
})
export class AuthCallbackComponent implements OnInit {

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    // Handle the query params for other OAuth logins (e.g., Google, Facebook)
    this.route.queryParams.subscribe(params => {
      console.log('Query Params:', params); // Log all query parameters
      const googleAuthCode = params['code'];
      const facebookAuthCode = params['code'];
      if (googleAuthCode) {
        console.log('Google Auth Code:', googleAuthCode);
      }
      if (facebookAuthCode) {
        console.log('Facebook Auth Code:', facebookAuthCode);
      }
    });

    // Handle the fragment if using response_mode=fragment for Apple
    this.route.fragment.subscribe(fragment => {
      console.log('Fragment:', fragment); // Log the entire fragment

      if (fragment) {
        const params = new URLSearchParams(fragment);
        params.forEach((value, key) => {
          console.log(`${key}: ${value}`); // Log each key-value pair
        });

        const appleAuthCode = params.get('code');  // Get the Apple authorization code
        if (appleAuthCode) {
          console.log('Apple Auth Code:', appleAuthCode);
        }
      }
    });

    // Redirect to a different page if needed after handling the auth code
    this.router.navigate(['/some-other-route']);
  }
}
