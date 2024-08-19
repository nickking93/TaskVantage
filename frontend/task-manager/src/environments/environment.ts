// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,  // Change to true in environment.prod.ts
  apiUrl: 'https://localhost:8080/api',
  googleClientId: 'YOUR_GOOGLE_CLIENT_ID',
  appleClientId: 'YOUR_APPLE_CLIENT_ID',
  appleRedirectURI: 'YOUR_APPLE_REDIRECT_URI',
  googleRedirectURI: 'YOUR_GOOGLE_REDIRECT_URI',
  facebookRedirectURI: 'YOUR_FACEBOOK_REDIRECT_URI'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
