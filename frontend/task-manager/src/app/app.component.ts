import { Component } from '@angular/core';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    standalone: false
})
export class AppComponent {
  title = 'TaskVantage';
  deferredPrompt: any;

  constructor() {
    this.setupInstallPrompt();
  }

  private setupInstallPrompt(): void {
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      this.deferredPrompt = e;
    });
  }

  async showInstallPrompt(): Promise<void> {
    if (this.deferredPrompt) {
      try {
        await this.deferredPrompt.prompt();
        await this.deferredPrompt.userChoice;
        this.deferredPrompt = null;
      } catch (error) {
      }
    }
  }
}