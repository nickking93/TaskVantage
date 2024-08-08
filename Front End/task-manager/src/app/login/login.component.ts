import { Component, OnInit, Renderer2 } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';

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

  constructor(private fb: FormBuilder, private renderer: Renderer2) {
    // Initialize the FormGroup using FormBuilder
    this.signin = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {
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
}
