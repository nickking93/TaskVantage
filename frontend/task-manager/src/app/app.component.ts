import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'TaskVantage';
  deferredPrompt: any; // Variable to store the deferred install prompt

  constructor() {}

  ngOnInit(): void {
    // Listen for the 'beforeinstallprompt' event
    window.addEventListener('beforeinstallprompt', (e) => {
      // Prevent the mini-infobar from appearing
      e.preventDefault();
      // Stash the event so it can be triggered later
      this.deferredPrompt = e;

      // Optionally, show a custom "Add to Home Screen" button
      const addBtn = document.querySelector('#add-button') as HTMLButtonElement;
      if (addBtn) {
        addBtn.style.display = 'block'; // Show the custom button
        addBtn.addEventListener('click', () => {
          this.showInstallPrompt();
        });
      }
    });
  }

  showInstallPrompt(): void {
    if (this.deferredPrompt) {
      // Show the install prompt
      this.deferredPrompt.prompt();

      // Wait for the user to respond to the prompt
      this.deferredPrompt.userChoice.then((choiceResult: any) => {
        if (choiceResult.outcome === 'accepted') {
          console.log('User accepted the install prompt');
        } else {
          console.log('User dismissed the install prompt');
        }
        this.deferredPrompt = null; // Clear the deferred prompt
      });
    }
  }
}
