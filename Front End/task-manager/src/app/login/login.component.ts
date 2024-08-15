import { Component, OnInit, Renderer2 } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Declare the gapi object to use Google API functions
declare const gapi: any;

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
    private renderer: Renderer2,
    private authService: AuthService,  // Inject AuthService
    private router: Router  // Inject Router
  ) {
    // Initialize the FormGroup using FormBuilder
    this.signin = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.overrideGoogleSignInButton();
    this.overrideFacebookSignInButton();
    this.overrideAppleSignInButton();

    // Dynamically load the Google API script and initialize Google Sign-In
    this.loadGoogleApi();
  }

  loadGoogleApi() {
    const script = this.renderer.createElement('script');
    script.src = 'https://apis.google.com/js/platform.js';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      this.initializeGoogleSignIn();
    };
    this.renderer.appendChild(document.body, script);
  }

  initializeGoogleSignIn() {
    gapi.load('auth2', () => {
      gapi.auth2.init({
        client_id: '455717879419-er79jss8v4n2msdq2ihtnr2h3b81jqo5.apps.googleusercontent.com' // actual Google client ID
      }).then(() => {
        // Attach the Google Sign-In to the button element
        this.attachSignIn(document.getElementById('google-signin-button'));
      });
    });
  }

  attachSignIn(element: HTMLElement | null) {
    if (element) {
      const auth2 = gapi.auth2.getAuthInstance();
      auth2.attachClickHandler(element, {},
        (googleUser: any) => this.onSignIn(googleUser), // On successful sign-in
        (error: any) => console.error(JSON.stringify(error)) // On error
      );
    }
  }

  onSignIn(googleUser: any) {
    const profile = googleUser.getBasicProfile();
    console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
    console.log('Name: ' + profile.getName());
    console.log('Image URL: ' + profile.getImageUrl());
    console.log('Email: ' + profile.getEmail()); // This is null if the 'email' scope is not present.
  }

  // Override Google Sign-In button click behavior
  overrideGoogleSignInButton() {
    const googleSignInButton = document.getElementById('google-signin-button');
    if (googleSignInButton) {
      googleSignInButton.addEventListener('click', (event) => {
        event.preventDefault(); // Prevent the default Google Sign-In behavior
        this.loginWithGoogle();  // Call the custom Google Sign-In function
      });
    }
  }

  // Override Facebook Sign-In button click behavior
  overrideFacebookSignInButton() {
    const facebookSignInButton = document.getElementById('facebook-signin-button');
    if (facebookSignInButton) {
      facebookSignInButton.addEventListener('click', (event) => {
        event.preventDefault(); // Prevent the default Facebook Sign-In behavior
        this.facebookLogin();  // Call the custom Facebook Sign-In function
      });
    }
  }

  // Override Apple Sign-In button click behavior
  overrideAppleSignInButton() {
    const appleSignInButton = document.getElementById('appleid-signin');
    if (appleSignInButton) {
      appleSignInButton.addEventListener('click', (event) => {
        event.preventDefault(); // Prevent the default pop-up or post behavior
        this.redirectToApple();  // Call the custom redirect function
      });
    }
  }

  // Redirect-based Apple Sign-In function with fragment response mode
  redirectToApple() {
    const clientId = 'com.taskvantage.bundle.backend'; // Your actual client ID
    const redirectURI = 'https://9057-104-0-14-39.ngrok-free.app/auth-callback';
    const state = '[STATE]'; // Optional: Replace with your state if needed
    const scope = 'name email'; // Adjust the scope as necessary

    // Construct the URL for redirect-based sign-in with fragment response mode
    const appleAuthURL = `https://appleid.apple.com/auth/authorize?client_id=${encodeURIComponent(clientId)}&redirect_uri=${encodeURIComponent(redirectURI)}&response_type=code&scope=${scope}&response_mode=fragment`;

    console.log('Redirecting to:', appleAuthURL); // Log the URL for debugging

    // Redirect to Apple's authentication page
    window.location.href = appleAuthURL;
  }

  // Method to trigger Google OAuth login using a redirect
  loginWithGoogle() {
    window.location.href = 'https://accounts.google.com/o/oauth2/auth?client_id=455717879419-er79jss8v4n2msdq2ihtnr2h3b81jqo5.apps.googleusercontent.com&redirect_uri=https://9057-104-0-14-39.ngrok-free.app/auth/callback&response_type=code&scope=profile email';
  }

  // Method to trigger Facebook OAuth login using a redirect
  facebookLogin() {
    window.location.href = 'https://www.facebook.com/v10.0/dialog/oauth?client_id=863689325633198&redirect_uri=https://9057-104-0-14-39.ngrok-free.app/auth/callback&response_type=code&scope=email,public_profile';
  }

  // Handle form submission
  onSubmit() {
    if (this.signin.valid) {
      const email = this.signin.get('email')?.value;
      const password = this.signin.get('password')?.value;

      console.log('Login attempt with email:', email); // Debugging log

      // Call AuthService to handle login
      this.authService.login({ username: email, password }).subscribe(
        response => {
          // Store the JWT token in localStorage
          localStorage.setItem('token', response.token);

          // Extract userId from response and navigate to user's home page
          const userId = response.id;
          this.router.navigate([`/home/${userId}`]);
        },
        error => {
          // Handle login error
          console.error('Login failed', error);
          alert('Login failed. Please check your credentials and try again.');
        }
      );
    } else {
      alert('Please fill out the form correctly.');
    }
  }
}
