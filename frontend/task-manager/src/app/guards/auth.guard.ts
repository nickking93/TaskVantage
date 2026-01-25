import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { Observable, from } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): Observable<boolean> {
    // Convert the Promise from isAuthenticated to an Observable
    return from(this.authService.isAuthenticated()).pipe(
      tap(isAuthenticated => {
        if (!isAuthenticated) {
          this.router.navigate(['/login']);
        } else {
          // Store last successful authentication timestamp for PWA
          if ('localStorage' in window) {
            localStorage.setItem('lastAuthCheck', new Date().toISOString());
          }
        }
      })
    );
  }

  /**
   * Helper method to validate session freshness
   * Useful for PWA to ensure session is still valid after resume
   */
  private async validateSessionFreshness(): Promise<boolean> {
    const lastCheck = localStorage.getItem('lastAuthCheck');
    if (!lastCheck) return false;

    const lastCheckTime = new Date(lastCheck).getTime();
    const currentTime = new Date().getTime();
    const timeDifference = currentTime - lastCheckTime;
    
    // If more than 30 minutes have passed, force re-authentication
    const THIRTY_MINUTES = 30 * 60 * 1000;
    if (timeDifference > THIRTY_MINUTES) {
      return false;
    }

    return true;
  }

  /**
   * Method to check authentication state when PWA resumes
   */
  async checkAuthenticationOnResume(): Promise<boolean> {
    // Only proceed if this is a PWA
    if (!window.matchMedia('(display-mode: standalone)').matches) {
      return true;
    }

    const isSessionFresh = await this.validateSessionFreshness();
    if (!isSessionFresh) {
      return this.authService.isAuthenticated();
    }

    return true;
  }

  /**
   * Method to set up resume listener for PWA
   */
  setupResumeListener(): void {
    if (!window.matchMedia('(display-mode: standalone)').matches) {
      return;
    }

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.checkAuthenticationOnResume().then(isValid => {
          if (!isValid) {
            this.router.navigate(['/login']);
          }
        });
      }
    });
  }

  /**
   * Method to handle authentication timeout
   */
  private handleAuthTimeout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login'], {
        queryParams: { 
          reason: 'session_expired',
          returnUrl: this.router.url
        }
      });
    });
  }
}