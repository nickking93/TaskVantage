import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { HttpClientModule } from '@angular/common/http';
import { MatDialogModule } from '@angular/material/dialog';
import { ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

// Import the SuccessDialogComponent
import { SuccessDialogComponent } from './success-dialog/success-dialog.component';

// Import the standalone PrivacyPolicyComponent
import { PrivacyPolicyComponent } from './privacy-policy/privacy-policy.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    SuccessDialogComponent  // Declare the SuccessDialogComponent here
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    MatInputModule,
    MatIconModule,
    RouterModule,
    HttpClientModule,
    MatDialogModule,  // Import MatDialogModule for dialogs
    PrivacyPolicyComponent  // Import the standalone component here
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
