import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { User } from '../models/user.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  username: string = '';
  userId: string = '';

  constructor(
    private authService: AuthService, 
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Retrieve userId from the route parameters
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('userId') || '';

      // Fetch user details and ensure the userId matches
      this.authService.getUserDetails().subscribe(user => {
        if (user && user.id && user.id.toString() === this.userId) {  // Ensure user and user.id are defined
          this.username = user.username;
        } else {
          // If userId doesn't match, log out the user and redirect to login
          this.logout();
        }
      }, err => {
        // Handle errors or redirect to login if not authenticated
        this.router.navigate(['/login']);
      });
    });
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}
